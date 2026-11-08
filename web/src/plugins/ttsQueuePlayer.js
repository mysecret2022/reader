/**
 * TTSQueuePlayer: 长文本流式预取播放器 (针对 iOS Safari 与移动端深度优化)
 * 特性：
 * 1. 同步静音帧解锁 (防异步 fetch 后 iOS 手势授权失效)
 * 2. 启发式切块器 (首包极速 15~30 字，常规语义块 60~120 字，过滤异常符号)
 * 3. 纯事件驱动预加载 (滑动窗口 PREFETCH_SIZE = 5，抗 iOS 锁屏定时器冻结)
 * 4. 单一 <audio> 实例 + Blob URL 即时换轨 (避开移动端 Autoplay 策略拦截)
 * 5. MediaSession 锁屏控制台 (书名、章节名、AirPods 上下一段、系统级保活)
 * 6. 异常容错跳过与 Blob 内存自动回收 (防内存泄漏)
 */

// 极短 Base64 静音 MP3 (约 0.1s)，用于在用户点击手势瞬间同步解锁 iOS WebKit 播放权限
const SILENT_MP3_DATA_URI =
  "data:audio/mpeg;base64,//uQxAAAAAAAAAAAAAAAAAAAAAAAWGluZwAAAA8AAAACAAACcQCAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICA//sQxAAMAAAAAbwAAAAAAB3AAA=";

/**
 * 启发式文本分块器
 */
export function chunkTextForTTS(rawText) {
  if (!rawText || typeof rawText !== "string") return [];

  // 清除无朗读意义的特殊符号和 markdown 分割线
  const cleanedText = rawText
    .replace(/[*#=~_`^|]{3,}/g, " ")
    .replace(/[\t\f\v]/g, " ")
    .replace(/[ ]{2,}/g, " ");

  // 按换行符和句末标点符号拆分
  const rawSegments = cleanedText
    .split(/([\r\n]+|[。！？!?；;\n]+)/g)
    .map(s => s.trim())
    .filter(s => s && !/^[*#=~_`^|\-–—\s.]+$/.test(s));

  if (!rawSegments.length) return [];

  const chunks = [];
  let buffer = "";

  for (let i = 0; i < rawSegments.length; i++) {
    const s = rawSegments[i];
    if (!s) continue;

    // 首块极速策略：首包严格控制在 15~30 字符，若当前已达到首块要求则立刻成块输出
    if (chunks.length === 0) {
      if (buffer.length + s.length <= 30) {
        buffer += s;
      } else {
        if (buffer) {
          chunks.push({ index: 0, text: buffer });
          buffer = s;
        } else {
          chunks.push({ index: 0, text: s });
        }
      }
      continue;
    }

    // 后续常规块：贪心合并至 60 ~ 120 字符
    if (buffer.length + s.length < 90) {
      buffer += s;
    } else {
      if (buffer) {
        chunks.push({ index: chunks.length, text: buffer });
      }
      buffer = s;
    }
  }

  if (buffer) {
    chunks.push({ index: chunks.length, text: buffer });
  }

  return chunks;
}

export class TTSQueuePlayer {
  constructor(options = {}) {
    this.audio = options.audio || new Audio();
    this.audio.preload = "auto";
    this.apiBase = options.apiBase || "";
    this.voice = options.voice || "zh-CN-XiaoxiaoNeural";
    this.rate = options.rate || 1.0;
    this.pitch = options.pitch || 1.0;
    this.prefetchWindow = 5; // iOS 锁屏保活增强：扩大预取窗口至 5

    this.chunks = [];
    this.currentIndex = 0;
    this.isPlaying = false;
    this.isUnlocked = false;

    // 元数据 (供 MediaSession 显示)
    this.title = options.title || "正在朗读";
    this.bookName = options.bookName || "Reader";

    // 缓存池: index -> { blob, blobUrl, controller, failed }
    this.cachePool = new Map();
    this.activeControllers = new Set();

    // 外部回调
    this.onChunkStart = options.onChunkStart || (() => {});
    this.onChunkEnd = options.onChunkEnd || (() => {});
    this.onEnded = options.onEnded || (() => {});
    this.onError = options.onError || (() => {});
    this.onQueueEmpty = options.onQueueEmpty || (() => {});
    this.onNearEnd = options.onNearEnd || (() => {}); // 提前预取下一章回调

    this._bindAudioEvents();
  }

  _bindAudioEvents() {
    // 纯事件驱动预加载：利用原生 ended 和 play 事件驱动预取，抗 iOS 锁屏定时器休眠
    this.audio.addEventListener("ended", () => {
      this.onChunkEnd(this.currentIndex);
      this._revokeChunk(this.currentIndex);
      this.currentIndex++;

      // 跨章无缝衔接：若播到倒数第 2 块，提前触发下一章正文静默拉取
      if (this.currentIndex >= this.chunks.length - 2) {
        this.onNearEnd();
      }

      if (this.currentIndex < this.chunks.length) {
        this._playCurrentChunk();
        this._prefetchSlidingWindow();
      } else {
        this._revokeChunk(this.currentIndex);
        this.isPlaying = false;
        this.onEnded();
        // 触发跨章节连播
        this.onQueueEmpty();
      }
    });

    this.audio.addEventListener("play", () => {
      this._prefetchSlidingWindow();
      this._updateMediaSessionPlaybackState("playing");
    });

    this.audio.addEventListener("pause", () => {
      if (this.isPlaying) {
        this._updateMediaSessionPlaybackState("paused");
      }
    });

    // 容错机制：单音频块播放失败时，跳过该块自动尝试下一个，防止卡死
    this.audio.addEventListener("error", () => {
      if (!this.isPlaying) return;
      this._revokeChunk(this.currentIndex);
      this.currentIndex++;
      if (this.currentIndex < this.chunks.length) {
        this._playCurrentChunk();
      } else {
        this.stop();
        this.onQueueEmpty();
      }
    });
  }

  /**
   * 同步静音帧解锁：必须在用户点击手势的同步调用栈中立即触发
   */
  unlockIOSAudio() {
    if (this.isUnlocked) return;
    try {
      this.audio.src = SILENT_MP3_DATA_URI;
      const playPromise = this.audio.play();
      if (playPromise !== undefined) {
        playPromise
          .then(() => {
            this.isUnlocked = true;
          })
          .catch(() => {});
      }
    } catch (e) {
      // 捕获权限探测
    }
  }

  /**
   * 加载待朗读文本
   */
  loadText(rawText, options = {}) {
    this.stop();
    this.title = options.title || this.title;
    this.bookName = options.bookName || this.bookName;
    this.chunks = chunkTextForTTS(rawText);
    this.currentIndex = 0;
    this._setupMediaSession();
  }

  /**
   * 追加新章节分块 (用于章节无缝衔接)
   */
  appendChunks(rawText, options = {}) {
    if (options.title) this.title = options.title;
    const newChunks = chunkTextForTTS(rawText);
    const startIdx = this.chunks.length;
    newChunks.forEach((c, i) => {
      c.index = startIdx + i;
      this.chunks.push(c);
    });
    this._setupMediaSession();
    if (this.isPlaying) {
      this._prefetchSlidingWindow();
    }
  }

  /**
   * 开始播放 (同步激活静音帧并拉取 Chunk[0])
   */
  async play(startIndex = null) {
    this.unlockIOSAudio();
    if (typeof startIndex === "number") {
      this.currentIndex = Math.max(
        0,
        Math.min(startIndex, this.chunks.length - 1)
      );
    }
    if (this.chunks.length === 0) {
      this.onQueueEmpty();
      return;
    }

    this.isPlaying = true;
    this._updateMediaSessionPlaybackState("playing");
    await this._playCurrentChunk();
    this._prefetchSlidingWindow();
  }

  async _playCurrentChunk() {
    if (!this.isPlaying || this.currentIndex >= this.chunks.length) return;

    const chunk = this.chunks[this.currentIndex];
    this.onChunkStart(chunk, this.currentIndex);

    let item = this.cachePool.get(this.currentIndex);
    if (!item || !item.blobUrl) {
      item = await this._fetchChunk(chunk);
    }

    // 若拉取失败，自动跳至下一段
    if (!item || !item.blobUrl) {
      this.currentIndex++;
      if (this.currentIndex < this.chunks.length) {
        this._playCurrentChunk();
      } else {
        this.stop();
        this.onQueueEmpty();
      }
      return;
    }

    if (this.isPlaying) {
      this.audio.src = item.blobUrl;
      // 双轨制语速：使用原生 playbackRate 变速，保持后端高频缓存复用
      if (this.rate && this.rate !== 1.0) {
        this.audio.playbackRate = Number(this.rate) || 1.0;
      }
      try {
        await this.audio.play();
      } catch (err) {
        // 捕获权限探测
      }
    }
  }

  _prefetchSlidingWindow() {
    if (!this.isPlaying) return;
    for (let i = 1; i <= this.prefetchWindow; i++) {
      const targetIdx = this.currentIndex + i;
      if (targetIdx < this.chunks.length && !this.cachePool.has(targetIdx)) {
        this._fetchChunk(this.chunks[targetIdx]);
      }
    }
  }

  async _fetchChunk(chunk) {
    if (!chunk || !chunk.text) return null;
    const controller = new AbortController();
    this.activeControllers.add(controller);
    this.cachePool.set(chunk.index, { loading: true, controller });

    try {
      const url = `${this.apiBase}/tts?text=${encodeURIComponent(
        chunk.text
      )}&voice=${encodeURIComponent(this.voice)}&rate=${this.rate}&pitch=${
        this.pitch
      }`;
      const res = await fetch(url, { signal: controller.signal });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);

      const blob = await res.blob();
      const blobUrl = URL.createObjectURL(blob);
      const item = { blob, blobUrl, controller };
      this.cachePool.set(chunk.index, item);
      return item;
    } catch (err) {
      this.cachePool.delete(chunk.index);
      return null;
    } finally {
      this.activeControllers.delete(controller);
    }
  }

  _revokeChunk(index) {
    const item = this.cachePool.get(index);
    if (item && item.blobUrl) {
      URL.revokeObjectURL(item.blobUrl);
      this.cachePool.delete(index);
    }
  }

  setVoice(voice) {
    if (this.voice === voice) return;
    this.voice = voice;
    // 发音人变更：立即取消当前未播出的预取缓存
    for (const ctrl of this.activeControllers) {
      ctrl.abort();
    }
    this.activeControllers.clear();
    for (const [idx] of this.cachePool) {
      if (idx > this.currentIndex) {
        this._revokeChunk(idx);
      }
    }
    if (this.isPlaying) {
      this._prefetchSlidingWindow();
    }
  }

  setRate(rate) {
    this.rate = rate;
    if (this.audio) {
      this.audio.playbackRate = Number(rate) || 1.0;
    }
  }

  pause() {
    this.isPlaying = false;
    this.audio.pause();
    this._updateMediaSessionPlaybackState("paused");
  }

  resume() {
    if (!this.isPlaying && this.currentIndex < this.chunks.length) {
      this.isPlaying = true;
      this.audio
        .play()
        .then(() => {
          this._updateMediaSessionPlaybackState("playing");
          this._prefetchSlidingWindow();
        })
        .catch(() => {
          this._playCurrentChunk();
        });
    }
  }

  nextTrack() {
    if (this.currentIndex + 1 < this.chunks.length) {
      this._revokeChunk(this.currentIndex);
      this.currentIndex++;
      this._playCurrentChunk();
      this._prefetchSlidingWindow();
    } else {
      this.onQueueEmpty();
    }
  }

  previousTrack() {
    if (this.currentIndex > 0) {
      this._revokeChunk(this.currentIndex);
      this.currentIndex--;
      this._playCurrentChunk();
      this._prefetchSlidingWindow();
    }
  }

  stop() {
    this.isPlaying = false;
    this.audio.pause();
    this.audio.currentTime = 0;
    this._updateMediaSessionPlaybackState("none");

    for (const ctrl of this.activeControllers) {
      ctrl.abort();
    }
    this.activeControllers.clear();

    for (const [idx] of this.cachePool) {
      this._revokeChunk(idx);
    }
    this.cachePool.clear();
  }

  /**
   * 注册 MediaSession (锁屏界面、控制中心、AirPods 交互与后台保活)
   */
  _setupMediaSession() {
    if (!("mediaSession" in navigator)) return;

    try {
      /* global MediaMetadata */
      navigator.mediaSession.metadata = new MediaMetadata({
        title: this.title,
        artist: this.bookName,
        album: "Reader 在线听书",
        artwork: [
          {
            src: "/favicon.ico",
            sizes: "96x96",
            type: "image/x-icon"
          }
        ]
      });

      navigator.mediaSession.setActionHandler("play", () => {
        this.resume();
      });

      navigator.mediaSession.setActionHandler("pause", () => {
        this.pause();
      });

      navigator.mediaSession.setActionHandler("previoustrack", () => {
        this.previousTrack();
      });

      navigator.mediaSession.setActionHandler("nexttrack", () => {
        this.nextTrack();
      });

      navigator.mediaSession.setActionHandler("stop", () => {
        this.stop();
      });
    } catch (e) {
      // 忽略部分浏览器不支持特定 Action 的情况
    }
  }

  _updateMediaSessionPlaybackState(state) {
    if ("mediaSession" in navigator) {
      try {
        navigator.mediaSession.playbackState = state;
      } catch (e) {
        // 忽略状态同步失败
      }
    }
  }
}

# AGENT_PROJECT_GUIDE (v2.2.4)

面向后续接手智能体的架构、功能与维护指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.2.4`
- **定位**：实现 **iOS Safari 独立桌面模式（添加到主屏幕）锁屏与后台持续听书（TTS）功能**，内嵌微软 Edge 神经语音合成服务与 HTML5 `<audio>` 音频流播放引擎，接入 iOS 锁屏 Media Session 控制中心与段落预加载接力，同时完整保留安卓与 PC 端原有本地语音设置、互不冲突。
- **上一主版本**：`v2.2.3`

---

## 2. 核心架构与功能特性清单 (2.2.4 Features)

### 2.1 服务端内置 Edge TTS 神经语音合成服务 (`EdgeTtsService.kt` & `YueduApi.kt`)
- **直连合成，零外部依赖**：
  - 新增 `src/main/java/com/htmake/reader/utils/EdgeTtsService.kt`，通过 `OkHttpClient` WebSocket 直连微软 `wss://api.msedgeservices.com/tts/cognitiveservices/websocket/v1`，内置 `Sec-MS-GEC`（v1-143.0.3650.75）时间窗令牌校验与 SSML XML 安全转义，无需外部 `8089` 端口容器代理；
  - 将生成后的 MP3 音频流原子写入 `storage/cache/tts/<sha256>.mp3` 磁盘缓存，并限制最大缓存文件数（300 个 LRU 淘汰），确保在 512M 内存受限容器下稳定运行。
- **iOS Safari 音频分片协议适配 (`YueduApi.kt`)**：
  - `/reader3/tts`、`/tts` 接口在 `Dispatchers.IO` 协程上下文执行合成，不阻塞 Vert.x 事件循环；
  - 完整支持 HTTP `Range` 请求头（返回 `206 Partial Content`、`Content-Range` 与 `Accept-Ranges: bytes`），满足 iOS Safari `<audio>` 对 MP3 音频流预检与播放的严格要求。

### 2.2 前端在线音频流播放与 iOS 锁屏控制中心 (`Reader.vue`)
- **双轨并行，零冲突隔离**：
  - 在 `fetchVoiceList()` 中拉取 `/tts/voices` 并合并展示 `🔊 ... (锁屏后台)` 在线神经语音选项；
  - **iOS 自动优选**：iOS 设备默认优先选中在线音频流语音（开箱即支持锁屏后台听书）；PC 与安卓端保持原有本地系统语音优先顺序，互不干扰。
- **iOS 后台防断流核心机制 (`startEdgeAudioSpeech`)**：
  - 全生命周期复用单一 `this.ttsAudio` HTML5 `<audio>` 实例（`playsInline = true`、`preload = "auto"`），在用户首次点击朗读时解锁 iOS `AVAudioSession`，切段与自动切章时保持音频会话不断开；
  - 播放当前段落时自动通过 `fetch` 预加载下 2 段 MP3 音频至浏览器与服务端缓存，消除锁屏状态下的切段空白等待。
- **锁屏媒体控制中心 (`updateMediaSession`)**：
  - 集成 `navigator.mediaSession` 元数据与控制回调（书名、章节名、作者、封面图、播放/暂停、上一段/下一段），支持在 iPhone 锁屏界面或蓝牙耳机上直接控制听书。

### 2.3 界面版本号更新 (`web/package.json` & `Admin.vue`)
- 将 `web/package.json` 与 `Admin.vue` 兜底版本号更新至 `2.2.4`。

---

## 3. 历史版本特性追溯

### 3.1 v2.2.3 特性追溯
- 修复 `filterContent()` 正文净化规则因空 `scope` 与 `"gim"` 多行标志导致安卓 Legado 规则在网页端误删中文小说正文的问题，加入 `@js:` 过滤与 60% 灾难性误删熔断器。

### 3.2 v2.2.2 特性追溯
- 彻底修复安卓/鸿蒙端长按选词后“能自由拖动选择但操作气泡不再弹出”的 `touchcancel` 死锁缺陷。

### 3.3 v2.2.1 / v2.2.0 特性追溯
- 安卓长按划词原生水滴手柄保留、设置提示 5 秒淡出，以及主页左侧栏搜索确认后平滑自动收起。

---

## 4. 运行、构建与测试指引
1. **前端构建与同步**：`cd web && yarn lint && yarn sync`（同步至 `src/main/resources/web/`）。
2. **后端编译与测试**：`./gradlew test`（包含 `EdgeTtsServiceTest` 实时语音合成与缓存验证）。
3. **版本归档目录**：`v2.2.4/source/` 与 `v2.2.4/AGENT_PROJECT_GUIDE.md`。

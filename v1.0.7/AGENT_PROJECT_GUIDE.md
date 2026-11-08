# AGENT_PROJECT_GUIDE (v1.0.7)

面向后续接手智能体的项目自说明与迭代指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v1.0.7`
- **定位**：小说阅读长文本 Edge-TTS 毫秒级流式朗读优化、移动端/iOS WebKit 后台锁屏深度增强与中文字体切片重构版本。
- **上一版本**：`v1.0.6`

---

## 2. 核心架构与功能变更 (Architectural Changes)

### 2.1 长文本 Edge-TTS 流式朗读优化 (TTS Streaming Architecture)
1. **服务端架构 (`server/bin/tts_server.py`)**：
   - **Singleflight 协程防击穿**：在内存中维护请求哈希字典 `in_flight_tasks`，同一文本、音色和语速的并发请求复用同一个异步 Future，杜绝重复向微软 Edge 建立 WebSocket 连接。
   - **LRU + TTL 滑动窗口磁盘清理**：单个音频缓存文件超过 24 小时自动淘汰；当缓存目录总容量超过 200MB 时，自动按最旧访问时间（atime/mtime）修剪至 80% 容量，彻底避免 Docker 卷被音频碎片撑爆。
   - **指数退避重试**：网络抖动时内置最多 2 次指数退避重试（0.5s, 1.0s）。
   - **GET & POST 双协议支持**：支持 `/tts`、`/reader3/tts`、`/api/tts/synthesize`。

2. **前端切块与播放队列 (`web/src/plugins/ttsQueuePlayer.js`)**：
   - **启发式文本切块**：
     - 首包极速策略（Fast First-Chunk）：`Chunk[0]` 严格控制在 15~30 字符以内，确保用户点击播放后 300ms 内极速发声；
     - 常规语义块（Chunk Merging）：后续块按标点贪心合并至 60~120 字符，消除语气割裂；
     - 异常符号清洗：自动清洗 `***`、`---`、大量连续空格等无朗读意义符号。
   - **单 `<audio>` 实例即时换轨**：避免多 `<audio>` 实例触发移动端 Autoplay 策略拦截。
   - **滑动窗口预取与内存回收**：并发受控滑动窗口预取（`PREFETCH_SIZE = 5`），播放完成即时调用 `URL.revokeObjectURL()` 释放浏览器内存。

### 2.2 针对 iOS (iPhone Safari) 的四大专项工程增强
1. **同步静音帧解锁**：在用户物理点击播放的当前同步调用栈中，立即触发一段 Base64 内嵌极短静音 MP3 解锁 WebKit 音频权限，彻底解决异步 fetch 响应后手势授权过期被拦截的问题。
2. **纯事件驱动预取**：不依赖任何 `setTimeout/setInterval`，预加载完全挂载在 `<audio>` 的 `play` 与 `ended` 原生事件上，免疫 iOS 锁屏后的 JS 定时器冻结。
3. **MediaSession 锁屏控制台**：注册 `navigator.mediaSession`（书名、章节名、控制中心与 AirPods 播放/暂停/上一段/下一段动作），大幅提高 iOS 后台存活优先级。
4. **跨章无缝连播**：队列播放完毕触发 `onQueueEmpty`，自动触发下一章正文请求并无缝接入队列。

### 2.3 阅读页正文字体精简与手写体落地
- 正文字体选项精简为：**系统、楷体、宋体、手写体、自定义**。
- **动态切片分包技术**：
  - **楷体**：霞鹜文楷（`LXGW WenKai`）woff2 切片；
  - **宋体**：思源宋体（`Noto Serif SC`）woff2 切片；
  - **手写体**：马善政毛笔手写体（`Ma Shan Zheng`）woff2 切片；
  - 各字体均按 Unicode 区间拆分为几十个 20~80KB 的小分包，按需极速加载，彻底解决全量中文字体体积庞大（10~20MB）导致的白屏与 FOUT 问题。
- 移除了冗余的黑体与仿宋，保留用户自主上传自定义 TTF 字体的独立入口。

### 2.4 控制台版本号动态绑定
- `web/src/views/Admin.vue` 顶栏标签与系统设置中的版本号由写死的字符串改为动态读取 Vuex 中的构建环境变量 `$store.state.version`，全系统版本统一自适应联动。

---

## 3. 构建与运行指引 (Build & Test Guide)

### 3.1 前端构建
```bash
cd web
yarn lint --no-fix   # 代码规范校验（必须 0 error）
yarn build           # 生产环境打包（输出至 web/dist）
```

### 3.2 静态资源同步与后端编译
```bash
# 静态资源同步至 Spring Boot 资源目录
rm -rf src/main/resources/web/*
cp -rf web/dist/* src/main/resources/web/

# Kotlin 后端编译
./gradlew compileKotlin
```

### 3.3 本地独立启动 TTS 服务（可选调试）
```bash
TTS_PORT=8089 TTS_CACHE_DIR=/tmp/tts_cache python3 server/bin/tts_server.py
```

---

## 4. 后续智能体迭代备忘 (Roadmap & Next Steps)
1. **音频倍速记忆**：当前语速和音调已保存在 Vuex 配置中，若需针对特定音色记忆个性化倍速可在 `ttsQueuePlayer.js` 中扩展音色映射表。
2. **多源离线包预热**：若用户阅读纯离线 TXT，可在后台线程按章节批量将切片预生成进本地缓存。

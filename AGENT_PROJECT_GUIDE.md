# AGENT_PROJECT_GUIDE (v2.0.0)

面向后续接手智能体的项目自说明与迭代指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.0.0`
- **定位**：小说阅读全系统稳定性与高并发深度重构里程碑版本（无 DB 存储原子安全保障、全链路分级超时体系、协程 Channel 响应式调度、Rhino JS 死循环看门狗、TTS 缓存机制环境自适应、Token 重启持久化与云备份白名单防 OOM）。
- **上一版本**：`v1.0.7`

---

## 2. 核心架构与 6 大硬伤根治记录 (Architectural Hardening)

### 2.1 纯 JSON 文件存储（No-DB）并发覆写与断电安全根治 (`VertExt.kt`)
- **根因**：原先使用直接文件截断写入，并发读写与断电关机时会导致 `shelf.json` 损坏截断变空文件。
- **解决方案**：
  1. **Write-to-Temp + Atomic Move**：先写入临时文件 `${file.name}.${nanoTime}.tmp`，数据落盘后通过 `java.nio.file.Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` 原子替换目标文件，彻底杜绝半截 JSON 或 0 字节文件损坏。
  2. **进程内读写锁 (`ReentrantReadWriteLock`)**：基于文件规范路径建立全局并发锁映射表，高并发读写 `saveStorage` 与 `getStorage` 时提供严格的线程同步安全。

### 2.2 全链路网络分级超时体系与非轮询协程改造
1. **底层 OkHttp 参数纠偏 (`HttpHelper.kt`)**：
   - `connectTimeout(5, TimeUnit.SECONDS)`：仅管 TCP/TLS 握手，超 5 秒直接判定为死链/黑洞丢包，避免耗尽连接池。
   - `readTimeout(12, TimeUnit.SECONDS)`：握手成功后为海外服务器和长章节生成留足 12 秒缓冲。
   - `retryOnConnectionFailure(false)`：关闭自动重试，失效 IP 立即报错换源，不再成倍耗死。
2. **业务端到端动作分级超时 (`BookController.kt`)**：
   - 搜索链路：`withTimeoutOrNull(6000L)`，配合 SSE 微批次即时推流，国内极速源 0.8s 首屏上屏，海外慢源 4~5s 默默返回合入备用源，兼顾首屏速度与海外源留存。
   - 目录与正文链路：`withTimeoutOrNull(15000L)`，防止点进死链站点无限转圈卡死。
3. **彻底移除 `limitConcurrent` 的 `delay(10)` 轮询反模式 (`BaseController.kt`)**：
   - 采用原生 Kotlin `Channel<Any>(Channel.UNLIMITED)` 生产消费队列；
   - 任务完成由 `channel.send(res)` 触发恢复，消费端由 `channel.receive()` 响应式挂起等待，配合批量 `channel.poll()` 提取已完成任务，零 CPU 空耗。

### 2.3 Edge-TTS 缓存 Linux / Docker 环境自适应 (`server/bin/tts_server.py`)
- **根因**：Linux 内核与 Docker overlay2 驱动默认采用 `noatime/relatime` 挂载，文件被读取时不刷新 `atime`，导致原有 LRU 淘汰退化为 FIFO。
- **解决方案**：
  - 命中音频缓存或向客户端发送文件前，显式触发 `os.utime(cache_file, None)` 强制更新 `mtime`；
  - 清理函数 `clean_expired_and_lru_cache()` 全面依据 `st_mtime` 进行时间计算与升序修剪，恢复真正的热点 LRU 机制。

### 2.4 书源健康度熔断器线程安全与误判隔离 (`SourceCircuitBreaker.kt`)
1. **原子状态安全**：将 `consecutiveFailures`、`totalRequests`、`totalRtMs`、`cooldownUntil` 全面重构为 `AtomicInteger` 与 `AtomicLong`，杜绝多协程统计竞态。
2. **严格隔离空结果与故障**：正常返回 0 条书籍结果判定为合法成功（`recordSuccess`），仅当发生真正网络超时、IOException 或 5xx/403 异常时才计入 `recordFailure`，杜绝冷门小说误杀主力书源。
3. **三级梯队调度机制**：
   - 第一梯队（< 1500ms）：国内极速源，最先发起并发连接；
   - 第二梯队（1500ms ~ 6000ms）：海外正常源，居中调度正常检索；
   - 熔断红线：连续 3 次超时或故障才进入 30 分钟冷却池。

### 2.5 Rhino JS 沙箱死循环看门狗 (`AppConst.kt`)
- **根因**：不可信书源编写死循环 JS 或灾难性正则回溯时，CPU 密集自旋无法响应 Kotlin 协程的挂起点协作式取消，导致工作线程永久卡死。
- **解决方案**：
  - 自定义 `ContextFactory.initGlobal(...)`；
  - 启用 `instructionObserverThreshold = 50000`；
  - 重写 `observeInstructionCount()`：当脚本累计执行指令超过 1,000,000 条时直接抛出 `RuntimeException` 强行击穿中断脚本，杜绝占用 CPU 100%。

### 2.6 Token 掉电持久化与云备份白名单防 OOM
1. **Token 掉电持久化 (`BaseController.kt`)**：
   - 登录时同步持久化 Token 映射至 `storage/data/tokens.json`；
   - 容器重启或升级后，终端携带的 Token 可直接反查恢复用户 Session，用户无感保活。
2. **云备份白名单排查防 OOM (`CloudSyncController.kt`)**：
   - `zipDirectoryToBytes()` 加入严格白名单机制，排除 `tmp`、`cache`、`logs`、`tts`、`bookInfoCache` 及含 `_` 的章节缓存目录；
   - 仅对小于 10MB 的核心配置与规则 JSON 文件打包，杜绝 Base64 内存暴增导致 JVM OOM。

### 2.7 本地语音精简优先与移动端管理后台排版响应式优化
1. **TTS 本地语音智能精简与优先级前置 (`Reader.vue`)**：
   - 过滤系统本地语音，仅保留中文普通话、粤语（香港/广东）与 1~2 种主流英语语音，剔除非必要小语种。
   - 排序策略调整：将过滤后的本地高质量语音置于列表最前端作为默认朗读音色，Microsoft Edge 在线音色作为后续扩展选项。
2. **手机端移动管理后台排版重构 (`Admin.vue`)**：
   - 针对 `@media (max-width: 768px)` 移动屏幕进行响应式深度适配；
   - 侧边栏/分类标签重构为支持横向滑动的自适应胶囊导航条；
   - 数据卡片与表单栅格由多列自动折叠为单列流式布局，去除表格水平截断与按钮挤压变形；
   - 弹窗与对话框宽度适配为手机视口 92%，操作按钮弹性换行，极大提升移动端操作体验。

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

# Kotlin 后端编译与生成可执行 JAR
./gradlew compileKotlin
./gradlew bootJar    # 输出产物：build/libs/reader-2.0.0.jar
```

### 3.3 运行可执行文件
```bash
java -jar reader-2.0.0.jar --reader.server.port=8080
```

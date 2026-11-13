# AGENT_PROJECT_GUIDE (v2.1.8)

面向后续接手智能体的架构、功能与维护指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.1.8`
- **定位**：书源导入沙箱 `$notify` 缺失弹窗报错修复，以及远程书源全协议自动重定向（302 跳转 CDN）与大文件超时拉取加固。
- **上一主版本**：`v2.1.7`

---

## 2. 核心架构与功能特性清单 (2.1.8 Features)

### 2.0 书源沙箱入库 `$notify` 异常彻底修复 (`element.js` & `BookSourceManage.vue`)
- **根因分析**：
  - 在 `web/src/plugins/element.js` 中按需引入 Element UI 组件时，未引入 `Notification`，也未注册 `Vue.prototype.$notify`；
  - 当用户在书源预检沙箱中勾选优质源点击入库时，若存在丢弃的失效源，前端试图调用 `this.$notify` 显示气泡通知，抛出 `TypeError: e.$notify is not a function`，并被外层 `catch` 捕获弹出“保存书源失败”的误导性提示。
- **加固方案**：
  - 在 `web/src/plugins/element.js` 中完整引入 `Notification` 并绑定至 `Vue.prototype.$notify = Notification`；
  - 在 `BookSourceManage.vue` 中对通知调用增加防御性降级判断 `typeof this.$notify === "function"`，若异常自动回退至 `this.$message.success`，彻底杜绝调用链报错。

### 2.1 远程书源跨域跳转与大文件拉取加固 (`BookSourceController.kt` & `BookSourceManage.vue` & `Admin.vue`)
- **根因分析**：
  - 像 `https://www.yckceo.com/yuedu/shuyuans/json/id/1335.json` 等远程书源采用 302 临时重定向至外部 CDN（如 `gcore.jsdelivr.net`），文件大小超 2.6MB；
  - 后端原有 `readRemoteSourceFile` 采用 `webClient.getAbs(url).timeout(3000)`，超时时间仅 3 秒，在公网发生 TLS 握手 + 重定向握手 + 2.6MB 传输时必然超时报错；
  - 原前端在服务端接口报错后直接回退到浏览器端发起 `Axios.get(url)`，而目标站点 302 阶段无 CORS 头，触发浏览器 CORS 跨域拦截导致最终彻底失败。
- **加固方案**：
  - **核心双通道网络模型 (`BookSourceController.kt`)**：
    1. 优先通道：调用项目成熟的 `io.legado.app.help.http.okHttpClient`，内置完整的 HTTP/HTTPS 全协议重定向（`followRedirects(true)`、`followSslRedirects(true)`）、Gzip 自动解压、现代与兼容 TLS 协商、标准浏览器 User-Agent 以及 20 秒超时配置；
    2. 降级通道：若 okHttp 异常，自动降级至 WebClient 并提供 20 秒充足超时与标准请求头；
  - **前端智能错误透传 (`BookSourceManage.vue` & `Admin.vue`)**：
    - 前端在直连兜底失败时，优先捕获并透传服务端返回的具体失败原因（如证书问题、超时、404 等），杜绝模糊的“Network Error”。

---

## 3. 历史版本特性追溯

### 3.1 v2.1.7 特性追溯
- **正文划词气泡一键过滤广告 (`Reader.vue` & `ReplaceRule.vue` & `vuex.js`)**：
  - 正文选中文本气泡专属「过滤广告」入口；
  - 零正则极简交互，支持「精准过滤」、「整行过滤」以及「智能链接过滤」；
  - 智能顶级根域名提取（自动剥离子域并兼容 `.com.cn` 等复合顶级域名）；
  - 跨平台换行符（CRLF/LF）隔离与安全匹配；
  - 过滤即时生效且锁定当前阅读进度不跳页。

### 3.2 v2.1.6 特性追溯
- **书籍封面轻量并发与分批接力扫描 (`BookController.kt` & `Index.vue` & `Admin.vue` & `BookManage.vue`)**：
  - 4 线程轻量并发后端适配，单批次抓取 5 张封面，抓满即断开 SSE 释放服务端算力；
  - 断点记忆与「获取后 5 张封面」断点续扫机制。

---

## 4. 运行、构建与测试指引

1. **前端构建与同步**：
   ```bash
   cd web
   yarn build
   yarn sync
   ```
2. **后端编译与语法校验**：
   ```bash
   ./gradlew compileKotlin
   ```
3. **单元与集成测试**：
   ```bash
   ./gradlew test
   ```
4. **本地服务启动**：
   ```bash
   ./build.sh serve
   ```

---

## 5. 已修复的 Bug 与技术改进 (Fixed Bugs & Technical Enhancements)

1. **书源导入预检暂存沙箱 `$notify is not a function` 修复**：
   - `element.js` 补齐 `Notification` 组件注册，沙箱入库完成通知恢复正常并增加兜底降级。
2. **远程书源 302 重定向至 CDN 与大文件拉取失败修复**：
   - 解决 3 秒短超时与浏览器 CORS 冲突，后端基于 `okHttpClient` 20s 自动跳转与 Gzip 完整拉取 2.6MB+ 书源，全协议重定向测试验证通过。
3. **低配 VPS 封面抓取 CPU 飙满 100% 痛点解决**：
   - 降级为 4 线程安全并发，单批次抓取 5 张即断开连接释放资源，断点记忆 `lastIndex` 接力续扫。
4. **书源缺乏批量删除与治理手段**：
   - 表格多选与批量删除确认弹窗，以及后端 `/deleteBookSources` 批量删除能力。
5. **跨书源换源时目录章节不一致导致的跳章串章**：
   - 引入 `fuzzyMatchChapter` 智能对齐引擎。
6. **快速翻页导致的自动换源误触发防范**：
   - 限制仅在阅读驻留时间 >= 8 秒且阅读进度到达 1/2 时才触发换源。
7. **书源置顶排序在服务端未持久化顺序的缺陷修复**：
   - 后端重构排序保存逻辑，严格按传入顺序重建书源列表并持久化。
8. **搜书结果页右上角幽灵「登录」按钮与误触强制登录修复**：
   - 清理多余幽灵登录分支，搜书状态下收敛为【书架】与【加载更多】。
9. **书源全场景支持「从本地导入」(.json / .txt / 拖拽读取)**：
   - 「书源管理与体检中心」与「控制台 - 书源设置」同步增加本地导入与拖拽解析。
10. **批量导入书源时的死源泛滥与数据库污染 (事前预检暂存沙箱机制落地)**：
   - 纯内存隔离暂存沙箱与 8 并发体检测速，实现零死源零污染入库。

---

## 6. 待讨论商定功能与路线图 (Roadmap & Discussion)
- **控制台书源设置 vs 书源管理中心架构协同**：控制台聚焦系统默认源底包、权限配置与沙箱调试，书源运营/体检/批量操作统一引导至管理中心。
- 离线缓存并发预加载队列防封限流。
- 正文排版字间距微调与分栏阅读适配。

# Reader v1.0.2 智能体工程指南 (Agent Project Guide)

> 本文档面向后续接手或维护该项目的智能体与工程师，记录系统架构、本次修复的关键缺陷、设计决策与后续迭代建议。

---

## 1. 核心架构与模块职责

- **后端核心**：
  - 基于 Kotlin + Vert.x + Spring Boot 构建的高性能网络阅读器服务端。
  - 服务端入口为 `com.htmake.reader.ReaderApplication`（切勿在无头容器中拉起依赖 JavaFX X11 的 `ReaderUIApplication`，服务端编译统一使用 `./build.sh cli assemble`）。
  - 控制器矩阵：
    - `BookController.kt`：书架增删改查、多源搜索、换源 (`setBookSource`)、静默备用源发现 (`searchBookSourceSSE`)、阅读进度等。
    - `CloudSyncController.kt`：云同步与全量打包备份治理，支持 GitHub 与 WebDAV 存储。**推送到 GitHub 强制采用 PBKDF2/SHA-256 + AES-256-GCM 强加密**。
    - `BaseController.kt` / `YueduApi.kt`：系统诊断日志 (`getSystemLogs`)、缓存清理与状态监控。
- **前端架构**：
  - 基于 Vue 2.6 + Element-UI 构建的响应式 SPA，针对移动端与桌面端分别优化。
  - `Index.vue`：前台主界面，包含书架卡片、分类标签、顶部操作按钮与书籍封面快捷替换。
  - `Admin.vue`：后台管理控制台，包含系统健康度概览、用户管理、书源治理（导入、失效体检与批量清理）、运行诊断日志实时抓取、云端同步备份。
  - `BookSource.vue`：换源组件，支持源加载反馈、【当前】使用源与【已使用】历史源辨识。

---

## 2. 本版本 (v1.0.2) 重点解决的问题与优化

### 2.1 换源速度极慢与重复请求问题修复
- **根本原因排查**：
  1. 换源接口 `setBookSource` 原逻辑未先读取本地已缓存的书源搜索结果，每次换源均通过 `WebBook.getBookInfo` 对远程目标书籍详情页发起冗余 HTTP 请求（耗时 2~5s）。
  2. `setBookSource` 内部同步抓取全量目录后，前端 `Reader.vue` 收到换源成功事件时又再次调用了 `loadCatalog(true, true)`，触发二次全网抓取。
- **优化方案**：
  1. `BookController.kt` 中优先检索本地内存与磁盘中保存的搜索源列表，直接复用已有的书籍信息，避免额外的远程 HTTP 请求。
  2. 为目录抓取增加协程超时保护（`withTimeoutOrNull(12000L)`），杜绝因个别卡顿书源导致整个请求长时间无响应。
  3. `Reader.vue` 换源后调整为 `this.loadCatalog(false, true)`，直接从服务端本地存储读取刚刚更新完成的目录数据，目录切换时间缩短至毫秒级。

### 2.2 顶部导航栏按钮排列与管理下拉菜单解耦
- **原因**：原 `.shelf-title` 内部子按钮使用 `float: right`，但新增的 `<el-dropdown>` 管理菜单并非纯浮动块级元素，导致管理菜单意外流式排布在左侧标题 `书架 (数量)` 旁边，与右侧的「书海、刷新、编辑」脱节。
- **优化方案**：
  - 将 `.shelf-title` 彻底重构成标准 Flexbox 布局。
  - 左侧固定为 `shelf-title-left`（书架书籍计数与折叠菜单），右侧为 `shelf-title-actions`。
  - 按钮从左至右整齐排布：`[书海] [刷新] [编辑] [管理 ▾ (admin)]`，视觉统一且彻底避免浮动错位。

### 2.3 搜索书籍时不检索封面以极大提速
- **需求**：多源高并发搜索时，图片资源下载极其占用网络连接与带宽。
- **优化方案**：
  - 后端：在 `BookList.kt` 中，当 `isSearch == true` 时，跳过 `ruleCoverUrl` 提取规则解析，不返回网络图片地址。
  - 前端：在 `Index.vue` 中，当 `isSearchResult` 为真时，展示极简图书图标占位符（`.search-cover-placeholder`），不再向后端发起 `/reader3/cover?path=...` 图片代理请求。

### 2.4 主页书架直接支持「修改图书封面」
- **实现位置**：
  - 在主页书架编辑状态下（点击「编辑」后），每本书籍卡片操作区新增「封面」图标按钮（`el-icon-picture`）。
  - 在书籍详情弹窗（`BookInfo.vue`）中也增设了「修改封面」快捷入口。
  - 弹窗功能：自动并发调用 `/getAvailableBookSource`，从该书在各大书源的检索结果中智能提取高清候选封面供用户一键点选替换；同时支持用户手动粘贴外部图片 URL 并即时保存刷新。

### 2.5 失效书源体检（实测书名扫描 + 后端故障记录）
- **实现位置**：`Admin.vue` 失效源治理面板。
- **双模式检测**：
  1. **书名实测扫描模式（前台同款机制）**：模拟用户真实搜索行为，使用指定测试书名（默认《斗罗大陆》）、超时时间与并发线程，向所有书源并发发送请求，捕获超时、HTTP 4xx/5xx 或连接拒绝，精准判定并列出失效原因。
  2. **后端运行异常记录模式**：直接拉取服务端运行时拦截并记录的故障书源。
  - 界面提供一键清理全部失效源与单个移除功能，并对判定原理进行了通俗说明。

### 2.6 后台管理系统诊断运行日志空白问题修复
- **根本原因**：`Admin.vue` 中调用接口时书写为 `Axios.get("/reader3/getSystemLogs")`，由于全局 Axios 实例的 `baseURL` 已经包含了 `/reader3`，导致最终请求地址变为 `/reader3/reader3/getSystemLogs` 触发 404 Not Found。
- **优化方案**：修改为 `Axios.get("/getSystemLogs")`，正确对齐路由，实时输出运行日志。

---

## 3. 运行、测试与构建指引

### 3.1 本地容器化运行
```bash
# 启动本地容器 (端口 8090)
docker run -d --name reader-local -p 8090:8080 \
  -v $(pwd)/storage:/app/storage \
  -v $(pwd)/logs:/app/logs \
  reader:local
```

### 3.2 编译与镜像构建
```bash
# 1. 编译前端资源
cd web && yarn build && cd ..
rm -rf src/main/resources/web/* && cp -r web/dist/* src/main/resources/web/

# 2. 编译服务端无头可执行 Jar (严禁使用无参数 gradlew build 引入 JavaFX)
./build.sh cli assemble

# 3. 重新构建 Docker 镜像
docker build -t reader:local .
```

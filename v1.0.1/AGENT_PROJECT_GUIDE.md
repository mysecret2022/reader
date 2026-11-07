# Reader 现代化增强版 (v1.0.1) 智能体工程自说明文档

> **版本定位**：v1.0.1 (当前基线版本)  
> **维护说明**：基于原始仓库二次开发与维护，完整适配现代容器化与本地宿主多环境部署。遵循本地构建与验证规范，未获得用户明确指令前**严禁执行远程推送 (git push)**。

---

## 1. 核心架构与模块功能

- **前端架构 (web/)**：
  - 基于 Vue 2 + Element UI + Vuex + Vue Router 构建。
  - **核心视图与组件**：
    - `Index.vue`：现代化书架视图，左上角显示当前系统版本号 `v1.0.1`。
    - `Reader.vue`：阅读与听书视图，集成微软 Edge TTS 多音色原声朗读，具备段落高亮追踪与自动翻页。
    - `Admin.vue`：独立管理员控制面板（`/#/admin`），具备独立导航布局，绝无跑行重叠风险。
    - `Login.vue`：沉浸式磨砂质感登录界面。
- **后端架构 (src/main/java/)**：
  - 基于 Spring Boot + Vert.x 异步网关架构。
  - `BaseController.kt`：鉴权、用户会话、管理权限识别、用户空间隔离与多租户存储基类。
  - `UserController.kt`：用户认证、初始管理员自举注入、密码重置、系统全局配置修改 (`getSystemConfig` / `saveSystemConfig`) 与缓存清理 (`clearSystemCache`)。
  - `BookController.kt`：书籍流转、书架管理、多源异步并发检索与章节内容清洗解析。
  - `YueduApi.kt`：REST API 映射路由，支持 `/reader3/tts`、`/tts`、静态文件路由与书籍导入导出。
- **听书微服务 (server/bin/tts_server.py)**：
  - 基于 Python 3 + `edge-tts` 的原生轻量 HTTP 服务，监听内部 `127.0.0.1:8089`。

---

## 2. 专项问题修复与功能增强记录

### 2.1 阅读左上角版本号与系统工程版本全面对齐 (v1.0.1)
- **原因**：前端 `package.json` 及后端 `cli.gradle`、`build.gradle.kts` 残留了原版本的 `2.5.4` 标识，导致左上角版本号文本显示为 `v2.5.4-...`。
- **修复方案**：
  1. `web/package.json` 中的 `version` 统一调整为 `1.0.1`。
  2. `web/vue.config.js` 统一输出标准简洁版本标识 `v1.0.1`。
  3. 后端 `cli.gradle` 与 `build.gradle.kts` 版本统一调整为 `1.0.1`，生成标准 `reader-1.0.1.jar`。
  4. `Dockerfile` 构建脚本改用通配符 `reader-*.jar`，平滑兼容版本号变更。

### 2.2 控制台侧边栏菜单排版严重错乱挤占修复
- **原因**：`web/src/plugins/element.js` 未导入并注册 `Menu` 与 `MenuItem` 组件，导致 `<el-menu-item>` 降级为未知内联 HTML 标签，导致“系统状态总览用户权限治理书源健康管理诊断运行日志本地书仓维护”全部水平连缀成行。
- **修复方案**：
  1. 在 `element.js` 中完整引入并注册 `Menu` 与 `MenuItem`。
  2. `Admin.vue` 侧边栏全面升级为完全自主掌控的独立弹性卡片项（`sidebar-nav-item`），具备独立的图标、文本、间距、圆角与高亮光晕，从根本上杜绝挤占与跑行问题。
  3. 全面支持移动端与小屏幕自适应。

### 2.3 系统总览四大指标卡片全面交互化（可看、可点、可改）
- **注册用户配额卡片**：展示当前人数与系统上限（如 `1 / 50 人`），点击卡片直接弹出配额调整窗口，可直接修改用户总上限，并支持一键前往用户管理。
- **已配置书源卡片**：展示书源数量，点击卡片弹出书源快捷管理窗口，支持**一键全部启用**、**一键全部禁用**与书源健康体检。
- **书架书籍总数卡片**：展示当前书架藏书量，点击卡片弹出书架全局整理窗口，支持查看书籍并移除失效书籍。
- **注册准入控制卡片**：展示当前自主注册开启状态，点击卡片可一键切换“开放注册 / 暂停注册”，无需繁琐配置。

### 2.4 快速维护操作升级为“管理员在线可编辑配置”面板
- **新增接口**：
  - `GET /reader3/getSystemConfig`：获取当前系统配置。
  - `POST /reader3/saveSystemConfig`：管理员在线保存系统配置并持久化至 `storage/data/systemConfig.json`。
  - `POST /reader3/clearSystemCache`：一键清理失效音频与解析缓存。
- **支持在线修改的关键业务策略**：
  - 自主注册开关（`allowRegister`）
  - 注册专属邀请码（`inviteCode`，留空不限制）
  - 系统用户总数配额（`userLimit`，1 ~ 500 人）
  - 单用户藏书配额（`userBookLimit`，10 ~ 1000 本）
  - 默认听书音色（`defaultVoice`，微软晓晓、云希、云扬、晓依等）
  - 多书源并发搜索线程数（`concurrentSearchCount`，8 ~ 48 线程）
  - 章节正文自动缓存开关（`cacheChapterContent`）

---

## 3. 本地构建与验证状态

- **镜像与容器**：`reader:local` 已重新构建，运行容器 `reader-local`（映射端口 `8090:8080`）。
- **实测访问**：`http://localhost:8090/`
- **GitHub 远端状态**：已在本地提交分支，**严格遵守规则，未向 GitHub 进行任何 push 操作**。

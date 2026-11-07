# Reader 现代化增强版 (v1.0.1) 智能体工程自说明文档

> **版本定位**：v1.0.1 (稳定热修版本)  
> **维护说明**：基于原始仓库二次开发与维护，完整适配现代容器化与本地宿主多环境部署。遵循本地构建与验证规范，未获得用户明确指令前**严禁执行远程推送 (git push)**。

---

## 1. 核心架构与模块功能

- **前端架构 (web/)**：
  - 基于 Vue 2 + Element UI + Vuex + Vue Router 构建。
  - **核心视图**：
    - `Index.vue`：现代化书架视图，支持书架分组、书源搜索、右上角用户安全治理抽屉菜单（管理控制台/修改密码/确认注销）。
    - `Reader.vue`：阅读与听书视图，集成微软 Edge TTS 多音色原声朗读，具备段落高亮追踪与自动翻页。
    - `Admin.vue`：独立管理员控制面板（`/#/admin`），聚合系统概况、用户权限治理、书源健康排查与实时运行诊断日志。
    - `Login.vue`：沉浸式磨砂质感登录界面，支持密码隐藏显示、多用户快速登录与自动保存凭证。
- **后端架构 (src/main/java/)**：
  - 基于 Spring Boot + Vert.x 异步网关架构。
  - `BaseController.kt`：鉴权、用户会话 (`checkAuth`)、管理权限识别 (`checkManagerAuth`)、用户空间隔离 (`getUserNameSpace`) 与多租户存储基类。
  - `UserController.kt`：用户认证、初始管理员自举注入、密码重置、权限控制与 WebDAV/书仓功能开关。
  - `BookController.kt`：书籍流转、书架管理、多源异步并发检索 (`searchBookSourceSSE`) 与章节内容清洗解析。
  - `YueduApi.kt`：REST API 映射路由，支持 `/reader3/tts`、`/tts`、静态文件路由与书籍导入导出。
- **听书微服务 (server/bin/tts_server.py)**：
  - 基于 Python 3 + `edge-tts` 的原生轻量 HTTP 服务，监听内部 `127.0.0.1:8089`，支持动态音色、语速、音调调节与智能分句。

---

## 2. v1.0.1 本次热修专项修复记录

### 2.1 听书功能直接失效修复 (Edge TTS 路由错配与端点兼容)
- **原因**：前端部分场景中 `this.api` 基础路径与 TTS 端点拼接产生了多重前缀（如 `/reader3/reader3/tts`），后端未配置多路径容错导致 404；且前端存在 ESLint 告警阻断。
- **修复方案**：
  1. 在 `YueduApi.kt` 中为 TTS 处理器同时注册 `/reader3/tts`、`/tts`、`/reader3/reader3/tts` 三重端点。
  2. 在 `Reader.vue` 中统一使用标准的 `${this.api}/tts` 进行请求，并移除控制台调试语法噪音。
  3. 实测通过 `curl -i "http://localhost:8090/reader3/tts?text=测试"`，返回标准 `audio/mpeg` 流。

### 2.2 书源“加载更多”直接显示“没有更多了”缺陷修复
- **原因**：在 `BookController.kt` 的 `searchBookSourceSSE` 与 `searchBookSource` 中，原逻辑会在未刷新时自动读取该书已缓存的书源列表，并将 `lastIndex` 强制覆盖为最后一个书源在用户书源库中的下标；当该小说是在只有一个书源或书源排在库尾时导入的，`lastIndex` 被强行计算为 `userBookSourceList.size - 1`，立即触发 `lastIndex >= size - 1` 退出分支，根本不检索后续或新导入的书源。
- **修复方案**：
  1. 移除对已存书源尾部下标的强行劫持逻辑，尊重客户端发送的实际分页下标。
  2. 当 `lastIndex >= userBookSourceList.size - 1` 时，重置 `lastIndex = -1`，允许用户再次点击“加载更多”或“刷新”时完整轮询全部书源。
  3. 在 `BookSource.vue` 中刷新时清理群组分页映射表，并在搜索完成无新增书源时给予温和的“暂未检索到更多新书源”轻提示。

### 2.3 点击主页 admin 报错“不支持的操作”与注销菜单改造
- **原因**：原 `Index.vue` 中顶部用户名按钮直接绑定了 `@click="logout"`，而未配置 `SECURE` 模式时，`UserController.kt` 的 `logout` 方法包含了硬编码检查 `if (!appConfig.secure) return returnData.setErrorMsg("不支持的操作")`。
- **修复方案**：
  1. 移除 `UserController.kt` 中对 `!appConfig.secure` 的无意义阻断，确保所有运行模式下均可正常销毁会话与清理令牌。
  2. 将顶部用户名按钮升级为优雅的下拉交互菜单：
     - **管理后台**：管理员一键跳转进入 `/#/admin`。
     - **修改密码**：弹出密码修改对话框，自验证并支持修改。
     - **退出登录**：增加确认提示框，避免误点直接登出。

### 2.4 默认管理员自举与首个注册用户自动绑定管理员
- **设计与实现**：
  1. 在 `BaseController.kt` 和 `UserController.kt` 中实现 `getUserMap()` 自举机制：当系统初始化且用户库为空时，自动创建默认管理员（账号 `admin`，初始密码 `admin`，已开启 WebDAV 与本地书仓，`is_admin = true`）。
  2. 在新用户自主注册逻辑中，若系统尚无普通用户，将首个注册的新用户自动设为 `is_admin = true`。
  3. `checkManagerAuth` 鉴权升级：当前登录用户若为管理员（`is_admin = true` 或用户名为 `admin`），自动放行管理类 API（`getUserList`、`addUser`、`resetPassword`、`updateUser`、`deleteUsers`），无须依赖外部固定密钥。
  4. 支持密码最低位数为 4 位，方便个人日常快捷使用与个性化重置。

---

## 3. 运行、测试与构建验证指引

### 3.1 本地测试容器
```bash
# 启动本地容器 (端口映射 8090)
docker run -d --name reader-local -p 8090:8080 -v $(pwd)/storage:/app/storage reader:local

# 访问地址
http://localhost:8090/
```

### 3.2 默认凭证
- **默认管理员账号**：`admin`
- **默认管理员密码**：`admin`（登录后可通过右上角用户菜单随时修改）

### 3.3 自动化构建流程
1. **前端构建**：`cd web && yarn build`（输出自动同步至 `src/main/resources/web/`）。
2. **后端打包**：`./build.sh cli assemble`（生成 `build/libs/reader-2.5.4.jar`）。
3. **镜像构建**：`docker build -t reader:local .`。

---

## 4. 智能体迭代与维护规范 (Agent Notes)

- **远程推送禁令**：本工程当前属于本地热修与用户验收阶段，除非收到用户的显式文本指令（如“推送”、“可以推送了”），绝对禁止执行 `git push`。
- **文件编码规范**：所有批处理或 Windows 脚本严禁使用无 BOM 的 UTF-8 编码，统一使用纯英文或 GBK 编码（CRLF 换行）。
- **零技术黑话交付**：向用户交付功能或解答疑问时，用直观易懂的生活化语言说明结论与操作方法，不堆砌晦涩代码名词。

# AGENT_PROJECT_GUIDE (v2.1.0)

面向后续接手智能体的架构、功能与维护指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.1.0`
- **定位**：PWA 本地化自给自足与原生安装体验大升级大版本。彻底摆脱对 Google 外部 CDN 的强依赖，解决断网或国内网络环境下 Service Worker 无法离线激活的问题；并在书架与导航栏原生支持【📲 安装到手机桌面】一键引导与唤起。
- **上一主版本**：`v2.0.10` / `v2.0`

---

## 2. 核心架构与功能特性清单 (2.1.0 Features)

### 2.1 PWA 纯本地离线自给自足架构 (`web/vue.config.js`)
- **背景与痛点**：以往 Workbox 默认通过 Google CDN (`https://storage.googleapis.com/workbox-cdn/...`) 加载 `workbox-sw.js` 与策略运行时。国内网络环境下该域名通常被封锁，导致 Service Worker 初始化失败，无法实现离线缓存与 PWA 功能。
- **技术实现**：
  - 在 `web/vue.config.js` 的 `pwa.workboxOptions` 中配置 `importWorkboxFrom: "local"`。
  - 构建后将 `workbox-v4.3.1` 全量核心策略依赖与运行时自动拷贝至本地静态资源目录，并同步输出至 `src/main/resources/web/`。
  - `service-worker.js` 改为相对路径 `importScripts("workbox-v4.3.1/workbox-sw.js")` 及本地模块加载，实现 100% 离线可用和内网自洽。

### 2.2 浏览器原生 PWA 安装捕获与交互引导 (`Index.vue` & `main.js` & `App.vue` & `index.html`)
- **早捕获保底机制**：
  - 在 `web/public/index.html` 首屏阶段及 `web/src/main.js`、`App.vue` 全局监听浏览器原生 `beforeinstallprompt` 事件，防止首屏 Vue 初始化较慢时遗漏浏览器触发的单次安装事件。
  - 接入 Vuex 响应式状态管理 (`$store.state.pwaInstallPrompt`)，实现全局状态同步。
- **UI 友好呈现与唤起交互**：
  - **书架顶部快捷入口**：书架主标题栏 (`shelf-title-actions`) 动态呈现醒目的【📲 安装到手机桌面】按钮；仅在常规书架模式下展示（搜索/探索模式下避让以防拥挤）；
  - **书架沉浸提示横幅**：书架顶部专属提示卡片（`.pwa-install-banner`），附带全屏原生阅读优势说明与一键安装、支持单次会话临时关闭；
  - **侧边栏常驻入口**：侧边抽屉导航菜单内常驻展示安装入口，方便随时调用；
  - **优雅降级与全平台兼容**：若用户已在 PWA/独立应用模式下运行，自动隐藏安装按钮；点击后通过 `prompt.prompt()` 原生调起系统安装弹窗，并根据用户选择给出友好交互提示。

---

## 3. 运行、构建与测试指引

1. **前端构建与同步**：
   ```bash
   cd web
   yarn build
   # 或一键编译并同步至服务端静态资源目录
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

## 4. 已修复的 Bug 与技术改进 (Fixed Bugs & Technical Enhancements)

1. **PWA 安装事件二次唤起崩溃异常 (Fatal DOMException: The prompt() method may only be called once)**：
   - **原因**：根据 W3C 规范，`BeforeInstallPromptEvent.prompt()` 每个事件实例仅允许调用一次。若用户初次点击安装后取消，而全局未清除失效的 prompt 事件引用，用户再次点击时将抛出 `InvalidStateError / DOMException`，陷入不可用状态。
   - **解决方案**：在 `installPwa()` 执行 `await promptEvent.prompt()` 后立即将 Vuex 及全局 `window.deferredPrompt` 置空；在 `catch` 异常分支也增加重置保底。
2. **Workbox 依赖 Google 外部 CDN 导致 SW 激活失败与离线瘫痪**：
   - **原因**：原有配置缺失 `importWorkboxFrom: "local"`，Workbox 默认远程请求 `storage.googleapis.com`，导致国内或内网无外网环境下离线加载失败。
   - **解决方案**：配置本地 Workbox 依赖包同步机制，68 个策略脚本全量本地化自包含。
3. **书架顶部操作栏在搜索状态及移动端窄屏下的挤压与折行问题**：
   - **原因**：移动端宽度小于 375px 时，标题栏同时容纳书架数量、长文案安装按钮、刷新、编辑易产生布局溢出。
   - **解决方案**：限制安装快捷按钮仅在非搜索模式 (`!isSearchResult`) 下展示，并配置 `white-space: nowrap` 及响应式字体调整。
4. **管理控制台默认配置版本号回退值过旧**：
   - **原因**：`Admin.vue` 中 `systemConfig.version` 缺省 fallback 为 `2.0.1`。
   - **解决方案**：同步升级为 `2.1.0`。

---

## 5. 路线图与待改进项 (Roadmap & Next Steps)

### 5.1 自动换源与智能切源机制（下次优化核心重点）
- **功能定位与开关准则**：
  - **默认严格关闭**：自动换源功能**默认必须保持关闭**，避免后台在用户未察觉时擅自频繁切源，造成排版和阅读进度混乱。
  - **用户主动开启入口**：必须由用户在**小说正文阅读界面**点开底栏/顶栏【设置】面板，在设置项中**手动开启**「自动换源」功能。
- **配套实现设计**：
  - 异常章节触发：仅在正文加载超时（如 15s 保底）、正文内容为空或匹配到劣质防盗章节标记时，才在后台基于可用书源列表进行静默切源与测速重试；
  - 书源健康度测速与权重排名：结合各书源延迟与有效内容返回率动态优选。

### 5.2 离线缓存增强
- 离线章节并发预加载限流防被封。
- 网络重连自动恢复下载队列。

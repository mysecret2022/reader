# Reader (阅读3 Web版) v2.5.4 智能体自说明与维护指南

## 1. 项目定位与背景
- **原上游项目**：https://github.com/hectorqin/reader 及衍生分支 https://github.com/rorschach883/reader
- **个人维护仓库**：https://github.com/mysecret2022/reader
- **架构体系**：
  - 后端：Kotlin + Java 11 + Spring Boot 2.1.6 + Vert.x 异步 Web 引擎 + H2 嵌入式数据库。
  - 前端：Vue 2.6 + Element UI + Stylus，编译后静态资源注入后端 `src/main/resources/web`。
  - 容器构建：Eclipse Temurin 11 (JRE) Debian 基础镜像 + Tini 进程守护 + 多架构支持（`linux/amd64` 与 `linux/arm64`）。
  - CI/CD 自动化：GitHub Actions 全流程自动化构建并推送至 GHCR（`ghcr.io/mysecret2022/reader:latest`）。

---

## 2. 关键定制特性与 Bug 修复记录
1. **现代化全屏登录/注册界面 (参考 lxserver 风格)**：
   - 移除原版简陋的弹出框 (`<el-dialog>`) 登录模式。
   - 新增全屏极光动态流光背景、毛玻璃卡片设计、平滑滑块切换「账号登录 / 用户注册」、密码显示切换与抖动错误反馈。
   - 实现组件：[`web/src/views/Login.vue`](file:///root/.gemini/antigravity/scratch/reader/web/src/views/Login.vue)，并在 [`web/src/App.vue`](file:///root/.gemini/antigravity/scratch/reader/web/src/App.vue) 中统一挂载。
2. **多架构 Docker 镜像支持修复**：
   - 原因：此前 Alpine 版 Temurin 11 在 Docker Hub 上缺少 ARM64 架构支持，导致 Buildx 跨架构构建失败。
   - 解决方案：切换至官方多架构支持的 `eclipse-temurin:11-jre`，构建多平台镜像。
3. **CI/CD 自动化全链条闭环**：
   - 原因：GitHub Actions runner 检出代码时不包含预构建的 JAR，若直接在 Docker 镜像内构建会导致 QEMU 模拟 ARM64 构建极其缓慢。
   - 解决方案：在 Actions 步骤中先通过 Node 18 执行 `yarn sync` 构建前端，再由 Java 11 执行 `./build.sh cli assemble` 生成 Fat JAR，最后通过 Buildx 极速封装并推送 GHCR。
4. **空章节列表索引越界 Bug 修复 (`Index 0 out of bounds for length 0`)**：
   - 原因：当部分书源（如顶点小说等失效或返回空列表的书源）解析目录返回 0 章节时，`BookChapterList.kt` 中记录首章信息的日志代码直接访问 `chapterList[0]`，引发未捕获的越界异常。
   - 解决方案：在访问 `chapterList[0]` 打印首章调试信息前增加 `if (chapterList.isNotEmpty())` 判空防护。
5. **前端界面精简与多余外链/图标移除**：
   - 移除侧边栏「其它」一栏（公众号与 Telegram 频道宣传外链）。
   - 移除侧边栏底部的 GitHub 仓库图标与日夜间切换按钮。
6. **服务端环境 webview 智能优雅降级**：
   - 原因：原项目移植自 Android 版 Legado。部分书源配置了 `{"webView": true}`，在安卓手机上会使用内置系统的 `android.webkit.WebView` 执行，而在 Linux/Docker 等服务端 JVM 无安卓 WebView 组件，原代码中直接粗暴 `throw Exception("不支持webview")` 导致书源报错中断。
   - 解决方案：在 `AnalyzeUrl.kt` 中移除直接抛错的逻辑，改为智能降级为标准 HTTP 客户端请求，并打印友情日志。这使得绝大部分只是误标或轻量爬虫校验的书源（如《总裁小说》等）在服务端上能顺利抓取章节与正文。

---

## 3. 本地编译与测试指引
- **前端编译**：
  ```bash
  cd web
  yarn --ignore-engines
  export NODE_OPTIONS=--openssl-legacy-provider
  yarn sync
  ```
- **后端 Fat JAR 打包**：
  ```bash
  chmod +x ./gradlew ./build.sh
  ./build.sh cli assemble
  # 生成产物：build/libs/reader-2.5.4.jar
  ```
- **Docker 启动验证**：
  ```bash
  docker compose up -d
  ```

---

## 4. 后续迭代路线与维护建议
- 支持 Web 端主题颜色与背景壁纸自定义持久化。
- 完善多用户书源隔离与导入配额限制。

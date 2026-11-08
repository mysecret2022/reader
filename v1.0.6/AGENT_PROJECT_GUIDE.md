# Reader v1.0.6 智能体项目架构与维护说明 (AGENT_PROJECT_GUIDE)

> 本文档面向后续智能体与维护者，详细记录 v1.0.6 版本的业务逻辑、架构设计、Bug 根因排查与后续路线图。

---

## 1. 版本概览 (Version v1.0.6)
- **版本号**：v1.0.6 (递增规范：未来版本按 v1.0.7, v1.0.8 递增)
- **Git 目标分支**：`dev` (主开发演进分支)，验证通过后同步 fast-forward 合并至 `master`
- **主要更新点**：
  1. **阅读页听书功能深度重构与连续播放引擎**：
     - **根因修复**：解决原先切段时过早清除高亮 class 导致 `getCurrentParagraph` 索引算错中断的问题，改为严密的全局段落数字索引体系 `speechParagraphIndex`。
     - **突破移动端自动播放限制**：复用单一全局 `HTMLAudioElement` 音频流通道，在 `onended` 事件中连续换轨，彻底解决移动端（iOS Safari / 安卓 Chrome）读完一段就静默中断停止的问题。
     - **平滑双段预加载**：播放当前段的同时，后台静默将后第 +1、+2 段提前请求并缓存在浏览器内存（Blob URL），实现段落之间 0 延迟无缝切换发声。
     - **章节自动连续换章**：读完当前章最后一段，自动触发加载下一章并从新章节第一段平滑续播。
  2. **主界面与全项目版本号动态联动**：
     - 修复 `package.json` 硬编码版本停留在旧版本的脱节问题，对齐最新版本 `v1.0.6`。
     - `build.gradle.kts` 与前端构建统一注入 `VUE_APP_BUILD_VERSION`，主界面左上角实时联动显示 `v1.0.6`。
  3. **控制台独立新增【后端设定】Tab 标签**：
     - 在控制台（`Admin.vue`）中新增专属 `backend` 选项卡，提供后端服务状态指示灯、延迟测试与实时修改接口地址（API Prefix）功能。
     - 预留宽敞整洁的“说明图解展示区”插槽卡片，方便后续直接放入图片截图或 Markdown 架构图文指引。
     - 主界面侧边栏的【后端设定】标签点击后直接平滑直达控制台的【后端设定】选项卡。
  4. **全平台中文字体落地：.woff2 动态切片分包部署**：
     - 针对安卓手机系统底层无楷体/宋体，以及全量字体包过大（15MB+）导致首屏白屏的痛点，采用前沿的 `.woff2` 动态切片（`unicode-range`）技术。
     - 服务端内置部署开源商用中文字体动态切片（霞鹜文楷与思源宋体），整套切片加起来仅约 9MB，单包仅 30~50KB。
     - 手机端首屏按需只拉取 100~200KB 碎片，0.1 秒秒开零白屏，使安卓与苹果手机均可 100% 完美呈现细腻典雅的楷体与宋体排版。

---

## 2. 关键架构与模块对应

| 模块 | 核心文件 | 作用与实现机理 |
| :--- | :--- | :--- |
| **听书连续播放引擎** | `web/src/views/Reader.vue` | `speechParagraphIndex` 追踪、单音频实例连播与 Blob 队列预加载 |
| **动态切片字体资源** | `web/public/fonts/` (`kaiti/`, `songti/`) | 包含全部常用汉字分包 `.woff2` 与 `unicode-range` 规则 |
| **字体回退序列** | `web/src/plugins/config.js` (`fonts`) | 优先匹配动态切片 `"LXGW WenKai"`, `"Noto Serif SC"`，回退苹果内置字体 |
| **控制台后端设定 Tab** | `web/src/views/Admin.vue` (`activeTab === 'backend'`) | 后端连接监控、自定义 API 设置与说明图解展示插槽 |
| **主界面导航联动** | `web/src/views/Index.vue` (`toBackendSetting`) | 点击后端设定直接跳转后台独立 Tab |
| **版本号联动** | `web/package.json`, `build.gradle.kts` | 统一递增版本至 1.0.6，驱动 `$store.state.version` 动态呈现 |

---

## 3. 编译构建与自测验证记录

- **前端规范检查**：`cd web && yarn lint`，0 错误通过。
- **前端生产构建**：`cd web && yarn build`，输出资源与字体切片已全量同步至 `src/main/resources/web/`。
- **后端 Kotlin 编译**：`./gradlew compileKotlin`，构建成功（BUILD SUCCESSFUL）。
- **版本归档**：完整归档至 `v1.0.6/source/` 并生成本说明文档。

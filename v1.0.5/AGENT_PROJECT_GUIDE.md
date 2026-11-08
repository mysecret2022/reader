# Reader v1.0.5 智能体项目架构与维护说明 (AGENT_PROJECT_GUIDE)

> 本文档面向后续智能体与维护者，详细记录 v1.0.5 版本的业务逻辑、架构设计、Bug 根因排查与后续路线图。

---

## 1. 版本概览 (Version v1.0.5)
- **版本号**：v1.0.5 (递增规范：未来版本按 v1.0.6, v1.0.7 递增)
- **Git 目标分支**：`dev` (主开发演进分支)，验证通过后同步 fast-forward 合并至 `master`
- **主要更新点**：
  1. **书架分组前台展示逻辑恢复**：
     - 回退 `Index.vue` 计算属性 `bookGroupDisplayList`，恢复“当前分组内必须有书才显示标签”的合理逻辑（即刚新建且未添加书籍的分组在前台主页标签栏中自动隐藏，避免空标签占用屏幕空间，放入书籍后即刻呈现）。
  2. **阅读页正文字体设置界面净化与集中自定义入口**：
     - 去除预设字体（系统、黑体、楷体、宋体、仿宋）右侧原有的云上传图标，还原为清爽的文字单选按钮。
     - 在右侧新增独立的「自定义」字体按钮并挂载唯一的云上传图标，普通用户可直接一键上传任意私有 `.ttf` 字体文件，并由系统统一管理。
  3. **彻底解决 iOS 系统下正文字体设置失效的根因问题**：
     - **根因分析**：iOS Safari 及 WebKit 内核出于隐私保护和防设备指纹识别（Fingerprinting Protection）机制，在 WebKit 内核中强制禁止了使用 `@font-face { src: local("...") }` 加载系统预置字体；原项目在 `config.js` 中将真实字体回退序列注释掉，仅引用了 `@font-face` 声明的虚拟别名，导致 iOS 判定字体全部缺失，直接兜底回退为苹方（PingFang SC），视觉上切换任何字体均无效果。
     - **解决方案**：在 `config.js` 的 `fontFamily` 声明中直接追加苹果系统内置字体族（楷体直连 `"Kaiti SC"`、宋体直连 `"Songti SC"`、黑体直连 `"PingFang SC"`/`"Heiti SC"` 等）；并在 `helper.js` 中引入现代浏览器及 iOS Safari 10+ 官方标准的 `FontFace` API 与 `format("truetype")` 支持，实现 iOS 完美生效。

---

## 2. 关键架构与模块对应

| 模块 | 前端对应文件 | 作用与实现机理 |
| :--- | :--- | :--- |
| **书架空分组过滤** | `web/src/views/Index.vue` (`bookGroupDisplayList`) | 依据 `getShowShelfBooks(v.groupId).length` 动态过滤无书分组 |
| **正文字体配置与 iOS 适配** | `web/src/plugins/config.js` (`fonts`) | 直连苹果内置字体族，绕过 WebKit 对 `local()` 的指纹拦截 |
| **阅读字体设置界面** | `web/src/components/ReadSettings.vue` | 去除预设字体云图标，提供独立且唯一的「自定义」字体上传与选择 |
| **自定义字体动态加载** | `web/src/plugins/helper.js` (`loadFont`) | 采用 `FontFace` API + 动态样式注入，兼容 iOS 与移动端 WebKit |
| **Vuex 字体状态计算** | `web/src/plugins/vuex.js` (`currentFontFamily`, `currentCustomFontFamily`) | 优雅适配索引 5 独立自定义字体通道 |

---

## 3. 编译构建与自测验证记录

- **前端代码规范检查**：`cd web && yarn lint`，0 错误通过。
- **前端生产构建**：`cd web && yarn build`，成功输出产物并同步部署至后端静态目录 `src/main/resources/web/`。
- **后端 Kotlin 编译**：`./gradlew compileKotlin` 编译成功（BUILD SUCCESSFUL）。
- **版本归档**：完整归档至 `v1.0.5/source/` 并生成本说明文档。

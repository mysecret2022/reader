# AGENT_PROJECT_GUIDE (v2.2.1)

面向后续接手智能体的架构、功能与维护指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.2.1`
- **定位**：修复安卓端长按选词立即弹出操作弹窗并清除选区的核心交互缺陷，以及阅读设置中「自动换源」与「广告过滤」提示语句 5 秒平滑自动淡出。
- **上一主版本**：`v2.2.0`

---

## 2. 核心架构与功能特性清单 (2.2.1 Features)

### 2.1 安卓端长按划词交互与手柄选区彻底修复 (`Reader.vue`)
- **交互痛点与根因剖析**：
  - 在 iOS Safari 中，长按文本时 `touchstart` 保持活跃，`touchcancel` 不会触发，系统能正确识别手指正处于按住状态，等用户手指抬起后才呼出弹窗；
  - 但在 Android Chromium / Android WebView 中，长按正文触发系统原生选词手势时，浏览器会向 DOM 派发 `touchcancel`；
  - 原前端代码在 `handleTouchCancel` 中直接将 `isTouchActive` 置为 `false`，并附带了 `setTimeout(80ms)` 触发选区检测；
  - 导致用户在安卓上长按（按住）不到 0.1 秒，代码就误判手指已离开，立即弹出操作弹窗（复制/过滤广告/添加书签），并调用了 `sel.removeAllRanges()` 强制清空选区；
  - 用户手指还贴在屏幕上，刚出来的选区和原生水滴调整手柄就被直接销毁，根本无法正常选中一段文字。
- **解决方案与实施**：
  1. **重构 Android 触摸生命周期追踪**：
     - 在全局 `window` 监听 `pointerdown`、`pointerup`、`touchend`，增加 `isFingerDown` 实体触控追踪；
     - 在 `handleTouchCancel` 中**彻底移除 80ms 提前触发选区的定时器**，明确标记用户进入选词状态（`isTextSelecting = true`），不再误置触摸结束；
  2. **Android 原生选区与手柄完整保留**：
     - 在呼出操作菜单 `showTextOperate` 时，区分系统平台：仅对 iOS Safari 执行 `removeAllRanges()`（阻断其强硬的原生菜单）；对安卓系统完整保留原生选区高亮与水滴拖拽把手（Android 通过 `contextmenu preventDefault` 已天然免疫原生菜单）；
  3. **手指抬起后再优雅弹窗**：
     - 确保在用户按住或拖动把手微调时绝不弹出弹窗打扰；
     - 只有当用户手指真正离开屏幕（`window.touchend / pointerup`）且选区稳定后，才在选区上方弹出快捷操作条。

### 2.2 阅读设置「自动换源」与「广告过滤」提示 5 秒自动隐退 (`ReadSettings.vue`)
- **需求与优化**：
  - 切换「自动换源」开关时，展开展示动态提示并附带平滑淡出动画（`<transition name="fade">`），展示 5 秒后自动消失；
  - 切换「广告过滤」开关时，同样展示对应开启/关闭清空提示，5 秒后自动隐退消失；
  - 在组件销毁 `beforeDestroy` 时妥善清理定时器防抖，完全对齐“边缘识别”与“简洁模式”的提示交互规范。

---

## 3. 历史版本特性追溯

### 3.1 v2.2.0 特性追溯
- **主页左侧栏搜索确定后自动向左滑出隐退** (`Index.vue`)。
- **搜索框集成显式「确定」操作按钮与夜间模式适配** (`Index.vue`)。

### 3.2 v2.1.9 特性追溯
- **移动端阅读提示 5 秒自动淡出** (`ReadSettings.vue`)。
- **阅读设置「自动换源」同排新增「广告过滤」总开关与误杀一键恢复** (`ReadSettings.vue` & `config.js` & `Reader.vue`)。
- **根除 iOS Safari 选区蓝色高亮框悬浮在弹窗之上的系统级 Bug** (`Reader.vue`)。
- **一键过滤广告弹窗单选样式升级（空心圆内加圆点）与生效范围收敛** (`Reader.vue`)。

---

## 4. 运行、构建与测试指引

1. **前端构建与同步**：
   ```bash
   cd web
   yarn lint
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

1. **安卓端长按正文直接弹出操作弹窗且无法正常选词的缺陷修复**：
   - 阻断 `touchcancel` 引发的误判提前弹窗，保留安卓原生选区与拖拽手柄，调整为抬手稳定后优雅呈现。
2. **阅读设置提示语占屏问题收敛**：
   - 自动换源与广告过滤提示语统一改为 5 秒后自动淡出消失。
3. **主页移动端左侧栏搜索后抽屉常驻遮挡搜索结果的交互缺陷修复**。
4. **iOS Safari 划词蓝框悬浮在弹窗上方的层级穿透问题修复**。

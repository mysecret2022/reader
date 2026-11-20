# AGENT_PROJECT_GUIDE (v2.2.2)

面向后续接手智能体的架构、功能与维护指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.2.2`
- **定位**：彻底修复安卓手机划词选中文字后“可以自由拖动但操作气泡不再弹出”的 `touchcancel` 死锁缺陷，完善安卓拖动手柄时气泡自动避让与停手自动重现体验，并补全 `<keep-alive>` 生命周期全局监听一致性。
- **上一主版本**：`v2.2.1`

---

## 2. 核心架构与功能特性清单 (2.2.2 Features)

### 2.1 安卓长按选词 `touchcancel` 死锁根治与手柄微调体验完善 (`Reader.vue`)
- **问题根因剖析**：
  1. **主死锁 (`handleTouchCancel`)**：在安卓 Chrome / WebView 中，长按触发系统选词后浏览器派发 `touchcancel`（按 W3C Touch Events 规范，已取消的触摸序列此后绝不会再派发 `touchend` / `pointerup`，而后续拖动蓝色水滴手柄也仅产生 `selectionchange`，不再派发 DOM 触摸事件）。`v2.2.1` 中在 `handleTouchCancel` 未解除 `isTouchActive` / `isFingerDown`，导致这两个标记长期锁死为 `true`，使 `handleSelectionChange` 永久提前返回，操作气泡完全不弹出。
  2. **`<keep-alive>` 监听与状态不对称**：`mounted()` 绑定了全局 `touchend` / `pointerup`，`deactivated()` 卸载了它们，但 `activated()` 未重新绑定；且 `deactivated()` 与 `beforeDestroy()` 也未复位 `isTouchActive`、`isFingerDown`、`isTextSelecting`、`isDismissingMenu`。
  3. **其他交互细节**：`handleTouchCancel` 未清理残留的 `isDismissingMenu`；`isAndroid` 未覆盖 HarmonyOS/OpenHarmony；二次拖动手柄微调选区时未临时隐退气泡避免遮挡文字；`suppress-callout` 与 `handleContextMenu` 对 `"过滤弹窗"` 兼容不全；跨段落选区的 `commonAncestorContainer` 容器判定可能误拒。
- **修复与优化方案**：
  1. **解除 `touchcancel` 状态锁死并引入统一防抖调度 (`scheduleSelectionMenu`)**：
     - 在 `handleTouchCancel()` 中立即复位 `isTouchActive = false`、`isFingerDown = false`、`lastMoveX = 0`、`lastTouch = false`、清理滑动位移与残留 `isDismissingMenu`；
     - 检测到选区（或处于安卓设备）时，统一通过 `scheduleSelectionMenu(420)` 延迟触发弹窗；用户在选词后继续拖动水滴手柄调整范围时，每次 `selectionchange` 都会顺滑重置定时器，停手稳定 380ms 后再弹出气泡，完全不打断划词。
  2. **拖动手柄时气泡自动退让与重现，保留安卓原生选区**：
     - 仅在 `this.isIOS` 时提取 `.custom-selection-highlight` 并调用 `sel.removeAllRanges()`；安卓/鸿蒙与 PC 端保持 `selectionHighlightRects = []`，完整保留原生蓝色选区与水滴拖拽手柄；
     - 在安卓/PC 上，若气泡已显示时用户再次拖动手柄修改选区，`handleSelectionChange` 仅将 `textActionMenuVisible = false` 临时隐退（不调用 `closeTextActionMenu`、不清空选区、不触发 `dismissCooldown`），选区稳定后自动在最新选区位置重新弹出；若选区折叠/清空则顺滑关闭气泡；
     - `showTextOperate()` 中当靠顶向下展示气泡时，安卓端加大下移偏移量（`+34px`）避开下方水滴手柄，且容器判定 `isSelectionInReaderContent(range)` 同时检查 `commonAncestorContainer`、`startContainer` 与 `endContainer`。
  3. **`<keep-alive>` 与兼容细节补齐**：
     - `activated()` 补齐全局 `touchend` / `pointerup` 监听重绑；`deactivated()` 与 `beforeDestroy()` 同步复位触控与菜单关闭状态标记；
     - `.text-action-popup` 增加 `@touchstart.stop` 防止触摸冒泡误触发页面点击；`suppress-callout` 与 `handleContextMenu` 同时兼容 `"操作弹窗"` 与 `"过滤弹窗"`；`isAndroid` 兼容 `Android|HarmonyOS|OpenHarmony`。

---

## 3. 历史版本特性追溯

### 3.1 v2.2.1 特性追溯
- 安卓长按划词原生水滴手柄保留，以及阅读设置「自动换源」「广告过滤」提示 5 秒自动淡出 (`Reader.vue` & `ReadSettings.vue`)。

### 3.2 v2.2.0 特性追溯
- 主页左侧栏搜索确定后自动向左滑出隐退，搜索框集成显式「确定」按钮与夜间模式适配 (`Index.vue`)。

### 3.3 v2.1.9 特性追溯
- iOS Safari 蓝框浮层根除、一键过滤弹窗空心圆点单选样式与当前书籍范围收敛 (`Reader.vue` & `ReadSettings.vue` & `config.js`)。

---

## 4. 运行、构建与测试指引

1. **前端代码检查**：
   ```bash
   cd web
   yarn lint
   ```
2. **前端生产打包与静态资源同步**：
   ```bash
   cd web
   yarn sync
   ```
3. **后端单元与集成测试**：
   ```bash
   ./gradlew test
   ```
4. **本地服务启动**：
   ```bash
   ./build.sh serve
   ```

---

## 5. 已修复的 Bug 与技术改进 (Fixed Bugs & Technical Enhancements)

1. **安卓/鸿蒙端长按选词后“能自由拖动选择但操作气泡不再弹出”的 `touchcancel` 死锁修复**：
   - 在 `handleTouchCancel` 中正确解除触控锁定，由 `selectionchange` 统一防抖触发气泡，彻底兼顾自由划词拖动与稳定后弹出操作气泡。
2. **安卓二次拖动手柄微调选区体验优化**：
   - 拖动调整选区期间气泡自动临时隐藏让位（不销毁原生选区与水滴手柄），停手后在新位置重新显现，靠顶向下弹出时自动避开底部水滴手柄。
3. **`<keep-alive>` 页面切换后全局触摸监听与状态一致性修复**：
   - 在 `activated()` 补齐监听器恢复，在 `deactivated()` / `beforeDestroy()` 统一清理状态标记。

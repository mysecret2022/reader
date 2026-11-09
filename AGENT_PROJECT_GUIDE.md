# AGENT_PROJECT_GUIDE (v2.0.8)

面向后续接手智能体的项目自说明与迭代指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.0.8`
- **定位**：移动端选词气泡弹窗瞬间消失（毫秒级误关）Bug 根因彻底修复与生命周期状态隔离。
- **上一版本**：`v2.0.7`

---

## 2. 核心架构与 Bug 修复记录 (Architectural Hardening & Fixes)

### 2.1 气泡弹窗瞬间消失根因与彻底修复 (`Reader.vue`)
- **根因分析**：
  经双重执行链路排查，该现象由两个互锁的事件联动误杀导致：
  1. **自解选区事件反向误杀**：
     为屏蔽 iOS/Android 原生菜单，`showTextOperate` 在绘制自研高亮层后执行了 `sel.removeAllRanges()`。但浏览器底层在清空选区瞬间会向文档派发一次 `selectionchange` 事件；原先的 `handleSelectionChange` 中包含了一段代码：
     ```javascript
     if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
       if (this.textActionMenuVisible && !this.isTouchActive) {
         this.textActionMenuVisible = false; // 误杀罪魁祸首！
       }
       return;
     }
     ```
     由于 `sel.removeAllRanges()` 刚刚把 `rangeCount` 变为了 0，导致弹窗刚被设为 `true` 的微秒级瞬间，立即被紧随其后的 `selectionchange` 重置为了 `false`。
  2. **移动端手势合成事件误触全局关闭**：
     在 `watch: textActionMenuVisible` 中，原逻辑通过 `$nextTick` 立即挂载了全局 `pointerdown` 关闭监听。而在移动设备上，用户划词抬手（`touchend`）后，WebKit 内核会在数十至上百毫秒后向 `document` 派发该触控的合成微任务事件。该事件被刚刚挂载的 `closeHandler` 捕获，判定触控点不在气泡内，再次触发了关闭。

- **技术解决方案**：
  1. **解除选区对气泡状态的绑定**：
     在 `handleSelectionChange` 中彻底移除了“选区为空则关闭弹窗”的判断。由于项目已采用自研高亮图层（`selectionHighlightRects`），原生 DOM 选区的清空属于符合预期的屏蔽策略，弹窗的生命周期只由用户明确的退出动作控制。
  2. **全局关闭监听延迟避让**：
     在 `watch: textActionMenuVisible` 中，将全局点击关闭监听从微任务 `$nextTick` 改为安全延迟 220 毫秒后挂载。这一机制完美跨越了手机浏览器在抬手后的合成事件时间窗口，确保气泡稳固浮现，仅在用户后续主动点击空白处或操作按钮时才正常关闭。

---

## 3. 构建与运行指引 (Build & Test Guide)

### 3.1 前端构建
```bash
cd web
npm run build           # 生产环境打包（输出至 web/dist）
```

### 3.2 静态资源同步与后端编译
```bash
# 静态资源同步至 Spring Boot 资源目录
rm -rf src/main/resources/web/*
cp -rf web/dist/* src/main/resources/web/

# Kotlin 后端编译与生成可执行 JAR
./gradlew compileKotlin
./gradlew bootJar    # 输出产物：build/libs/reader-2.0.1.jar
```

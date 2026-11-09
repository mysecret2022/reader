# AGENT_PROJECT_GUIDE (v2.0.4)

面向后续接手智能体的项目自说明与迭代指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.0.4`
- **定位**：iOS 后台顶部状态栏避让修复、移动端（iOS / Android）阅读文本长按气泡菜单优化与原生浏览器菜单屏蔽。
- **上一版本**：`v2.0.3`

---

## 2. 核心架构与 Bug 修复记录 (Architectural Hardening & Fixes)

### 2.1 iOS 后台状态栏重叠与失效修复 (`Admin.vue`)
- **根因分析**：
  在 iOS 浏览器及 PWA 全屏模式下，系统状态栏（顶部时间、网络信号、电量图标等）位于页面最上层。原 `.admin-header` 采用固定高度 `60px`（移动端 `52px`），未计算 iOS 安全视口顶部内边距（`env(safe-area-inset-top)`），导致“返回书架”按钮、管理徽章与系统状态栏完全重叠，按钮被手势截断或无法被手指精准触控。
- **解决方案**：
  1. 引入 Webkit 标准安全区环境变量计算：
     - 高度调整为 `height: calc(60px + env(safe-area-inset-top, 0px))`（移动端 `@media (max-width: 768px)` 调整为 `calc(52px + env(safe-area-inset-top, 0px))`）。
     - 增加顶部内边距 `padding-top: env(safe-area-inset-top, 0px)` 及左右安全避让 `padding-left: calc(24px + env(safe-area-inset-left, 0px))`、`padding-right: calc(24px + env(safe-area-inset-right, 0px))`，并配置 `box-sizing: border-box`。
  2. 元素整体自然下移避开齐刘海与灵动岛，彻底杜绝按钮与状态栏的视觉及点击冲突。

### 2.2 移动端（iOS / Android）文本选择气泡优化与原生菜单屏蔽 (`Reader.vue`)
- **根因分析**：
  在手机端（iOS Safari / Android Chrome / WebKit）开启“选择文字 - 操作弹窗”时，用户长按或滑动选中文本后，系统会同时唤起原生浏览器的上下文菜单（如 iOS 的“拷贝 / 查询 / 翻译 / 共享 / 搜索”，Android Chrome 的浮动操作栏），与阅读器弹窗重叠打架，视觉繁杂且易发生误触。
- **解决方案**：
  1. **原生菜单全面屏蔽**：
     - **iOS 端**：当配置开启“操作弹窗”时，动态为阅读容器注入 `suppress-callout` 类，并在 CSS 中应用 `-webkit-touch-callout: none;`，彻底禁用 iOS WebKit 的默认气泡弹窗，同时保留文本选区判定。
     - **Android 端**：在阅读主体上拦截并阻止默认 `@contextmenu` 事件（`e.preventDefault()`），消除安卓长按呼出的原生上下文菜单。
     - **选区即时清除**：在获取选中文本及其屏幕坐标后，立即执行 `window.getSelection().removeAllRanges()`，清除系统高亮范围，迫使 Android/iOS 浏览器在最底层关闭其原生浮动工具栏与拉手。
  2. **轻量自研悬浮气泡胶囊 (`.text-action-popup`)**：
     - 包含三个高频核心操作：**【复制】**、**【添加书签】**、**【添加过滤规则】**。
     - **复制功能**：优先调用现代化 `navigator.clipboard.writeText` 异步接口，向下无缝兼容隐藏 `textarea` + `document.execCommand('copy')` 方案，复制成功后给出轻提示。
     - **智能避让与定位**：基于文本选区的 `getBoundingClientRect()` 计算绝对视口坐标，居中悬浮在选中文本上方 10px 处；若选区过分靠顶（`< 55px`），则平滑翻转至选区正下方 10px 处显示；且内置左右安全边界限制，防止在大屏或边缘被截断。
     - **全局点击关闭监听器**：弹窗弹出后，通过 `pointerdown` 事件监听，只要手指点击弹窗外任意位置即刻关闭气泡，体验与原生系统一致且极其流畅纯净。

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

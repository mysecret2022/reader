# AGENT_PROJECT_GUIDE (v2.1.9)

面向后续接手智能体的架构、功能与维护指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.1.9`
- **定位**：阅读器移动端体验细节打磨，包含设置提示 5 秒自动隐退、阅读设置新增广告过滤开关与一键清空还原、彻底根除 iOS Safari 蓝框悬浮在弹窗之上、以及广告过滤弹窗单选样式升级（空心圆内加圆点）与生效范围默认收敛为当前书籍。
- **上一主版本**：`v2.1.8`

---

## 2. 核心架构与功能特性清单 (2.1.9 Features)

### 2.1 移动端设置提示 5 秒自动淡出 (`ReadSettings.vue`)
- **需求与优化**：
  - 在移动端阅读设置中的“正文识别”模块，点击“边缘识别”和“简洁模式”时，下方会展开提示语句（`small-tip`）；
  - 过去提示语常驻占据屏幕空间；现改为触发后展示 5 秒，随后通过平滑淡出动画（`<transition name="fade">`）自动隐退消失；若 5 秒内用户反复切换，内置防抖重置定时器，保证交互流畅自然。

### 2.2 阅读设置「自动换源」同排新增「广告过滤」总开关与误杀一键恢复 (`ReadSettings.vue` & `config.js` & `Reader.vue`)
- **需求与优化**：
  - 在阅读设置的“自动换源”一排，并排新增“广告过滤”开关按钮；
  - 针对用户在划词去广告时误将小说正文当成广告过滤的痛点场景：
    - 当用户关闭「广告过滤」时，系统自动检索并清空当前书籍在后台已添加的所有广告过滤规则（匹配分组 `广告过滤` 或前缀 `[广告过滤]`），并调用 `/deleteReplaceRules` 彻底删除；
    - 联动事件总线 `refreshReaderContentAfterAdClear`，无感刷新重载当前章节原始内容并重新排版，被误杀的正文文字瞬间还原；
    - 在正文内容过滤器 `filterContent` 中同步增加 `config.adFilterEnabled` 防护，若用户关闭广告过滤，广告规则被完全跳过。

### 2.3 根除 iOS Safari 选区蓝色高亮框悬浮在弹窗之上的系统级 Bug (`Reader.vue`)
- **根因分析**：
  - iOS Safari 在文本长按划词时，WebKit 内核会生成系统级原生选区及手柄；
  - 同时项目内置了用于屏蔽原生系统菜单的自定义选区高亮层 `.custom-selection-highlight`，其 `z-index` 被设为 `9998`；而 Element UI 的对话框 `<el-dialog>` 的 `z-index` 为 `2000-2100`；
  - 在弹出“一键过滤广告”弹窗时，未彻底清空 WebKit 原生选区，且自定义高亮层层级过高，导致蓝色的选中框浮在弹窗遮罩与弹窗上方。
- **修复方案**：
  - 将 `.custom-selection-highlight` 的 `z-index` 调整为 `100`，严格置于弹窗和顶部栏层级之下；
  - 在选区图层外包裹 `<template v-if="!showAdFilterDialog">`，只要一键过滤弹窗呼出，立即彻底销毁高亮层 DOM；
  - 点击“过滤广告”进入弹窗时，新增 `clearBrowserSelection()` 方法，通过 `window.getSelection().removeAllRanges()`、`document.selection?.empty()` 与 `activeElement.blur()` 彻底清除浏览器原生选区，在 iOS Safari 与各端浏览器中均实现零杂影、零蓝框浮层。

### 2.4 一键过滤广告弹窗样式升级与范围收敛 (`Reader.vue`)
- **需求与优化**：
  - **默认精准过滤**：弹窗打开时过滤方式默认选中为“精准过滤”（`exact`），用户亦可手动点选切换为“整行过滤”（`line`）或“智能链接过滤”（`domain`）；
  - **空心圆中间加圆点视觉规范**：
    - 重构原有 Element 默认单选框，定制纯 CSS 矢量圆点组件 `.ad-radio-custom-item`；
    - 未选中时呈现精简高雅的空心圆环（`border: 2px solid; background: transparent;`）；
    - 选中时空心圆环高亮，且在空心圆中间弹出一个实心点（`transform: scale(1);`），完美符合“选中的规则在空心圆中加一个点”的视觉要求；
  - **生效范围默认收敛为当前书籍**：
    - 从弹窗模板中彻底删除“全部书籍生效”相关选项与说明；
    - 默认且强制仅在当前书籍（`bookName;bookUrl`）生效，防止全局误伤其它小说。

---

## 3. 历史版本特性追溯

### 3.1 v2.1.8 特性追溯
- **书源沙箱入库 `$notify` 异常彻底修复** (`element.js` & `BookSourceManage.vue`)。
- **远程书源 302 重定向至 CDN 与大文件拉取加固** (`BookSourceController.kt`)。

### 3.2 v2.1.7 特性追溯
- **正文划词气泡一键过滤广告** (`Reader.vue` & `ReplaceRule.vue` & `vuex.js`)。

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

1. **iOS Safari 划词蓝框悬浮在弹窗上方的层级穿透问题修复**：
   - 彻底清除浏览器原生 Range 选区，降低自定义高亮层 z-index 至 100，并在弹窗展示时销毁高亮 DOM。
2. **移动端阅读设置“边缘识别/简洁模式”提示语长期占据屏幕空间的体验问题**：
   - 增加 5 秒自动平滑淡出与定时器防抖。
3. **误将小说正文划词过滤为广告后的恢复困难痛点**：
   - 在自动换源排新增广告过滤开关，关闭后一键清空当前书籍已添加的广告规则，并秒级刷新复原正文。
4. **一键过滤广告弹窗单选视觉样式与生效范围收敛**：
   - 默认精准过滤，单选按钮采用空心圆加内点设计，删除全局生效选项，固定保护为仅当前书籍生效。
5. **书源导入预检暂存沙箱 `$notify is not a function` 修复**。
6. **远程书源 302 重定向至 CDN 与大文件拉取失败修复**。

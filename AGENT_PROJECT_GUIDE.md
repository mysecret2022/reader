# AGENT_PROJECT_GUIDE (v2.0.5)

面向后续接手智能体的项目自说明与迭代指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.0.5`
- **定位**：阅读设置面板 3 标签等宽标准化重构、配置方案精简、默认正文字体升级为“文楷”、以及仿真翻页动画架构评测。
- **上一版本**：`v2.0.4`

---

## 2. 核心架构与功能迭代记录 (Architectural Hardening & Improvements)

### 2.1 全设置项 3 标签等宽网格排版规范 (`ReadSettings.vue`)
- **设计规范**：
  以“特殊模式”（正常、简洁、边缘识别）为视觉排版基准，彻底消除以往标签长短不一、换行参差不齐的凌乱感。
- **技术实现**：
  - 将 `.selection-zone` 升级为现代弹性流式布局：
    ```css
    display: inline-flex;
    flex-wrap: wrap;
    gap: 10px;
    width: calc(100% - 72px);
    ```
  - 将所有 `.span-item` 统一限定为单排 3 等分宽度：
    ```css
    width: calc((100% - 20px) / 3);
    height: 34px;
    box-sizing: border-box;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    ```
  - 覆盖范围包括：特殊模式、配置方案、正文字体、简繁转换、页面模式、翻页方式、自动翻页、点击翻页、选择文字等所有选项区域。

### 2.2 特殊模式与条件说明优化 (`ReadSettings.vue`)
- **调整点**：
  1. 标签固定为 `正常`、`简洁`、`边缘识别` 3 个。
  2. 提示联动：原简洁模式说明常驻的问题已修复，现仅当选中“简洁”（`config.pageType === 'Kindle'`）时才动态浮现说明，逻辑与“边缘识别”保持一致。

### 2.3 配置方案与精简方案类型 (`ReadSettings.vue` & `config.js`)
- **调整点**：
  1. 彻底删除“方案类型”（白天默认 / 黑夜默认）冗余配置行。
  2. 精简“配置方案”为纯粹的 3 个选项：`内置白天`、`内置黑夜`、`自动切换`。
  3. 新增 `selectBuiltinScheme` 快捷调度逻辑，点击后平滑联动 Vuex 状态。

### 2.4 正文字体升级为“文楷”并设为默认 (`config.js` & `ReadSettings.vue`)
- **背景与选型**：
  原“楷体”实际上集成了高质量的“霞鹜文楷”（LXGW WenKai，包含 119 个极速切片字体包），在中文网文及电子出版物排版中享有极高美誉度（类似微信读书排版风格）。
- **落地改造**：
  1. 设置面板中正式命名为“文楷”。
  2. 将系统 `defaultDayConfig.font` 与 `defaultNightConfig.font` 默认索引由 `0`（系统黑体）提升为 `1`（文楷），开箱即享沉浸典雅的阅读字形。

### 2.5 仿真翻页（3D 纸张翻折）动画资源评测 (Simulation Page Turn Evaluation)
- **评测结论**：**服务器负载消耗为 0%**。
- **架构分析**：
  1. 阅读器采用纯前端渲染与富客户端架构。服务端（Spring Boot / Vert.x）职责仅为章节文本存储与传输，绝不参与页面视觉帧渲染。
  2. 仿真翻页属于纯客户端（浏览器端）图形变换，依靠 WebGL / HTML5 Canvas / CSS 3D Transform 实时计算贝塞尔曲面与阴影。整个翻页过程完全在用户手机或电脑的 GPU/CPU 本地运算，不产生任何服务端网络请求与资源占用。
  3. **客户端体验权衡建议**：
     - **上下滑动 / 左右滑动**：硬件加速平移，极致流畅省电（120Hz 满帧）。
     - **仿真翻页**：拟真纸书翻页，沉浸感极强，但对极低端设备 GPU 算力有轻微要求。
     - **最佳落地方案**：作为“翻页方式”的扩展模式加入，供用户在设置中自主自由选择。

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

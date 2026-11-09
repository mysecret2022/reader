# AGENT_PROJECT_GUIDE (v2.0.7)

面向后续接手智能体的项目自说明与迭代指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.0.7`
- **定位**：长按划选文字屏幕乱跳、划选抬手误触发翻页跳转前一页 Bug 彻底修复，以及无感复制焦点锁定优化。
- **上一版本**：`v2.0.6`

---

## 2. 核心架构与 Bug 修复记录 (Architectural Hardening & Fixes)

### 2.1 长按划选文字屏幕乱跳与跳转前一页根因与彻底修复 (`Reader.vue`)
- **根因分析**：
  1. **视口位移干扰选区**：
     在手机触摸滑动（`touchmove`）过程中，用户拖拽游标划选文字时，旧版事件监听未能在第一时间锁定正文视口。手指横向划选产生的微小或较大位移（`moveX`）直接被应用到 `contentStyle = { transform: translateX(...) }` 上，导致正在划选的正文在指尖下方左右位移。正文一动，浏览器底层的光标命中点随之改变，引发选区拉手和高亮在屏幕上剧烈跳动（“屏幕上面会乱跳”）。
  2. **划词位移被误判为翻页手势**：
     当用户完成划词抬起手指（`touchend`）时，由于从左向右划词产生了 `moveX > 30` 的累积正向位移，`handleTouchEnd` 将其误当成了“向右滑动屏幕翻页”的手势，从而强行触发了 `prevPage()`，导致刚选完字屏幕瞬间跳转到了前一页（“然后复制的内容会跳转到前一页”）。
  3. **触控中提前触发选区销毁**：
     旧逻辑中 `handleSelectionChange` 在 `isTouchActive === true`（手指仍在屏上）时仍启动了延时定时器，导致用户还没选完字，底层原生选区就提前被 `removeAllRanges()` 销毁，引发浏览器光标重置。

- **技术解决方案**：
  1. **划选状态严格锁定 (`isTextSelecting`)**：
     - 在 `handleTouchStart` 中初始化 `isTextSelecting = false`。
     - 在 `handleTouchMove` 顶部首先判断 `isTextSelecting` 或底层选区（`window.getSelection()`）：一旦检测到选词正在进行，立即将 `isTextSelecting` 标记为 `true` 并将位移重置为 `lastMoveX = 0`，**直接跳出事件，严禁改变 `contentStyle` 任何坐标**，正文稳如磐石，彻底消除选词晃动跳跃。
     - 在 `handleSelectionChange` 中，只要手指仍在屏幕上（`isTouchActive === true`），仅记录 `isTextSelecting = true` 并立即退出，**绝不提前清空选区，也绝不提前弹窗打断用户的连续拉手选词体验**。
  2. **抬手拦截翻页与坐标精确复位**：
     - 在 `handleTouchEnd` 中，首先检测 `hasSelectedText`（处于划选状态或存在有效选中文本）。
     - 一旦命中，立即清空位移 `this.lastMoveX = 0`，将微小位移瞬间复位到精确的 `translateX(${this.transformX}px)`，随后直接 `return`，**彻底掐死翻页逻辑（上一页/下一页）的触发路径**。
     - 此时手指已经离开屏幕，再平稳调用 `checkAndTriggerSelection()`，提取高亮坐标、呈现自研高亮图层、屏蔽原生系统菜单并弹出操作气泡，整套流程严密流畅。
  3. **复制焦点防跳与按钮事件隔离**：
     - 在 `fallbackCopy` 中为隐藏的临时输入框补全安全样式：`readonly`、`pointer-events: none`、`position: fixed; top: -9999px; left: -9999px; opacity: 0`，彻底防止输入框聚焦拉动页面滚动。
     - 为气泡弹窗内的【复制】、【添加书签】、【添加过滤规则】按钮全面绑定 `@click.stop.prevent` 与 `@touchend.stop.prevent`，杜绝任何穿透与冒泡。

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

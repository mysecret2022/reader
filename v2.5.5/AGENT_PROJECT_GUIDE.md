# Reader v2.5.5 Agent Project Guide

本项目是针对开源开源阅读 Web 版本的持续演进与增强版本。本指南专为后续接手的 AI 智能体与工程师编写，便于快速定位架构职责、已修复缺陷与测试部署指引。

---

## 1. 架构与模块职责

- **核心运行框架**：Spring Boot 2.1.6 + Vert.x 3.7.1 协程异步驱动。
- **书源解析引擎**：`io.legado.app.model.analyzeRule.AnalyzeRule` 与 `WebBook`，内置 Rhino JavaScript 执行环境与增强扩展（`JsExtensions.kt`）。
- **前端交互组件**：Vue 2 + Element-UI 单页应用 (`web/src/views/` 与 `web/src/components/`)。
- **持久化与缓存**：基于文件目录存储（`storage/data/<user>/`）与内存缓存（`bookInfoCache`、`chapterCache`）。

---

## 2. v2.5.5 关键缺陷修复与功能增强

### 1) 修复换源后再次点击其他书源报错“书籍信息错误”
- **缺陷现象**：在书源弹窗中选择了“顶点小说”后，再点击列表中其他书源，页面直接弹出“书籍信息错误”，无法继续换源。
- **根本原因**：
  1. **前端**：`BookSource.vue` 中 `changeBookSource` 原逻辑在保存换源结果时仅赋值了 `book.bookUrl = searchBook.bookUrl`，未补齐 `book.origin` 与 `book.originName`；导致下次点击书源时，`isInShelf` 校验 `!book.origin` 为空直接抛错。
  2. **后端**：`BookController.kt` 中的 `setBookSource` 原仅通过 `getShelfBookByURL(bookUrl)` 查找书本。当换源后 URL 发生变更或通过书名搜索进入时，URL 无法匹配书架记录，返回“书籍信息错误”。
- **修复方案**：
  1. `BookSource.vue`：在换源成功后同步更新 Vuex `readingBook` 中的 `origin`、`originName`、`tocUrl`、`type` 与 `res.data.data`。
  2. `App.vue`：`isInShelf` 函数增强，同时支持按 URL 或 `(name, author)` 组合在书架中匹配，若 `book.origin` 为空自动从已匹配的书架记录或当前阅读对象中回填。
  3. `BookController.kt`：`setBookSource` 中在根据 URL 找不到书籍时，退行通过 `getShelfBookByNameAndAuthor` 查找，最后通过 `bookInfoCache` 查找，彻底避免返回“书籍信息错误”。

### 2) 修复空目录导致的 IndexOutOfBoundsException
- **缺陷现象**：部分空目录书源加载时崩溃，抛出 `java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0`。
- **原因与修复**：`BookChapterList.kt` 中日志记录前直接访问 `chapterList[0]`。现增加 `if (chapterList.isNotEmpty())` 安全保护。

### 3) 移除不支持 WebView 的报错阻断
- **缺陷现象**：标注了 `{'webView': true}` 的书源（如《独步小说》等）弹窗报错 `java.lang.Exception: 不支持webview`。
- **原因与修复**：在 `AnalyzeUrl.kt` 中将抛出异常改为安全降级为普通 HTTP 请求并记录 WARN 日志，使书源能正常获取内容。

### 4) 增强 JS 加密扩展支持（顶点小说等复杂源）
- **原因与修复**：顶点小说等书源规则在正文解密时调用了 `java.createSymmetricCrypto` 与 `<+js>` 正则。
- **实现**：
  - 新增 `SymmetricCrypto.kt`，实现 AES/DES 对称解密。
  - 在 `JsExtensions.kt` 中注入 `createSymmetricCrypto`、`bytesToStr`、`strToBytes`。
  - 在 `AppPattern.kt` 中放宽 `JS_PATTERN` 匹配以容忍书源排版 typo。

### 5) 诊断与日志运维接口
- **接口**：`GET /reader3/getSystemLogs?lines=200&error=false`
  - 返回系统最近输出的格式化运行日志（支持 `lines` 行数限制及 `error=true` 错误筛选）。
- **标准输出控制**：`logback-spring.xml` 在生产环境 `prod` 下启用了控制台 `STDOUT` 输出，支持 `docker logs -f` 实时观察。

### 6) 界面净化
- 在 `web/src/views/Index.vue` 中移除了底部的微信/TG 推广链接、GitHub 图标以及白天黑夜主题切换图标。

### 7) 书源分组与换源筛选增加 @javascript 与 支持webview 选项
- **背景与需求**：
  - 用户书源包含普通格式小说、包含脚本解密的 `@javascript` 书源（如顶点小说等），以及标记了 `{'webView': true}` 的 WebView 书源（如独步小说、总裁小说等）。
  - 用户需要能快速精准筛选/搜索 `@javascript` 格式或 WebView 格式的书源，避免全部混杂在一起。
- **实现方案**：
  1. **前端 Vuex 与界面**：在 `bookSourceGroupList`、`BookSource.vue` 及 `Index.vue` 中自动识别与统计 `@javascript (脚本解密)`、`支持webview` 和 `普通格式 (标准小说)`，动态加入选择框与书源管理分类。
  2. **换源联动**：在“换源/来源”弹窗中，切换对应分组时实时过滤并可自动异步检索对应类型的书源。
  3. **后端过滤**：在 `loadBookSourceStringList` 中对 `@javascript`、`支持webview`、`普通格式` 做专门规则分类匹配，支持在多源搜索与换源检索中精准过滤。

---

## 3. 本地部署与测试指引

### 1) 本机测试端口与容器
- **本地宿主机运行**：
  ```bash
  docker run -d --name reader-local --restart always -p 8090:8080 \
    -v $(pwd)/storage:/storage \
    -v $(pwd)/logs:/logs \
    -e TZ=Asia/Shanghai -e SPRING_PROFILES_ACTIVE=prod \
    reader:local
  ```
- **访问入口**：`http://localhost:8090/` 或 `http://192.168.0.91:8090/`
- **日志诊断入口**：`http://localhost:8090/reader3/getSystemLogs`

---

## 4. 远程交付镜像
- GitHub 远程仓库：`https://github.com/mysecret2022/reader.git`
- 自动构建交付镜像：`ghcr.io/mysecret2022/reader:latest`

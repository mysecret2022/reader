# AGENT_PROJECT_GUIDE (v2.0.1)

面向后续接手智能体的项目自说明与迭代指引文档。

---

## 1. 版本概述 (Version Summary)
- **版本号**：`v2.0.1`
- **定位**：书架并发竞态数据丢失根治、后台移动端排版精细化适配与胶囊滑动体验升级。
- **上一版本**：`v2.0.0`

---

## 2. 核心架构与 Bug 修复记录 (Architectural Hardening & Fixes)

### 2.1 书架图书数量跳动与丢失 Bug 根治 (`BookController.kt`)
- **根因分析**：
  原 `getBookShelfBooks()` 方法在获取书架列表时，直接将并发协程（`limitConcurrent(16)`）生成的结果通过未经同步保护的 `ArrayList.add(book)` 写入。多协程在没有锁保护的情况下并发写入 Java `ArrayList`，存在典型的线程安全竞态，导致底层数组扩容或赋值覆盖，造成书籍随机丢失；且执行 `add` 的时间先后不固定，导致每次刷新书架的书籍顺序被打乱。
- **解决方案**：
  1. **非刷新模式（`refresh == false`）**：书架加载日常无需重新拉取网络最新章节，直接对持久化的 JSON 数组按原始顺序遍历顺序填充 `ArrayList`，零多线程竞态，100% 保持原有书架排列顺序。
  2. **刷新模式（`refresh == true`）**：预先分配固定大小的数组槽位 `val bookArr = arrayOfNulls<Book>(shelfSize)`，在并发协程内部依据索引下标 `bookArr[it] = book` 进行写入。每个协程拥有独占的数组槽位，互不冲突，彻底消除了写竞争，最后过滤返回非空结果。

### 2.2 书源设置工具栏优化与 2 排等比栅格 (`Admin.vue`)
- **移除冗余入口**：后台书源设置中移除了非必要的“探索书源”按钮与冗余弹窗，使工具栏保持精简聚焦。
- **2 排自适应布局**：保留的 6 个核心操作按钮（导入书源、远程书源、失效书源、调试书源、全部启用、全部禁用）在移动端采用 `grid-template-columns: repeat(3, 1fr)` 响应式网格布局，整齐划分为 2 排，并随手机屏幕宽度等比缩放放大，避免按钮挤压换行或错位。

### 2.3 书架二级标签横向滑动胶囊化 (`Admin.vue`)
- **二级标签（书籍管理、分组管理、导入书籍、浏览书仓、刷新缓存）适配**：
  在手机端（`max-width: 768px`）对 Element UI 的 `el-tabs--border-card` 进行了穿透样式重构，将生硬的卡片边框转为透明背景与横向平滑滚动的圆角胶囊药丸（Pill Badges），支持触摸横滑无缝切换，完全贴合移动端交互习惯。

### 2.4 用户空间配置卡片移动端屏幕溢出修复 (`Admin.vue`)
- **根因分析**：
  “当前用户空间”与“终端配置同步与备份”中的卡片容器原先使用了固定行内样式 `display: flex; gap: 20px;`，未设置换行规则，导致手机小屏下左右两个卡片各占 50% 宽度并被挤出屏幕边缘，文字被截断。
- **解决方案**：
  重构为响应式布局类 `.user-space-info-body`、`.user-space-row`、`.user-config-cards` 与 `.user-config-card`。在移动屏幕下自动转换为单列垂直流式排版（`flex-direction: column`），选择框与操作按钮自适应 100% 宽度，彻底解决文字排版超出屏幕边界的问题。

---

## 3. 构建与运行指引 (Build & Test Guide)

### 3.1 前端构建
```bash
cd web
yarn lint --no-fix   # 代码规范校验（必须 0 error）
yarn build           # 生产环境打包（输出至 web/dist）
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

### 3.3 运行可执行文件
```bash
java -jar reader-2.0.1.jar --reader.server.port=8080
```

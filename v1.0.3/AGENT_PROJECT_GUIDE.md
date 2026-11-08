# Reader v1.0.3 智能体项目架构与维护说明 (AGENT_PROJECT_GUIDE)

> 本文档面向后续智能体与维护者，详细记录 v1.0.3 版本的业务逻辑、架构设计、Bug 根因排查与后续路线图。

---

## 1. 版本概览 (Version v1.0.3)
- **版本号**：v1.0.3 (递增规范：未来版本按 v1.0.4, v1.0.5 递增)
- **Git 目标分支**：`dev` (主开发演进分支)，稳定版本发布后推至 `master`
- **主要更新点**：
  1. **彻底根治换源读条卡顿与缓慢问题**：
     - 后端 `searchBookWithSource` 增加 `withTimeoutOrNull(4000L)`，彻底杜绝单书源网络假死挂起整个批量检索流程。
     - 后端 `setBookSource` 增加目录拉取超时保护 `withTimeoutOrNull(6000L)`，防止换源卡死在外部源服务器。
     - 前端 `Reader.vue` 换源后直接使用缓存已更新目录（`loadCatalog(false, true)`），避免换源后再次重复向外部服务器请求全量目录导致双重网络等待。
     - 前端 `BookSource.vue` 移除常驻卡死无超时的 `duration: 0` 提示，增加 10s 保护定时器，显示【当前】与【已使用】书源徽章。
  2. **普通用户子管理后台权限体系 (Ordinary User Sub-Management Portal)**：
     - 普通用户登录后可直接点击顶部 `管理 (用户名) ▾` 选项进入后台 `/admin`。
     - 普通用户在后台仅可见、仅可访问两大核心模块：
       - **书源管理**：书源列表与检索、启用/禁用、删除、失效书源检测与一键清理、导入书源（本地上传/URL/JSON文本）、**一键同步管理员默认书源**（一键拉取默认48个书源）。
       - **书架设置**：书籍管理（含在线智能修改封面、置顶、移除）、分组管理（新增、重命名、删除）、导入本地书籍（.txt, .epub, .cbz）、浏览共享书仓（查看 `/localStore` 并一键入架）、刷新缓存。
     - 普通用户严格禁止接触：用户权限治理、云同步与定时备份、诊断运行日志、底层本地书仓磁盘维护。
  3. **管理员默认书源全量共享机制**：
     - 当新用户或普通用户未配置个人书源时，后端 `getUserBookSourceJson` 与 `loadBookSourceStringList` 自动回退使用 `admin` 配置的高质量默认书源。
     - 新增 `/reader3/syncAdminBookSources` 接口，支持一键重置/同步管理员书源。
  4. **主页与书架顶栏交互重构**：
     - 右侧操作按钮统一为：`[书海] [刷新] [编辑] [管理 (用户名) ▾]`。
     - 去除原先与书架标题黏连的单独 `admin` 标签，将所有管理与账户操作统一收拢在下拉菜单中。
  5. **后台诊断运行日志 404 修复**：
     - 修复 `Admin.vue` 中 Axios 请求遗漏 `this.api` 前缀导致访问 `/getSystemLogs` 报 404 错误的问题。修复后实时显示 Vert.x 后台运行日志。
  6. **图书搜索与无感备用源查找**：
     - 搜索图书时不强制加载外部高分辨率图片封面，使用轻量图标占位，提高搜索流畅度。
     - 用户点击加入书架后，后台低并发（2线程）静默并发搜索最多10个备用源并自动终止。
  7. **书籍封面在线替换**：
     - 书架管理与书籍编辑均支持在线拉取各书源封面并实时替换，支持输入自定义封面 URL。
  8. **GitHub Actions 自动化集成**：
     - `.github/workflows/docker-publish.yml` 新增 `dev` 分支触发支持，直接推送即可在云端触发全自动多架构编译与镜像发布。

---

## 2. 关键架构与模块对应

| 模块 | 前端对应文件 | 后端对应接口 / 控制器 |
| :--- | :--- | :--- |
| **换源与书源选择** | `web/src/components/BookSource.vue` | `BookController.kt` (`setBookSource`, `getAvailableBookSource`, `searchBookSourceSSE`) |
| **阅读器与目录刷新** | `web/src/views/Reader.vue` | `BookController.kt` (`getChapterList`, `getBookContent`) |
| **书架与主页** | `web/src/views/Index.vue` | `BookController.kt` (`getBookshelf`, `saveBook`, `deleteBook`) |
| **管理后台 (总管与子后台)**| `web/src/views/Admin.vue` | `UserController.kt`, `BookSourceController.kt`, `CloudSyncController.kt` |
| **诊断日志** | `web/src/views/Admin.vue` (`activeTab === 'logs'`) | `BaseController.kt` (`/reader3/getSystemLogs`) |
| **书源同步** | `web/src/views/Admin.vue` (`syncAdminSources`) | `BookSourceController.kt` (`/reader3/syncAdminBookSources`) |

---

## 3. 编译构建与静态资源同步要点 (Crucial Build Rule)

> **极度重要**：
> Gradle 构建脚本 (`cli.gradle`) 打包进 JAR 的静态前端文件位于 `src/main/resources/web/`。
> 本地前端代码在 `web/` 下，若本地编译，必须执行：
> ```bash
> cd web && yarn build
> rm -rf ../src/main/resources/web/* && cp -r dist/* ../src/main/resources/web/
> cd .. && ./build.sh cli assemble
> ```
> 在 GitHub Actions 中，已配置 `yarn sync` 自动完成此复制流程。

---

## 4. 后续路线图 (Roadmap for v1.0.4+)
1. **书源健康度实时打分与排序**：按响应延迟和章节成功率对可用书源进行自动降序排列。
2. **多端阅读进度云端秒级冲突解决**：利用时间戳比对或 CRDT 解决手机端与 Web 端同时阅读同一本书籍时的进度覆盖问题。
3. **WebDAV 增量同步性能提升**：针对海量书籍缓存，仅同步元数据与进度，内容按需缓存。

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
  8. **控制台 404 故障根因与彻底修复**：
     - **根因**：`Admin.vue` 中定义了私有的 `api() { return this.$store.state.api || "/reader3"; }`，而 Vuex 中 `state.api` 默认为 `location.host + "/reader3"`（无 `//` 协议头）。在缺少协议头时，浏览器将 Axios 请求处理为当前页面的相对路径，拼接成 `http://192.168.0.30:8080/192.168.0.30:8080/reader3/...`，导致“系统运行异常记录 (`/getInvalidBookSources`)”、“添加用户 (`/addUser`)”、“诊断运行日志 (`/getSystemLogs`)”全部触发 404。
     - **修复**：将 `Admin.vue` 的 `api()` 计算属性统一调整为 `return this.$store.getters.api`，自动补充协议头 `//`，彻底恢复所有管理端接口正常调用。
  9. **强制登录与彻底关闭游客模式**：
     - 前端 `App.vue` 将 `canCancelLogin` 严格绑定为当前用户登录状态（`!!userInfo.username`），未登录状态下严禁关闭登录界面。
     - 彻底移除 `Login.vue` 中的“游客体验 / 直接进入书架”按钮与 `reader_guest_mode` 免登录分支，所有用户必须验证登录才可访问书架与阅读功能。
  10. **书籍封面拉取“图片一闪而过”根因与彻底修复**：
     - **根因**：原代码在 `onCoverImgError` 中定义了 `this.candidateCovers.splice(idx, 1)`，且前端直接通过 `<img :src="cover.url">` 请求第三方图床。当外部书源图床开启防盗链拦截（HTTP 403 Forbidden）时，图片触发 `@error` 立即将卡片从数组中剔除，导致刚流式拉取出来的封面卡片在数百毫秒内闪烁消失。
     - **修复**：
       1. 彻底移除 `candidateCovers.splice` 删卡逻辑，报错时仅优雅降级为本地默认封面占位图（`noCover.jpeg`），卡片与对应书源标签永久稳定保留；
       2. 图片渲染增加 `referrerpolicy="no-referrer"` 标头，并接入 `getCover(cover.url, true)` 后端代理中转双重防盗链，彻底解决第三方图床 403 阻断；
       3. 列表循环绑定 `:key="cover.url || idx"` 唯一键，避免流式追加新封面时旧卡片发生 DOM 闪烁重绘。
  11. **搜索后换源强校验先加书架**：
     - 恢复经典防呆逻辑：搜索完书籍直接阅读时，若要进行换源操作，必须先通过 `isInShelf` 弹出确认提示加入书架。
     - 强化 `App.vue` 的 `isInShelf` 流程：在用户确认后，先请求 `/saveBook`，成功后自动刷新 Vuex 书架状态，再继续执行换源并清除临时搜索路由参数；若用户取消加入书架，则立即终止换源。
  12. **书籍封面解析与图片展示全面恢复**：
     - **搜书列表封面还原**：恢复在搜书结果中展示真实书籍封面（移除图标占位）。
     - **封面自动匹配与拉取彻底修复**：之前在优化搜索时将 `BookList.kt` 中的 `ruleCoverUrl` 在搜索状态下置空（`if (isSearch) emptyList()`），导致搜书和流式换源返回的 `coverUrl` 全部为 `null`，使得封面候选完全拉取不到封面。现已彻底恢复 `ruleCoverUrl` 正常解析，重连书源真实封面流。
  13. **普通用户独立个人后台路径与弹窗彻底消除**：
     - 普通用户专属管理路径：支持 `http://.../#/[用户名]`，并保留管理员 `/admin` 路径。
     - 普通用户进入个人后台（书源管理、书架设置）时，前端严格跳过请求只有超级管理员才有权访问的系统全局接口（`/getUserList`, `/getSystemConfig`, `/getCloudSyncConfig`），**彻底消除了烦人的“请输入管理密码后继续操作”的弹窗拦截**。
  14. **GitHub Actions 自动化集成**：
     - `.github/workflows/docker-publish.yml` 新增 `dev` 分支触发支持，直接推送即可在云端触发全自动多架构编译与镜像发布。
  15. **全量侧边功能移入后台 & 普通用户自定义 WebDAV 与配置管理**：
     - **书源设置**：集成书源管理、探索书源（弹窗集成 Explore 组件并支持浏览发现书籍一键入架）、导入书源（支持文件上传与 JSON 粘贴）、远程书源（URL 链接拉取并解析）、失效书源（关键词体检与运行异常检测，支持一键清空失效源）、调试书源（快捷拉起独立调试器）。
     - **书架设置**：书籍管理（在线改封面、置顶、移除）、分组管理（自定义分类增删改）、导入书籍（本地上传 .txt, .epub, .cbz）、浏览书仓（服务端 `/localStore` 目录穿透与入架）、刷新缓存（即时重新载入最新书架状态）。
     - **用户空间**：展示当前登录身份与工作空间（userNS），支持在管理模式下拉取并切换用户空间；提供“备份用户配置”与“同步用户配置”，支持将本机阅读习惯、界面主题、书架配置一键备份至服务端或从服务端同步恢复。
     - **WebDAV 与云端备份**：集成 WebDAV 文件管理弹窗（支持云端文件浏览、下载、还原备份与图书入架）、一键保存备份至 WebDAV；**普通用户支持自定义 WebDAV 参数**（服务器地址、账户名、密码及远端目录），且支持单选切换“使用管理员默认设置”（自动继承同步管理员配置的 WebDAV 节点）。
     - **本地缓存**：可视化展示浏览器端缓存总量、书源缓存、章节列表缓存与章节正文缓存容量，提供精准清空书源缓存、清空章节列表缓存、清空章节内容缓存及全量重置缓存等实用操作。

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

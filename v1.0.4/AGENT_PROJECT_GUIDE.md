# Reader v1.0.4 智能体项目架构与维护说明 (AGENT_PROJECT_GUIDE)

> 本文档面向后续智能体与维护者，详细记录 v1.0.4 版本的业务逻辑、架构设计、Bug 根因排查与后续路线图。

---

## 1. 版本概览 (Version v1.0.4)
- **版本号**：v1.0.4 (递增规范：未来版本按 v1.0.5, v1.0.6 递增)
- **Git 目标分支**：`dev` (主开发演进分支)，验证通过后同步 fast-forward 合并至 `master`
- **主要更新点**：
  1. **彻底修复书源 JS 引擎 `ReferenceError: "getVariable" is not defined` 报错**：
     - 在 `AppConst.kt` 中注入跨对象全局 JS 兼容函数包 `JS_INJECT_HELPERS`，涵盖 `getVariable()`, `getVariable(key)`, `setVariable(...)`, `putVariable(...)`。
     - 在 `BaseSource.kt`、`AnalyzeUrl.kt`、`AnalyzeRule.kt` 的 `evalJS` 执行前自动注入该辅助层，并将上下文对象的变量存取方法对齐，彻底兼容各类第三方网络书源规则。
  2. **彻底解决失效书源一键清理“看似删除了，再点击失效书源依然弹出来”的问题**：
     - **前端原因修复**：修复 `Admin.vue` 的 `loadRuntimeInvalidSources` 将后端返回的数组误作 Object 做 `for in` 遍历导致获取数组下标 `"0"`, `"1"` 的 Bug，确保提取真实的 `bookSourceUrl`。
     - **前端请求修复**：修复关键词体检 `startKeywordSourceScan` 中直接请求 `/searchBook` 导致的路径偏差，补全 `this.api + "/searchBook"`。
     - **后端反序列化健壮性**：`BookSourceController.kt` 重构 `deleteBookSources`，不再使用易抛非空异常的 `mapTo(BookSource::class.java)`，改为直接提取 `bookSourceUrl` 集合快速过滤。
     - **缓存同步彻底清理**：后端新增 `/clearInvalidBookSources` 接口并在 `deleteBookSources` 时自动移除失效缓存，前端一键清理时双向调用，彻底清除失效书源记录。
  3. **普通用户“用户空间”界面去冗余优化**：
     - 隐藏普通用户下的“当前用户空间与身份信息（当前登录账号、工作空间 userNS）”只读卡片（增加 `v-if="isAdmin"` 仅管理员可见）。
     - 普通用户界面更加聚焦于实用的“终端配置同步与备份”。
  4. **书架自定义分组前台即时展示修复**：
     - 修复 `Index.vue` 计算属性 `bookGroupDisplayList` 原先强制要求 `this.getShowShelfBooks(v.groupId).length > 0` 导致刚新建的空分组在前台标签栏被自动过滤隐藏的 Bug。放宽为只要是用户自定义分组（`v.groupId > 0`），新建后即可在前台直接体现分组 Tab。
     - `Admin.vue` 中 `fetchBookGroups` 同步提交全局状态 `$store.commit("setBookGroupList")` 并刷新离线持久化存储；`Index.vue` 在 `activated` 钩子中同步刷新最新分组列表。
  5. **后台书籍管理增加图书手动分组功能**：
     - 后台书籍管理表格在“分组”列提供带编辑图标的可点击标签，在“操作”列新增“分组”按钮。
     - 新增“修改书籍分组”弹窗，提供用户所有自定义分组及“未分组”选项。
     - 后端 `BookController.kt` 将 `saveBookGroupId` 的范围校验由 `<= 0` 优化为 `< 0`，完美支持把图书设置回“未分组 (`0`)”。

---

## 2. 关键架构与模块对应

| 模块 | 前端对应文件 | 后端对应接口 / 控制器 |
| :--- | :--- | :--- |
| **JS 规则执行与变量兼容** | - | `AppConst.kt` (`JS_INJECT_HELPERS`), `AnalyzeRule.kt`, `AnalyzeUrl.kt`, `BaseSource.kt` |
| **失效书源体检与清理** | `web/src/views/Admin.vue` (`loadRuntimeInvalidSources`, `cleanupAllInvalidSources`) | `BookController.kt` (`getInvalidBookSources`, `clearInvalidBookSources`), `BookSourceController.kt` (`deleteBookSources`) |
| **书籍手动分组** | `web/src/views/Admin.vue` (`openChangeGroupDialog`, `confirmChangeGroup`) | `BookController.kt` (`saveBookGroupId`, `saveBook`) |
| **前台书架分组呈现** | `web/src/views/Index.vue` (`bookGroupDisplayList`) | `BookController.kt` (`getBookGroups`, `getBookshelf`) |
| **用户空间与配置管理** | `web/src/views/Admin.vue` (`activeTab === 'userspace'`) | `UserController.kt`, 本地浏览器离线缓存 |

---

## 3. 编译构建与自测验证记录

- **前端打包**：`cd web && yarn lint && yarn build`（无 Lint 错误，生产资源已完整同步至 `src/main/resources/web/`）。
- **后端编译**：`./gradlew compileKotlin` 编译通过，无任何语法与类型异常。
- **打包组装**：`./build.sh cli assemble` 成功生成生产 Fat JAR。
- **源码与说明归档**：完整归档至 `v1.0.4/source/` 并更新本说明文档。

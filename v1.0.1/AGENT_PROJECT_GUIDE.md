# Reader 项目智能体工程说明与维护指南 (AGENT_PROJECT_GUIDE)

> 版本：**v1.0.1**  
> 运行容器：`reader-local` (`0.0.0.0:8090->8080/tcp`)  
> 默认管理账号：`admin` / `admin`  
> 构建与打包规范：未收到用户明确“推送”指令前，严格禁止 `git push`。

---

## 1. 架构与主要模块说明

- **后端核心**：Kotlin + Vert.x Web 框架 + Spring 调度注解 (`@Scheduled`) + OkHttp3。
- **前端 SPA**：Vue 2.6 + Vuex + Element UI + Axios，自适配移动端与桌面端。
- **本地书仓模块** (`BookController.kt`)：
  - 路径：`storage/localStore`。
  - 实现了放书即读，修复了路径缺少前导斜杠导致“路径不存在”的字符串拼接缺陷。
  - 管理员账号全面豁免 `enable_local_store` 权限校验。
- **WebDAV 同步与备份模块** (`WebdavController.kt` / `WebDAV.vue`)：
  - 路径：`storage/data/<username>/webdav`。
  - 管理员账号全面放行 WebDAV 文件浏览与操作权限。
  - 优化了首次无客户端备份时的自动备份生成逻辑，支持网页端直接点击「保存备份」生成整站书源、书架、规则等压缩包。
- **云同步与定时备份模块** (`CloudSyncController.kt` / `Admin.vue`)：
  - 参考 **Firefly-Panel** 云存储架构演进（排除 Cloudflare，专注于 **GitHub 仓库** 与 **WebDAV 云盘**）。
  - **增量同步**：定时将系统核心配置（`users.json`、`systemConfig.json`、书源、书架、替换规则、书签等）同步至远端。
  - **全量备份**：将整个 `storage/data` 目录打包生成时间戳 ZIP 归档，直接上传至 GitHub Contents API 或 WebDAV 目录。
  - 后台轮询调度任务（每 5 分钟检查配置的定时调度策略并自动执行）。
- **TTS 语音服务**：基于 Python 3 + `edge-tts` 的微服务架构，映射到内部 8089 端口，支持微软高清自然语音朗读。

---

## 2. 本版本修复的 Bug 清单

1. **管理员权限限制问题**：
   - 原代码中即便用户拥有管理员角色或登录 `admin`，若未在普通用户字段勾选 `enable_local_store` 和 `enable_webdav`，会被拦截。
   - **修复方案**：在 `BaseController.kt`、`BookController.kt`、`WebdavController.kt` 中加入 `isAdmin` 判定，管理员账号全部直接放行。
2. **书仓路径拼接缺陷**：
   - 原代码使用 `home + path`，当 path 未包含 `/` 时拼接成 `/app/storage/localStoretest` 报“路径不存在”。
   - **修复方案**：对 path 严格进行前导斜杠格式化与边界检查。
3. **WebDAV 首次备份报错“请先使用阅读App备份”**：
   - 原逻辑强依赖本地已存在旧版备份。
   - **修复方案**：优化 `saveToWebdav`，未检测到旧备份时自动建立新归档打包，并支持网页端一键生成并下载。
4. **后台管理界面云同步与备份功能缺失**：
   - 新增「云同步与备份」标签页，支持配置 GitHub Token、仓库、分支、路径以及 WebDAV 地址与认证。
   - 支持设置「增量同步间隔」与「全量备份间隔」，并支持管理员一键手动立即触发。

---

## 3. 本地运行与测试指引

```bash
# 启动本地容器测试
docker run -d --name reader-local -p 8090:8080 -v $(pwd)/storage:/app/storage reader:local

# 访问地址
# Web 端主页: http://localhost:8090/
# 后台管理页面: http://localhost:8090/#/admin
```

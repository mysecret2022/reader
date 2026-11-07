# Reader v1.0.1 智能体工程自说明文档 (AGENT_PROJECT_GUIDE)

> **版本标识**：`v1.0.1`  
> **服务定位**：开源阅读 Web 端 (Spring Boot + Vert.x + Vue2 + Element-UI + Python Edge-TTS 神经语音引擎)  
> **容器运行端口**：宿主机 `8090` -> 容器内 `8080` (内部 TTS 监听 `127.0.0.1:8089`)

---

## 一、系统架构与模块拓扑

### 1.1 后端服务 (`src/main/java/com/htmake/reader/`)
* **`ReaderApplication.kt`**: Spring Boot 启动入口。
* **`RestVerticle.kt`**: Vert.x 响应式 HTTP Verticle，挂载主路由。
* **`YueduApi.kt`**:
  * 挂载 `/reader3/tts/voices` 与 `/reader3/tts`：无缝反向代理与中继本地 Edge TTS 神经语音引擎。
  * 自动初始化系统核心持久化目录：`storage/assets/`、`storage/localStore/`、`storage/cache/tts/`。
* **`BookController.kt`**:
  * 书籍、书架及本地书仓管理。
  * `getLocalStoreFileList`：自愈机制，当路径不存在时自动调用 `mkdirs()`，彻底杜绝前端“路径不存在”报错。
* **`UserController.kt`**:
  * 用户登录、注销、密码重置、多用户存储与权限控制。

### 1.2 语音合成服务 (`server/bin/tts_server.py`)
* 基于 Python `edge-tts` 库的本地极速微服务。
* 内置全套微软高保真神经中文语音（晓晓、云希、云健、晓伊、云扬、晓北、晓曼、晓臻）。
* 具备 MD5 级本地音频缓存（存储于 `storage/cache/tts/`），相同段落生成耗时从 3~5 秒降至 **30 毫秒**。

### 1.3 前端架构 (`web/src/`)
* **`views/Login.vue`**: 全屏无弹窗沉浸式毛玻璃拟态登录门户，支持账号登录、新用户注册与“游客体验 / 直接进入书架”快速通道。
* **`views/Admin.vue`**: 全新专用管理员后台控制台 (`/admin`)，包含系统运行概览、用户权限治理、书源健康排查、实时诊断日志查看及本地书仓维护。
* **`views/Index.vue`**: 主书架视图，顶部导航已移除失效的 RSS 入口，新增“管理后台 / 登录 / 个人中心”直通按钮；书源导入对话框集成 `@javascript` 与 `支持webview` 精细化授权过滤器。
* **`views/Reader.vue`**: 阅读器核心，语音朗读已升级为微软 Edge TTS 神经语音引擎，采用 HTML5 Audio 播放并附带下一段智能预加载机制，支持平滑回退至系统内置语音。

---

## 二、本次版本 (v1.0.1) 修复与改进清单

| 模块 | 问题与原状 | 修复方案与具体实现 |
| :--- | :--- | :--- |
| **本地书仓** | 点击“浏览书仓”时由于 `/app/storage/localStore` 目录初始不存在，后端抛出 `路径不存在` 错误 | 在 `YueduApi.kt` 启动与 `BookController.kt` 的 `getLocalStoreFileList` 中加入目录自检与自动创建逻辑 (`mkdirs()`) |
| **RSS 订阅** | 用户需求彻底移除 RSS 相关订阅功能 | 在 `App.vue` 与 `Index.vue` 中清理全部 RSS 导航标签、导入按钮、缓存清理项及相关无用组件引用 |
| **登录界面门户** | 之前未登录或单用户模式下登录界面隐藏，用户无法便捷访问与切换身份 | 设定为首次未登录时默认迎宾展示，并在 `Index.vue` 顶部新增常驻“登录/注册”入口；登录页支持“游客体验”一键直达 |
| **管理员专属后台** | 缺少专用的系统管理控制面板 | 新增专属路由 `/admin` 与 `Admin.vue` 视图，提供多维度概览、用户管理、书源排查、实时日志读取与书仓维护 |
| **微软神经语音库** | 浏览器 `window.speechSynthesis` 兼容性差、声音生硬甚至无法发声 | 引入微软 Edge TTS 神经语音引擎，提供晓晓、云希等优质自然声音，后端支持毫秒级音频缓存，前端支持流式预加载 |

---

## 三、运行与测试指引

### 3.1 容器启动与验证
```bash
# 启动容器并挂载数据目录
docker run -d --name reader-local \
  -p 8090:8080 \
  -v /root/.gemini/antigravity/scratch/reader/storage:/storage \
  -v /root/.gemini/antigravity/scratch/reader/logs:/logs \
  reader:local

# 验证书仓接口
curl -s http://127.0.0.1:8090/reader3/getLocalStoreFileList
# 响应: {"isSuccess":true,"errorMsg":"","data":[]}

# 验证微软语音库接口
curl -s http://127.0.0.1:8090/reader3/tts/voices
# 验证音频生成 (输出 audio/mpeg)
curl -s -i "http://127.0.0.1:8090/reader3/tts?text=测试微软晓晓语音&voice=zh-CN-XiaoxiaoNeural"
```

### 3.2 页面访问入口
* **主阅读器与书架**：`http://localhost:8090/`
* **管理员专属后台**：`http://localhost:8090/#/admin`

---

## 四、后续智能体迭代注意事项与路线图
1. **GitHub 推送限制**：当前已完成本地 Commit 与版本归档，**严禁执行 `git push`**，必须等待用户明确下达“推送”指令后方可推流。
2. **多用户安全模式**：若启用安全多用户模式，需在容器环境变量中配置 `SECURE=true` 与 `SECURE_KEY=your_key`。
3. **书源持续治理**：可在 `Admin.vue` 中扩展书源连通性自动并发 Ping 测试脚本。

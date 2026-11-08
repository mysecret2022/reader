# reader (阅读3 服务器增强版)

> **声明**：本项目基于原开源项目 [hectorqin/reader](https://github.com/hectorqin/reader) 进行二次开发与构建维护。维护者非官方原作者，特此向原作者团队及相关开源贡献者致敬。

---

## 📖 项目简介

**reader** 是一款功能强大、体验舒适的**阅读 3.0 服务器版 Web 阅读器**。
无需依赖手机端 App，即可在浏览器、手机、平板、电视盒子、电子墨水屏 (Kindle) 等任意设备上畅享小说阅读，支持书源管理、在线并发搜书、进度多端同步与云端备份。

### 🌟 本分支全新特性 (v1.0.3)
- 🚀 **极速无感换源**：重构换源与目录解析逻辑，引入单源 4 秒自动熔断机制，彻底告别传统换源“一直转圈读条卡死”的通病；清晰展示【当前】与【已使用】书源标签。
- 👥 **分权管理后台**：
  - **管理员控制台**：全站状态概览、配额治理、用户鉴权分配、WebDAV/GitHub AES 加密云同步与定时备份、诊断运行日志、全盘维护。
  - **普通用户专属子后台**：支持书源管理、失效源并发检测与一键清理、导入书源、**一键同步管理员优质默认书源**；支持书架书籍管理、**在线修改各源封面**、本地书仓浏览导入、书架与缓存刷新。
- 🎙️ **微软 Edge 神经语音 TTS 听书**：内置集成微软高保真 Edge TTS 语音引擎（晓晓、云希等多种自然语音），无需复杂配置即可随时听书。
- 🔒 **全数据密码加密云同步**：支持将书架、书源、配置增量加密同步到 GitHub 仓库（强制 AES 密码加密保护）及 WebDAV 网盘，确保数据安全无泄露风险。
- 🖼️ **极速轻量图书搜索**：搜索时不强制加载外部高分辨率图片封面，大幅节省流量与加载耗时；书籍加入书架后后台低并发自动无感补充 10 个备用源。
- 🖥️ **免弹窗拟态全屏登录**：高质感磨砂毛玻璃卡片设计，支持自主注册策略与邀请码开关。

---

## 🚀 安装部署指南

### 方式一：Docker Compose 极速部署（推荐 🌟）

适合部署在 Linux VPS、云服务器或群晖/威联通/绿联/极空间等各类 NAS 设备上。

#### 1. 创建配置文件
在宿主机新建目录（如 `reader`），并在该目录下创建 `docker-compose.yml` 文件：

```yaml
services:
  reader:
    image: ghcr.io/mysecret2022/reader:dev
    container_name: reader
    restart: always
    ports:
      - "8080:8080" # 外部访问端口，按需调整
    volumes:
      - ./storage:/storage # 数据持久化目录（书源、书架、缓存及配置）
      - ./logs:/app/logs   # 日志输出目录（与管理后台诊断日志实时互通）
    environment:
      - TZ=Asia/Shanghai
      - SPRING_PROFILES_ACTIVE=prod
```

#### 2. 启动服务
在包含 `docker-compose.yml` 的目录下执行：
```bash
docker compose up -d
```
启动完成后，在浏览器访问 `http://<服务器IP>:8080` 即可进入登录界面。
- **默认管理员账号**：`admin`
- **默认管理员密码**：`admin`（首次登录后请在后台管理页面修改密码；若系统存在历史管理员数据则以已有配置为准）

---

### 方式二：Docker 命令行一键运行

如果您习惯使用纯 Docker 命令：

```bash
docker run -d \
  --name reader \
  --restart always \
  -p 8080:8080 \
  -v $(pwd)/storage:/storage \
  -v $(pwd)/logs:/app/logs \
  -e TZ=Asia/Shanghai \
  -e SPRING_PROFILES_ACTIVE=prod \
  ghcr.io/mysecret2022/reader:dev
```

---

### 方式三：群晖 / 威联通 / 绿联 / 极空间 NAS 图形化部署

1. **拉取镜像**：在 NAS 的 Docker/Container 应用中搜索并拉取：
   `ghcr.io/mysecret2022/reader:dev`
2. **端口设置**：
   - 容器端口：`8080`
   - 本地端口：`8080`（或任意未占用端口）
3. **文件夹映射 (存储卷)**：
   - 本地 `docker/reader/storage` -> 容器内 `/storage`（或 `/app/storage`）
   - 本地 `docker/reader/logs` -> 容器内 `/app/logs`（或 `/logs`）
4. **环境参数添加**：
   - `TZ` = `Asia/Shanghai`
   - `SPRING_PROFILES_ACTIVE` = `prod`
5. 点击创建并运行容器即可。所有权限、密码与配额直接在进入后台 `http://<NAS_IP>:8080/#/admin` 界面在线设置。

---

### 方式四：Java JAR 包直接运行 (Windows / Linux / macOS)

仅需环境安装 **Java 11 (JRE 或 JDK 均可)**：

1. 前往本仓库 [Releases 页面](https://github.com/mysecret2022/reader/releases) 或 GitHub Actions 下载最新的 `reader-1.0.3.jar`。
2. 在终端或命令行执行启动：

```bash
# 1. 简易自用模式（免登录）
java -jar reader-1.0.3.jar --reader.server.port=8080

# 2. 多用户安全模式（推荐）
java -jar reader-1.0.3.jar \
  --reader.server.port=8080 \
  --reader.app.secure=true \
  --reader.app.secureKey=你的管理员密码 \
  --reader.app.cacheChapterContent=true
```

#### Windows 一键启动脚本 (.bat)
在 `reader-1.0.3.jar` 同目录下新建 `启动阅读.bat`（请确保保存编码为 ANSI/GBK）：
```cmd
@echo off
chcp 936 >nul
title Reader 服务端
echo 正在启动 Reader 阅读服务器，请稍候...
java -jar reader-1.0.3.jar --reader.server.port=8080 --reader.app.secure=true --reader.app.secureKey=admin123456
pause
```
双击 `启动阅读.bat` 即可开机即用，浏览器打开 `http://localhost:8080`。

---

## ⚙️ 核心参数与环境变量说明

> **推荐说明**：多用户登录鉴权、管理员密码、注册邀请码、系统用户上限、章节高速缓存等均已支持在管理员后台（`http://<服务器IP>:8080/#/admin`）在线可视化调整，即时生效且自动持久化保存，无需在容器配置中手动指定。如有特殊需要可在启动时预设以下环境变量：

| 环境变量参数 | 命令行对应参数 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `READER_APP_SECURE` | `--reader.app.secure` | `true` | 是否开启多用户登录鉴权（默认为 `true` 开启强制登录） |
| `READER_APP_SECUREKEY` | `--reader.app.secureKey` | 无 | 初始系统管理员 `admin` 的密码（系统初次启动默认密码为 `admin`） |
| `READER_APP_INVITECODE` | `--reader.app.inviteCode` | 空 | 注册专属邀请码（后台可随时开关） |
| `READER_APP_USERLIMIT` | `--reader.app.userLimit` | `50` | 允许注册的最大用户数上限（后台可弹性调整） |
| `READER_APP_USERBOOKLIMIT`| `--reader.app.userBookLimit` | `200` | 单个用户书架最大收藏图书数量 |
| `READER_APP_CACHECHAPTERCONTENT` | `--reader.app.cacheChapterContent` | `true` | 是否在服务端本地缓存章节正文内容（默认为 `true`） |
| `READER_SERVER_PORT` | `--reader.server.port` | `8080` | 服务端监听端口号 |

---

## 🛠️ 数据目录结构说明

所有书架、书源、用户账户与缓存均保存在本地 `storage` 目录下（容器内挂载至 `/app/storage`）：
```text
storage/
├── assets/       # 自定义阅读背景图、个性化样式文件
├── cache/        # 搜索缓存与封面图片高速缓存
├── data/         # 用户书源、书架、加密配置与用户信息
│   ├── admin/    # 管理员个人书架与默认书源 (供全站用户一键同步)
│   ├── users.json# 用户鉴权与加密凭据数据库
│   └── systemConfig.json # 全局动态配置
└── localStore/   # 共享本地书仓（可存放 txt/epub/cbz 书籍，前台一键入架）
```

---

## 🤝 致谢与开源引用声明 (Credits & Acknowledgements)

本项目深度站在开源巨人的肩膀上，对以下优秀开源项目、开发者以及社区作者表示由衷的感谢与崇高敬意：

1. **[hectorqin/reader](https://github.com/hectorqin/reader)**
   - 本项目核心代码源自作者 [hectorqin](https://github.com/hectorqin) 创造的 Reader 2.x 完整服务器端，感谢其在小说抓取规则解析、Vert.x 服务端架构上所做的卓越设计。
2. **[gedoor/legado (阅读3.0)](https://github.com/gedoor/legado) & [gedoor/MyBookshelf](https://github.com/gedoor/MyBookshelf)**
   - 开源界最强阅读神器，本项目规则引擎、数据结构标准与书源解析生态均脱胎于阅读开源社区规范。
3. **[XCQ0607/lxserver](https://github.com/XCQ0607/lxserver)**
   - 本项目免弹窗全屏拟态登录页面的极光渐变、磨砂玻璃与现代卡片式交互设计灵感来源于该项目的 Web 登录界面设计。
4. **[celetor/web-yuedu3](https://github.com/celetor/web-yuedu3)**
   - 为阅读 Web 端初期提供了优秀的交互设计与组件化实现借鉴。

---

## 📄 免责声明与开源协议

<details>
<summary>点击展开免责声明</summary>

阅读是一款提供网络文学搜索的工具，为广大网络文学爱好者提供一种方便、快捷舒适的试读体验。

当您搜索一本书的时，阅读会将该书的书名以关键词的形式提交到各个第三方网络文学网站。各第三方网站返回的内容与阅读无关，阅读对其概不负责，亦不承担任何法律责任。任何通过使用阅读而链接到的第三方网页均系他人制作或提供，您可能从第三方网页上获得其他服务，阅读对其合法性概不负责，亦不承担任何法律责任。第三方搜索引擎结果根据您提交的书名自动搜索获得并提供试读，不代表阅读赞成或被搜索链接到的第三方网页上的内容或立场。您应该对使用搜索引擎的结果自行承担风险。

阅读不做任何形式的保证：不保证第三方搜索引擎的搜索结果满足您的要求，不保证搜索服务不中断，不保证搜索结果的安全性、正确性、及时性、合法性。因网络状况、通讯线路、第三方网站等任何原因而导致您不能正常使用阅读，阅读不承担任何法律责任。阅读尊重并保护所有使用阅读用户的个人隐私权，您注册的用户名、电子邮件地址等个人资料，非经您亲自许可或根据相关法律、法规的强制性规定，阅读不会主动地泄露给第三方。
</details>

- 本项目遵循开源许可证协议，完整保留了原作者的 [LICENSE](LICENSE) 声明文件。
- 更多详细的高级接口与自定义规则配置请查阅 [doc.md](doc.md)。

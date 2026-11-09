# 文档 (Documentation)

> **声明与出处**：本项目基于原开源项目 [hectorqin/reader](https://github.com/hectorqin/reader) 进行二次开发、架构重构与持续维护，维护者非官方原作者。特此向原作者团队及所有开源贡献者致敬。

---

- [文档 (Documentation)](#文档-documentation)
  - [免责声明（Disclaimer）](#免责声明disclaimer)
  - [数据存储](#数据存储)
    - [存储目录结构](#存储目录结构)
    - [本地书仓](#本地书仓)
  - [阅读页面与访问](#阅读页面与访问)
    - [全功能 Web 端](#全功能-web-端)
  - [阅读主题与新版阅读体验](#阅读主题与新版阅读体验)
    - [主题与视觉定制](#主题与视觉定制)
    - [v2.0 交互升级](#v20-交互升级)
    - [自定义 CSS 样式](#自定义-css-样式)
  - [接口服务配置 (application.yml)](#接口服务配置-applicationyml)
  - [WebDAV 同步配置](#webdav-同步配置)
  - [部署与运行指南](#部署与运行指南)
    - [Docker 部署（推荐）](#docker-部署推荐)
    - [Docker Compose 部署（标准推荐）](#docker-compose-部署标准推荐)
    - [Java 原生运行 (Jar 包)](#java-原生运行-jar-包)
  - [Nginx 反向代理配置](#nginx-反向代理配置)
  - [开发与源码编译](#开发与源码编译)
    - [编译前端 (Web)](#编译前端-web)
    - [编译后端 (Gradle)](#编译后端-gradle)
    - [快捷构建脚本](#快捷构建脚本)
  - [后端 API 接口文档](#后端-api-接口文档)
    - [与阅读 3 (Legado) 协议兼容性](#与阅读-3-legado-协议兼容性)
    - [核心业务接口](#核心业务接口)
      - [1. 加入书架 (`saveBook`)](#1-加入书架-savebook)
      - [2. 获取书籍书源 (`getBookSource`)](#2-获取书籍书源-getbooksource)
      - [3. 搜索书籍更多书源 (`searchBookSource`)](#3-搜索书籍更多书源-searchbooksource)
      - [4. 书籍换源 (`setBookSource`)](#4-书籍换源-setbooksource)

---

## 免责声明（Disclaimer）

1. **阅读**是一款提供网络文学搜索、格式转换与多端排版渲染的开源工具，致力于为广大网络文学爱好者提供方便、舒适、无广告的个人试读体验。
2. 当您搜索一本书籍时，阅读会将书名关键词提交至由您自行添加或配置的第三方网络文学书源网站。各第三方网站返回的内容与阅读软件本身无关，阅读对其合法性、真实性、时效性概不负责，亦不承担任何连带法律责任。
3. 第三方书源搜索结果由自动化程序抓取呈现，不代表阅读赞成或代表被搜索网页上的内容或立场。使用者应自行对网络内容的辨别与版权负责，并提倡支持正版文学作品。
4. 阅读充分尊重并保护用户个人隐私与数据安全，所有数据（书架、进度、缓存、书源）完全存储在用户自建的本地或私有服务器环境中，不会向任何未授权第三方泄露。

---

## 数据存储

接口服务默认采用轻量高效的**纯文件系统**进行存储（无外部数据库依赖），默认存储位置为程序工作目录下的 `storage` 目录。

> **参数覆盖**：启动时可通过 Java 系统属性 `-Dreader.app.storagePath=/path/to/storage` 或环境变量自定义存储路径。

### 存储目录结构

```bash
storage
├── assets                                        # 静态资源与用户媒体
│   ├── <用户名>                                  # 多用户模式下特定用户的静态资源（如 admin）
│   │   ├── covers                                # 本地导入书籍的封面图片
│   │   └── background                            # 自定义阅读背景图
│   └── reader.css                                # 全局自定义 CSS 样式文件
├── cache                                         # 临时缓存目录
│   ├── bookInfoCache                             # 书籍详情及搜索 ACache 缓存
│   └── [hash].jpg                                # 远程章节与封面图片本地缓存
├── data                                          # 业务核心数据（含原子防掉保护机制）
│   ├── default                                   # 单用户模式或未登录状态默认数据目录
│   │   ├── bookSource.json                       # 书源配置列表
│   │   ├── bookshelf.json                        # 书架图书列表
│   │   └── 书名_作者名                            # 书籍离线章节缓存及目录
│   ├── <用户名>                                  # 多用户模式下独立用户数据空间（如 admin）
│   │   ├── bookSource.json                       # 用户私有书源列表
│   │   ├── bookshelf.json                        # 用户私有书架列表
│   │   ├── webdav                                # WebDAV 同步及备份归档
│   │   └── 书名_作者名                            # 章节内容缓存
│   └── users.json                                # 用户鉴权账户列表 (secure 开启时)
├── localStore                                    # 本地公共书仓（支持所有有权限用户浏览导入）
│   ├── 示例书籍A.txt
│   └── 示例书籍B.epub
└── windowConfig.json                             # 桌面端/嵌入式窗口状态配置文件
```

### 本地书仓

在 `storage/localStore` 目录中可以集中存放 txt、epub 等本地电子书文件。在多用户模式下，管理员为用户赋予“书仓权限”后，用户即可在 Web 界面的「书架 -> 浏览书仓」中直接选择并一键批量导入到个人书架中阅读。

---

## 阅读页面与访问

### 全功能 Web 端

```text
http://ip:端口/
```

- 适用于现代 PC 浏览器（Chrome、Edge、Safari、Firefox）及智能手机、平板、电纸书等各类设备。
- 支持全屏沉浸阅读、仿真 3D 纸张翻页、双边缘手势滑动、深浅主题切换、字体定制与多端书签实时同步。

---

## 阅读主题与新版阅读体验

### 主题与视觉定制

- **白天与黑夜**：书架与管理后台全面支持深色模式与浅色模式。
- **自定义色彩与壁纸**：支持自主设定页面背景色、内容文字颜色、浮动控制台背景色以及上传自定义阅读背景图片。

### v2.0 交互升级

- 📖 **仿真翻页（3D 纸书翻折效果）**：
  采用客户端硬件加速变换与曲度光影，翻页时具备真实纸张背光与底层书脊阴影，翻过 90 度自动切换纸张背面，翻页效果丝滑且**服务器资源占用为 0%**。
- ✂️ **选词快捷气泡与独立高亮**：
  长按或滑动划选正文文本时，弹出【复制】、【添加书签】、【添加过滤规则】轻量操作气泡；配合独立选区高亮层，**彻底屏蔽 iOS Safari 和 Android 系统的原生浏览器弹窗**，杜绝误触与跳屏。
- 📐 **3 标签等宽规范排版**：
  设置面板采用统一的 3 标签等宽网格排版，视觉整洁一致；特殊模式分为「正常」、「简洁」、「边缘识别」，简洁说明动态悬浮呈现。
- 🔤 **霞鹜文楷优质排版字体**：
  内置轻量化高品质中文字体「霞鹜文楷」，开箱即享出版级字形排版质感。
- 📱 **屏幕边缘侧滑手势**：
  在手机端全屏模式下，支持左/右边缘向内滑动返回书架，带有弧形指示器与防误触二次确认。

### 自定义 CSS 样式

如需进行深度样式微调，可在 `storage/assets/reader.css` 文件中编写自定义 CSS 规则。该文件会在前端页面加载时自动引入并生效。
> 提示：覆盖默认样式时建议搭配 `!important` 提升权重。

---

## 接口服务配置 (application.yml)

服务端主要配置位于 `conf/application.properties` 或 `src/main/resources/application.yml` 中。以下为核心参数及中文含义说明：

```yaml
reader:
  app:
    workDir: ""            # 服务工作目录（为空时自动识别为当前目录）
    secure: true           # 是否开启登录鉴权，默认为 true（标准多用户权限模式，系统自动创建默认管理员：admin / admin）
    inviteCode: ""         # 注册邀请码：为空则开放注册；可直接在 Web 管理后台可视化配置
    secureKey: ""          # 初始管理密码：若留空则首次运行默认管理员密码为 admin，可在后台直接修改
    cacheChapterContent: false # 是否在服务器持久化缓存抓取的章节正文内容
    debugLog: false        # 是否打印详细的抓取与通信调试日志
    autoClearInactiveUser: 0 # 自动清理不活跃用户天数：0 表示不清理，大于 0 表示清理超过指定天数未登录的用户
    mongoUri: ""           # MongoDB 数据库连接串（可选，用于数据远程异地备份）
    mongoDbName: "reader"  # MongoDB 备份数据库名
    shelfUpdateInteval: 10 # 书架后台自动追更检测间隔（单位：分钟，须为 10 的倍数）
    userLimit: 50          # 允许注册的最大用户数上限
    remoteWebviewApi: ""   # 外部 remote-webview 节点地址（用于辅助破解极验或强 JS 防爬书源）
    defaultUserEnableWebdav: true    # 新注册用户是否默认开通 WebDAV 同步服务
    defaultUserEnableLocalStore: true # 新注册用户是否默认允许访问本地公共书仓
    defaultUserEnableBookSource: true # 新注册用户是否允许私有编辑/添加书源（false 则只能共享公共书源）
    defaultUserEnableRssSource: true  # 新注册用户是否允许配置 RSS 订阅源
    defaultUserBookSourceLimit: 200   # 新用户默认可添加的最大书源数量
    defaultUserBookLimit: 300         # 新用户书架默认最大书籍容量
    autoBackupUserData: false         # 是否开启周期性自动备份用户数据
    minUserPasswordLength: 6          # 注册密码最低字符长度
    remoteBookSourceUpdateInterval: 720 # 远程订阅书源自动拉取刷新间隔（单位：分钟）

  server:
    port: 8080             # 服务监听 HTTP 端口
    contextPath: ""        # 二级虚拟子路径（例如 /reader，留空表示根路径）
    webUrl: "http://localhost:${reader.server.port}" # 外部访问基准 URL
```

---

## WebDAV 同步配置

服务内置与阅读 3 (Legado) 客户端协议完全兼容的 WebDAV 服务端接口，支持多端书架与阅读进度双向同步：

1. **阅读手机端 App 配置**：
   - 打开阅读手机 App -> 我的 -> 备份与恢复 -> WebDAV 设置。
   - **WebDAV 地址**：`http://你的服务器IP:端口/reader3/webdav/`
   - **账号密码**：
     - 使用在网页端注册的个人用户名和密码登录（或使用超级管理员账号 `admin` 及密码）。
2. **执行备份**：
   - 在手机 App 中点击「备份到 WebDAV」，系统会将书源、书架及进度打包上传。
3. **网页端恢复与同步**：
   - 在 Web 网页端点击「用户 -> WebDAV 备份」，即可查看备份时间戳，并一键导入还原；
   - 阅读器在打开书籍时会自动拉取并对齐最新的章节阅读位置。

---

## 部署与运行指南

### Docker 命令行一键运行（推荐）

直接使用标准 Docker 命令启动容器服务：

```bash
docker run -d \
  --name reader \
  --restart always \
  -p 8080:8080 \
  -v $(pwd)/storage:/storage \
  -v $(pwd)/logs:/app/logs \
  -e TZ=Asia/Shanghai \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e READER_APP_SECURE=true \
  ghcr.io/mysecret2022/reader:latest
```

> **默认管理员账户与安全说明**：
> - **默认管理员账号**：`admin`
> - **默认初始密码**：`admin`
> - 容器首次启动后，访问 `http://<服务器IP>:8080` 即可使用 `admin / admin` 登录。
> - 登录后进入管理后台，可直接在线修改管理员密码，并在「系统设置」中可视化开启/关闭注册或设定专属注册邀请码。
> - 如需在启动时直接通过环境变量预设管理密码或邀请码，可在命令中添加 `-e "READER_APP_SECUREKEY=自定义管理密码" -e "READER_APP_INVITECODE=自定义邀请码"`。

### Docker Compose 部署（标准推荐）

推荐在服务器上使用 `docker-compose.yaml` 管理，方便持久化与后续一键更新：

```yaml
services:
  reader:
    image: ghcr.io/mysecret2022/reader:latest
    container_name: reader
    restart: always
    ports:
      - "8080:8080" # 外部访问端口，按需调整（宿主机端口:容器端口）
    volumes:
      - ./storage:/storage # 数据持久化目录（书源、书架、用户数据、缓存及配置）
      - ./logs:/app/logs   # 日志输出目录（与管理后台诊断日志实时互通）
    environment:
      - TZ=Asia/Shanghai
      - SPRING_PROFILES_ACTIVE=prod
      - READER_APP_SECURE=true # 默认多用户鉴权模式（默认管理员：admin，默认密码：admin）
      # 可在首次登录后台后直接修改密码及配置邀请码，也可通过下方环境变量直接初始化：
      # - READER_APP_SECUREKEY=admin # 初始管理员密码
      # - READER_APP_INVITECODE=      # 注册邀请码（留空则开放注册）
```

**操作命令**：
```bash
# 启动服务
docker compose up -d

# 查看日志
docker compose logs -f

# 更新至最新镜像
docker compose pull && docker compose up -d
```

### Java 原生运行 (Jar 包)

需要环境：Java 8 / 11 / 17 及以上 JRE/JDK。

```bash
# 启动 Jar 服务
java -jar reader-server.jar --spring.profiles.active=prod

# 指定自定义数据存储路径
java -Dreader.app.storagePath=/opt/reader/storage -jar reader-server.jar
```

---

## Nginx 反向代理配置

若需要使用域名访问（如配合 SSL 证书或复用 80/443 端口），推荐 Nginx 配置如下：

```nginx
server {
    listen 80;
    server_name reader.yourdomain.com;

    # 启用 Gzip 压缩，提高移动端静态文件加载速度
    gzip on;
    gzip_min_length 1k;
    gzip_buffers 4 16k;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/javascript application/json application/javascript application/x-javascript;
    gzip_vary on;

    # 上传本地大体积 txt / epub 电子书必须放开限制
    client_max_body_size 100m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;

        # WebSocket 支持
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # 真实客户端信息透传
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
    }
}
```

---

## 开发与源码编译

本项目为标准的前后端分离架构：
- **后端**：Kotlin + Spring Boot + JavaFX 引擎
- **前端**：Vue 2 + Vue CLI + Tailwind / ElementUI

### 编译前端 (Web)

```bash
cd web

# 1. 安装前端依赖
yarn install

# 2. 本地开发热重载（端口 8081）
yarn serve

# 3. 生产打包并自动同步到后端资源目录 (src/main/resources/web)
yarn sync
```

### 编译后端 (Gradle)

```bash
# 执行打包构建
./gradlew assemble

# 运行构建产物
java -jar build/libs/reader-2.0.0.jar
```

### 快捷构建脚本

根目录下自带 `./build.sh` 脚本工具：
- `./build.sh sync`：编译前端并将静态资源同步到后端 resources
- `./build.sh build`：全量构建
- `./build.sh win` / `./build.sh linux` / `./build.sh mac`：打包对应平台可执行文件

---

## 后端 API 接口文档

### 与阅读 3 (Legado) 协议兼容性

本服务完整向下兼容 [阅读 3 (Legado) Web 接口规范](https://github.com/gedoor/legado/blob/master/api.md)，所有请求统一带有 `/reader3/` 路由前缀。

### 核心业务接口

#### 1. 加入书架 (`saveBook`)

- **URL**：`/reader3/saveBook`
- **Method**：`POST`
- **Content-Type**：`application/json`
- **Request Body**：

```json
{
    "name": "示例小说",
    "author": "示例作者",
    "bookUrl": "https://www.example.com/book/123.html",
    "origin": "https://www.example.com",
    "originName": "示例书源",
    "type": 0,
    "intro": "书籍简介说明...",
    "tocUrl": "",
    "latestChapterTitle": "第100章 最终决战",
    "time": 1690000000000
}
```

- **Response**：
```json
{
    "isSuccess": true,
    "errorMsg": "",
    "data": {
        "name": "示例小说",
        "author": "示例作者",
        "bookUrl": "https://www.example.com/book/123.html"
    }
}
```

#### 2. 获取书籍书源 (`getBookSource`)

- **URL**：`/reader3/getBookSource?url=xxx`
- **Method**：`GET`
- **说明**：获取指定 URL 绑定的书源详情。
- **Response**：
```json
{
    "isSuccess": true,
    "errorMsg": "",
    "data": [
        {
            "bookSourceUrl": "https://www.example.com",
            "bookSourceName": "示例书源",
            "bookSourceType": 0
        }
    ]
}
```

#### 3. 搜索书籍更多书源 (`searchBookSource`)

- **URL**：`/reader3/searchBookSource?name=书名&author=作者名&lastIndex=0`
- **Method**：`GET`
- **说明**：在所有启用的书源中并行检索匹配的同名同作者书籍。
- **Response**：
```json
{
    "isSuccess": true,
    "errorMsg": "",
    "data": [
        {
            "name": "书名",
            "author": "作者名",
            "bookUrl": "https://www.site-b.com/book/456.html",
            "origin": "https://www.site-b.com",
            "originName": "站点B",
            "latestChapterTitle": "最新章节标题"
        }
    ]
}
```

#### 4. 书籍换源 (`setBookSource`)

- **URL**：`/reader3/setBookSource`
- **Method**：`POST`
- **Content-Type**：`application/json`
- **Request Body**：
```json
{
    "name": "书籍名称",
    "newUrl": "https://www.site-b.com/book/456.html",
    "bookSourceUrl": "https://www.site-b.com"
}
```
- **Response**：
```json
{
    "isSuccess": true,
    "errorMsg": "",
    "data": {
        "name": "书籍名称",
        "bookUrl": "https://www.site-b.com/book/456.html"
    }
}
```

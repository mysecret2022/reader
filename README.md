# reader (阅读3 服务器增强版)

> **声明**：本项目基于原开源项目 [rorschach883/reader](https://github.com/rorschach883/reader) 与 [hectorqin/reader](https://github.com/hectorqin/reader) 进行二次开发与构建维护。维护者非官方原作者，特此向原作者团队及相关开源贡献者致敬。

---

## 📖 项目简介

**reader** 是一款功能强大、体验舒适的**阅读 3.0 服务器版 Web 阅读器**。
无需依赖手机端 App，即可在浏览器、平板、电子墨水屏 (Kindle) 等任意设备上畅享小说阅读，支持书源管理、在线并发搜书、进度云端同步与 WebDAV 备份。

### 🌟 本分支专属改进与特性
- **免弹窗全屏拟态登录页**：彻底移除原生简易弹窗，引入沉浸式高质感毛玻璃拟态登录/注册界面（视觉设计参考 [XCQ0607/lxserver](https://github.com/XCQ0607/lxserver)）；
- **全套预构建前端资源集成**：源码仓库内直接内置编译好的 Web 资源，无论是服务器还是个人电脑，**仅需 Java 运行环境即可一键启动，无需安装 Node.js、npm 或 yarn**；
- **支持 Docker / Docker Compose 一键部署**：容器化持久运行，数据安全可靠。

---

## ✨ 核心功能

- 📚 **书架与书源**：支持导入主流阅读 3.0 JSON 书源、书源失效检测、分组管理、书架定时更新。
- 🔍 **智能搜索**：多书源并发快速搜书、书籍换源、目录重载、章节正文智能排版。
- 📱 **多端全设备适配**：桌面大屏、手机移动端适配，专门提供针对 Kindle / 墨水屏优化的 `simple-web` 模式。
- 🎨 **高度个性化**：预设多种雅致阅读背景（羊皮纸、边彩画布、墨水屏黑白等），支持自定义字体大小、行距、翻页方式与 CSS 样式。
- ☁️ **数据同步与多用户**：支持 WebDAV 进度自动备份与还原、多用户隔离与鉴权管理机制。
- 🎧 **多媒体与本地书仓**：支持本地导入 TXT、EPUB、UMD、PDF 格式书籍，支持音频书籍与部分浏览器听书功能。

---

## 🚀 快速启动指南

### 方式一：Docker Compose 极速部署（推荐 🌟）

适合部署在 Linux / VPS / NAS 服务器上。新建 `docker-compose.yml` 文件：

```yaml
version: '3.8'

services:
  reader:
    image: hectorqin/reader:latest
    container_name: reader
    restart: always
    ports:
      - "8080:8080" # 访问端口，可按需修改，如 4396:8080
    volumes:
      - ./storage:/storage # 数据持久化目录（书源、书架、缓存及配置）
      - ./logs:/logs       # 日志输出目录
    environment:
      - TZ=Asia/Shanghai
      - SPRING_PROFILES_ACTIVE=prod
      # ================= 权限模式配置 =================
      # 个人自用直接免密：READER_APP_SECURE=false
      # 开启多用户与登录鉴权：READER_APP_SECURE=true
      - READER_APP_SECURE=true
      - READER_APP_SECUREKEY=admin_password_123 # 管理员密码（必改）
      - READER_APP_INVITECODE=                  # 注册邀请码（留空则开放自由注册）
      - READER_APP_CACHECHAPTERCONTENT=true     # 开启章节内容本地缓存
      - READER_APP_USERLIMIT=50                 # 最大用户限制
```

启动命令：
```bash
docker compose up -d
```
启动后在浏览器打开 `http://<服务器IP>:8080` 即可开始阅读。

---

### 方式二：直接运行编译好的 JAR 包

仅需安装 **Java 11 (JRE 或 JDK 均可)**：

1. 前往本仓库 [Releases 页面](https://github.com/mysecret2022/reader/releases) 下载最新的 `reader-2.5.4.jar`。
2. 在终端/命令行中执行启动：
   ```bash
   # 单用户自用模式
   java -jar reader-2.5.4.jar --reader.server.port=8080

   # 多用户登录模式（安全模式）
   java -jar reader-2.5.4.jar --reader.server.port=8080 --reader.app.secure=true --reader.app.secureKey=你的管理密码
   ```

---

### 方式三：从本仓库源码运行 / 重新编译

由于本项目已包含内置 Web 资源，编译极为简便：

```bash
# 1. 克隆代码
git clone https://github.com/mysecret2022/reader.git
cd reader

# 2. 一键编译并运行后端服务（端口 8080）
./build.sh cli serve 8080

# 或仅编译出可执行 JAR 包
./build.sh cli assemble
# 编译产物位于 build/libs/reader-2.5.4.jar
```

---

## 🛠️ 数据目录结构说明

所有书架、书源与用户设置均保存在本地 `storage` 目录下（Docker 部署时挂载在容器 `/storage`）：
```text
storage/
├── assets/       # 自定义阅读背景、封面与样式
├── cache/        # 搜索缓存与封面图片缓存
├── data/         # 用户书源 (bookSource.json)、书架 (bookshelf.json)、用户信息
└── localStore/   # 本地集中书仓（可在此放入本地 txt/epub 书籍）
```

---

## 🤝 致谢与开源引用声明 (Credits & Acknowledgements)

本项目深度站在开源巨人的肩膀上，对以下优秀开源项目、开发者以及社区作者表示由衷的感谢与崇高敬意：

1. **[hectorqin/reader](https://github.com/hectorqin/reader) & [rorschach883/reader](https://github.com/rorschach883/reader)**
   - 本项目核心代码源自作者 [hectorqin](https://github.com/hectorqin) 创造的 Reader 2.x 完整服务器端，感谢其在小说抓取规则解析、Vert.x 服务端架构上所做的卓越设计。
2. **[gedoor/legado (阅读3.0)](https://github.com/gedoor/legado) & [gedoor/MyBookshelf](https://github.com/gedoor/MyBookshelf)**
   - 开源界最强阅读神器，本项目规则引擎、数据结构标准与书源解析生态均脱胎于阅读开源社区规范。
3. **[XCQ0607/lxserver](https://github.com/XCQ0607/lxserver)**
   - 本项目免弹窗全屏拟态登录页面的极光渐变、磨砂玻璃与现代卡片式交互设计灵感来源于该项目的 Web 登录界面设计。
4. **[celetor/web-yuedu3](https://github.com/celetor/web-yuedu3)**
   - 为阅读 Web 端初期提供了优秀的交互设计与组件化实现借鉴。
5. **[lightink-小说API](https://github.com/lightink-qingmo/lightink-server)**
   - 为项目早期小说 API 设计思路提供了重要参考。

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

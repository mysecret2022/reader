package com.htmake.reader.api

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssSource
import io.legado.app.data.entities.RssArticle
import io.legado.app.model.webBook.WebBook
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler;
import mu.KotlinLogging
import com.htmake.reader.config.AppConfig
import com.htmake.reader.config.BookConfig
import io.legado.app.constant.DeepinkBookSource
import com.htmake.reader.api.controller.BookController
import com.htmake.reader.api.controller.BookSourceController
import com.htmake.reader.api.controller.RssSourceController
import com.htmake.reader.api.controller.UserController
import com.htmake.reader.api.controller.WebdavController
import com.htmake.reader.api.controller.ReplaceRuleController
import com.htmake.reader.api.controller.BookmarkController
import com.htmake.reader.api.controller.CloudSyncController
import com.htmake.reader.utils.error
import com.htmake.reader.utils.success
import com.htmake.reader.utils.getStorage
import com.htmake.reader.utils.saveStorage
import com.htmake.reader.utils.asJsonArray
import com.htmake.reader.utils.asJsonObject
import com.htmake.reader.utils.toDataClass
import com.htmake.reader.utils.toMap
import com.htmake.reader.utils.fillData
import com.htmake.reader.utils.getWorkDir
import com.htmake.reader.utils.getRandomString
import com.htmake.reader.utils.genEncryptedPassword
import com.htmake.reader.entity.User
import com.htmake.reader.utils.SpringContextUtils
import com.htmake.reader.utils.deleteRecursively
import com.htmake.reader.utils.unzip
import com.htmake.reader.utils.zip
import com.htmake.reader.utils.jsonEncode
import com.htmake.reader.utils.getRelativePath
import com.htmake.reader.verticle.RestVerticle
import com.htmake.reader.SpringEvent
import org.springframework.stereotype.Component
import io.vertx.core.json.JsonObject
import io.vertx.core.json.JsonArray
import io.vertx.core.http.HttpMethod
import com.htmake.reader.api.ReturnData
import io.legado.app.utils.MD5Utils
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URL;
import java.util.UUID;
import io.vertx.ext.web.client.WebClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import java.io.File
import java.lang.Runtime
import kotlin.collections.mutableMapOf
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat;
import io.legado.app.utils.EncoderUtils
import io.legado.app.model.rss.Rss
import org.springframework.scheduling.annotation.Scheduled
import io.legado.app.model.localBook.LocalBook
import java.nio.file.Paths
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CoroutineScope

private val logger = KotlinLogging.logger {}

@Component
class YueduApi : RestVerticle() {
    @Autowired
    private lateinit var appConfig: AppConfig

    @Autowired
    private lateinit var env: Environment

    override suspend fun initRouter(router: Router) {
        setupPort()

        // 旧版数据迁移
        migration()

        // web界面
        router.route("/*").handler(StaticHandler.create("web").setDefaultContentEncoding("UTF-8"));

        // assets
        var assetsDir = getWorkDir("storage", "assets");
        var assetsDirFile = File(assetsDir);
        if (!assetsDirFile.exists()) {
            assetsDirFile.mkdirs();
        }
        var assetsCss = getWorkDir("storage", "assets", "reader.css");
        var assetsCssFile = File(assetsCss);
        if (!assetsCssFile.exists()) {
            assetsCssFile.writeText("/* 在此处可以编写CSS样式来自定义页面 */");
        }
        router.route("/assets/*").handler(StaticHandler.create().setAllowRootFileSystemAccess(true).setWebRoot(assetsDir).setDefaultContentEncoding("UTF-8"));

        // localStore 目录自初始化
        val localStoreDir = File(getWorkDir("storage", "localStore"))
        if (!localStoreDir.exists()) {
            localStoreDir.mkdirs()
        }
        // TTS 缓存目录初始化
        val ttsCacheDir = File(getWorkDir("storage", "cache", "tts"))
        if (!ttsCacheDir.exists()) {
            ttsCacheDir.mkdirs()
        }

        // epub资源
        var dataDir = getWorkDir("storage", "data");
        router.route("/epub/*").handler {
            var path = it.request().path().replace("/epub/", "/", true)
            path = URLDecoder.decode(path, "UTF-8")
            if (path.endsWith("html", true)) {
                var filePath = File(dataDir + path)
                if (filePath.exists()) {
                    // 处理 js 注入脚本
                    BookConfig.injectJavascriptToEpubChapter(filePath.toString())
                }
            }
            it.next()
        }
        router.route("/epub/*").handler(StaticHandler.create().setAllowRootFileSystemAccess(true).setWebRoot(dataDir).setDefaultContentEncoding("UTF-8"));

        // 获取系统信息
        router.get("/reader3/getSystemInfo").coroutineHandler { getSystemInfo(it) }


        ////////// 接口部分
        val bookController = BookController(coroutineContext)
        val bookSourceController = BookSourceController(coroutineContext)
        val rssSourceController = RssSourceController(coroutineContext)
        val userController = UserController(coroutineContext)
        val webdavController = WebdavController(coroutineContext, router) { ctx, error ->
            onHandlerError(ctx, error)
        }
        val replaceRuleController = ReplaceRuleController(coroutineContext)
        val bookmarkController = BookmarkController(coroutineContext)
        val cloudSyncController = CloudSyncController(coroutineContext)

        /** 云同步与备份模块 */
        router.get("/reader3/getCloudSyncConfig").coroutineHandler { cloudSyncController.getCloudSyncConfig(it) }
        router.post("/reader3/saveCloudSyncConfig").coroutineHandler { cloudSyncController.saveCloudSyncConfig(it) }
        router.post("/reader3/triggerCloudSync").coroutineHandler { cloudSyncController.triggerCloudSync(it) }
        router.post("/reader3/triggerCloudBackup").coroutineHandler { cloudSyncController.triggerCloudBackup(it) }

        /** 书源模块 */
        router.post("/reader3/saveBookSource").coroutineHandler { bookSourceController.saveBookSource(it) }
        router.post("/reader3/saveBookSources").coroutineHandler { bookSourceController.saveBookSources(it) }

        router.get("/reader3/getBookSource").coroutineHandler { bookSourceController.getBookSource(it) }
        router.post("/reader3/getBookSource").coroutineHandler { bookSourceController.getBookSource(it) }
        router.get("/reader3/getBookSources").coroutineHandler { bookSourceController.getBookSources(it) }
        router.post("/reader3/getBookSources").coroutineHandler { bookSourceController.getBookSources(it) }

        router.post("/reader3/deleteAllBookSources").coroutineHandler { bookSourceController.deleteAllBookSources(it) }
        router.post("/reader3/deleteBookSource").coroutineHandler { bookSourceController.deleteBookSource(it) }
        router.post("/reader3/deleteBookSources").coroutineHandler { bookSourceController.deleteBookSources(it) }
        router.post("/deleteBookSource").coroutineHandler { bookSourceController.deleteBookSource(it) }
        router.post("/deleteBookSources").coroutineHandler { bookSourceController.deleteBookSources(it) }

        // 书源测速体检
        router.post("/reader3/testBookSourceSpeed").coroutineHandler { bookSourceController.testBookSourceSpeed(it) }
        router.get("/reader3/testBookSourceSpeed").coroutineHandler { bookSourceController.testBookSourceSpeed(it) }
        router.post("/testBookSourceSpeed").coroutineHandler { bookSourceController.testBookSourceSpeed(it) }
        router.get("/testBookSourceSpeed").coroutineHandler { bookSourceController.testBookSourceSpeed(it) }
        router.post("/reader3/testBookSourcesSpeed").coroutineHandler { bookSourceController.testBookSourcesSpeed(it) }
        router.post("/testBookSourcesSpeed").coroutineHandler { bookSourceController.testBookSourcesSpeed(it) }

        // 上传书源文件
        router.post("/reader3/readSourceFile").coroutineHandler { bookSourceController.readSourceFile(it) }
        router.post("/readSourceFile").coroutineHandler { bookSourceController.readSourceFile(it) }

        // 读取远程书源文件
        router.post("/reader3/readRemoteSourceFile").coroutineHandlerWithoutRes { bookSourceController.readRemoteSourceFile(it) }
        router.post("/readRemoteSourceFile").coroutineHandlerWithoutRes { bookSourceController.readRemoteSourceFile(it) }

        // 设置默认书源与同步管理员书源
        router.post("/reader3/setAsDefaultBookSources").coroutineHandler { bookSourceController.setAsDefaultBookSources(it) }
        router.post("/reader3/syncAdminBookSources").coroutineHandler { bookSourceController.syncAdminBookSources(it) }
        router.get("/reader3/syncAdminBookSources").coroutineHandler { bookSourceController.syncAdminBookSources(it) }
        router.post("/syncAdminBookSources").coroutineHandler { bookSourceController.syncAdminBookSources(it) }
        router.get("/syncAdminBookSources").coroutineHandler { bookSourceController.syncAdminBookSources(it) }
        router.post("/reader3/deleteUserBookSource").coroutineHandler { bookSourceController.deleteUserBookSource(it) }
        router.post("/reader3/deleteBookSourcesFile").coroutineHandler { bookSourceController.deleteBookSourcesFile(it) }
        router.post("/deleteBookSourcesFile").coroutineHandler { bookSourceController.deleteBookSourcesFile(it) }

        /** 书籍模块 */
        // 书架
        router.get("/reader3/getBookshelf").coroutineHandler { bookController.getBookshelf(it) }
        router.get("/reader3/getShelfBook").coroutineHandler { bookController.getShelfBook(it) }
        router.post("/reader3/saveBook").coroutineHandler { bookController.saveBook(it) }
        router.post("/reader3/deleteBook").coroutineHandler { bookController.deleteBook(it) }
        router.post("/reader3/deleteBooks").coroutineHandler { bookController.deleteBooks(it) }

        // 失效书源
        router.post("/reader3/getInvalidBookSources").coroutineHandler { bookController.getInvalidBookSources(it) }
        router.post("/getInvalidBookSources").coroutineHandler { bookController.getInvalidBookSources(it) }
        router.post("/reader3/clearInvalidBookSources").coroutineHandler { bookController.clearInvalidBookSources(it) }
        router.post("/clearInvalidBookSources").coroutineHandler { bookController.clearInvalidBookSources(it) }

        // 探索
        router.post("/reader3/exploreBook").coroutineHandler { bookController.exploreBook(it) }
        router.get("/reader3/exploreBook").coroutineHandler { bookController.exploreBook(it) }

        // 搜索
        router.get("/reader3/searchBook").coroutineHandler { bookController.searchBook(it) }
        router.post("/reader3/searchBook").coroutineHandler { bookController.searchBook(it) }
        router.get("/reader3/searchBookMulti").coroutineHandler { bookController.searchBookMulti(it) }
        router.post("/reader3/searchBookMulti").coroutineHandler { bookController.searchBookMulti(it) }
        router.get("/reader3/searchBookMultiSSE").coroutineHandlerWithoutRes { bookController.searchBookMultiSSE(it) }

        // 书籍详情
        router.get("/reader3/getBookInfo").coroutineHandler { bookController.getBookInfo(it) }
        router.post("/reader3/getBookInfo").coroutineHandler { bookController.getBookInfo(it) }

        // 章节列表
        router.get("/reader3/getChapterList").coroutineHandler { bookController.getChapterList(it) }
        router.post("/reader3/getChapterList").coroutineHandler { bookController.getChapterList(it) }

        // 内容
        router.get("/reader3/getBookContent").coroutineHandler { bookController.getBookContent(it) }
        router.post("/reader3/getBookContent").coroutineHandler { bookController.getBookContent(it) }

        // 保存阅读进度
        router.post("/reader3/saveBookProgress").coroutineHandler { bookController.saveBookProgress(it) }

        // 封面
        router.get("/reader3/cover").coroutineHandlerWithoutRes { bookController.getBookCover(it) }

        // 搜索其它来源
        router.get("/reader3/searchBookSource").coroutineHandler { bookController.searchBookSource(it) }
        router.post("/reader3/searchBookSource").coroutineHandler { bookController.searchBookSource(it) }
        router.get("/reader3/getAvailableBookSource").coroutineHandler { bookController.getAvailableBookSource(it) }
        router.post("/reader3/getAvailableBookSource").coroutineHandler { bookController.getAvailableBookSource(it) }
        router.get("/reader3/searchBookSourceSSE").coroutineHandlerWithoutRes { bookController.searchBookSourceSSE(it) }

        // 换源
        router.get("/reader3/setBookSource").coroutineHandler { bookController.setBookSource(it) }
        router.post("/reader3/setBookSource").coroutineHandler { bookController.setBookSource(it) }

        // 修改分组
        router.post("/reader3/saveBookGroupId").coroutineHandler { bookController.saveBookGroupId(it) }
        router.post("/reader3/addBookGroupMulti").coroutineHandler { bookController.addBookGroupMulti(it) }
        router.post("/reader3/removeBookGroupMulti").coroutineHandler { bookController.removeBookGroupMulti(it) }

        // 导入本地文件
        router.post("/reader3/importBookPreview").coroutineHandler { bookController.importBookPreview(it) }
        router.post("/reader3/refreshLocalBook").coroutineHandler { bookController.refreshLocalBook(it) }

        // 获取txt章节规则
        router.get("/reader3/getTxtTocRules").coroutineHandler { bookController.getTxtTocRules(it) }
        router.post("/reader3/getChapterListByRule").coroutineHandler { bookController.getChapterListByRule(it) }

        // 书籍分组
        router.get("/reader3/getBookGroups").coroutineHandler { bookController.getBookGroups(it) }
        router.post("/reader3/saveBookGroup").coroutineHandler { bookController.saveBookGroup(it) }
        router.post("/reader3/deleteBookGroup").coroutineHandler { bookController.deleteBookGroup(it) }
        router.post("/reader3/saveBookGroupOrder").coroutineHandler { bookController.saveBookGroupOrder(it) }

        // 书仓功能
        // 获取书仓文件列表
        router.get("/reader3/getLocalStoreFileList").coroutineHandler { bookController.getLocalStoreFileList(it) }
        router.get("/getLocalStoreFileList").coroutineHandler { bookController.getLocalStoreFileList(it) }
        // 下载书仓文件
        router.get("/reader3/getLocalStoreFile").coroutineHandlerWithoutRes { bookController.getLocalStoreFile(it) }
        router.get("/getLocalStoreFile").coroutineHandlerWithoutRes { bookController.getLocalStoreFile(it) }
        // 删除书仓文件
        router.post("/reader3/deleteLocalStoreFile").coroutineHandler { bookController.deleteLocalStoreFile(it) }
        router.post("/deleteLocalStoreFile").coroutineHandler { bookController.deleteLocalStoreFile(it) }
        router.post("/reader3/deleteLocalStoreFileList").coroutineHandler { bookController.deleteLocalStoreFileList(it) }
        router.post("/deleteLocalStoreFileList").coroutineHandler { bookController.deleteLocalStoreFileList(it) }
        // 从本地书仓/webdav导入
        router.post("/reader3/importFromLocalPathPreview").coroutineHandler { bookController.importFromLocalPathPreview(it) }
        router.post("/importFromLocalPathPreview").coroutineHandler { bookController.importFromLocalPathPreview(it) }
        // 上传文件到书仓
        router.post("/reader3/uploadFileToLocalStore").coroutineHandler { bookController.uploadFileToLocalStore(it) }
        router.post("/uploadFileToLocalStore").coroutineHandler { bookController.uploadFileToLocalStore(it) }

        // 调试书源
        router.get("/reader3/bookSourceDebugSSE").coroutineHandlerWithoutRes { bookController.bookSourceDebugSSE(it) }

        // 缓存书籍章节
        router.get("/reader3/cacheBookSSE").coroutineHandlerWithoutRes { bookController.cacheBookSSE(it) }
        // 获取书籍缓存信息
        router.get("/reader3/getShelfBookWithCacheInfo").coroutineHandler { bookController.getShelfBookWithCacheInfo(it) }
        // 删除书籍章节缓存
        router.post("/reader3/deleteBookCache").coroutineHandler { bookController.deleteBookCache(it) }

        // 导出书籍
        router.post("/reader3/exportBook").coroutineHandlerWithoutRes { bookController.exportBook(it) }
        router.get("/reader3/exportBook").coroutineHandlerWithoutRes { bookController.exportBook(it) }

        // 全文搜索
        router.get("/reader3/searchBookContent").coroutineHandler { bookController.searchBookContent(it) }
        router.post("/reader3/searchBookContent").coroutineHandler { bookController.searchBookContent(it) }

        // 微软 Edge TTS 神经语音库接口
        val voicesHandler: suspend (io.vertx.ext.web.RoutingContext) -> ReturnData = { _ ->
            val returnData = ReturnData()
            val voices = listOf(
                mapOf("id" to "zh-CN-XiaoxiaoNeural", "name" to "晓晓 (女声·小说优选)", "gender" to "Female", "locale" to "zh-CN"),
                mapOf("id" to "zh-CN-YunxiNeural", "name" to "云希 (男声·生动多情)", "gender" to "Male", "locale" to "zh-CN"),
                mapOf("id" to "zh-CN-YunjianNeural", "name" to "云健 (男声·热血沉稳)", "gender" to "Male", "locale" to "zh-CN"),
                mapOf("id" to "zh-CN-XiaoyiNeural", "name" to "晓伊 (女声·灵动甜美)", "gender" to "Female", "locale" to "zh-CN"),
                mapOf("id" to "zh-CN-YunyangNeural", "name" to "云扬 (男声·专业播音)", "gender" to "Male", "locale" to "zh-CN"),
                mapOf("id" to "zh-CN-liaoning-XiaobeiNeural", "name" to "晓北 (东北女声·幽默)", "gender" to "Female", "locale" to "zh-CN"),
                mapOf("id" to "zh-HK-HiuMaanNeural", "name" to "晓曼 (粤语女声·标准)", "gender" to "Female", "locale" to "zh-HK"),
                mapOf("id" to "zh-TW-HsiaoChenNeural", "name" to "晓臻 (国语女声·台湾)", "gender" to "Female", "locale" to "zh-TW")
            )
            returnData.setData(voices)
        }
        router.get("/reader3/tts/voices").coroutineHandler(voicesHandler)
        router.get("/reader3/reader3/tts/voices").coroutineHandler(voicesHandler)
        router.get("/tts/voices").coroutineHandler(voicesHandler)

        val ttsHandler: suspend (io.vertx.ext.web.RoutingContext) -> Unit = { context ->
            val query = context.request().query() ?: ""
            try {
                val url = java.net.URL("http://127.0.0.1:8089/tts?" + query)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 40000
                conn.requestMethod = "GET"
                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val response = context.response()
                    response.putHeader("Content-Type", "audio/mpeg")
                    response.putHeader("Cache-Control", "public, max-age=604800")
                    val buffer = io.vertx.core.buffer.Buffer.buffer(conn.inputStream.readBytes())
                    response.end(buffer)
                } else {
                    val errBytes = conn.errorStream?.readBytes() ?: conn.inputStream?.readBytes() ?: ByteArray(0)
                    context.response().setStatusCode(responseCode).end(String(errBytes))
                }
            } catch (e: Exception) {
                logger.error("TTS proxy error: ${e.message}")
                context.response().setStatusCode(503).end("TTS service unavailable: ${e.message}")
            }
        }
        router.get("/reader3/tts").coroutineHandlerWithoutRes(ttsHandler)
        router.get("/reader3/reader3/tts").coroutineHandlerWithoutRes(ttsHandler)
        router.get("/tts").coroutineHandlerWithoutRes(ttsHandler)

        // 系统日志读取接口 (供诊断自查)
        val systemLogsHandler: suspend (RoutingContext) -> ReturnData = { context ->
            val returnData = ReturnData()
            val linesParam = context.queryParam("lines").firstOrNull()?.toIntOrNull() ?: 200
            val errorOnly = context.queryParam("error").firstOrNull()?.toBoolean() ?: false

            val candidateDirs = listOf(
                java.io.File("/app/logs"),
                java.io.File("./logs"),
                java.io.File("/logs"),
                java.io.File(getWorkDir("logs")),
                java.io.File(System.getProperty("user.dir", ".") + "/logs"),
                java.io.File("/storage/logs")
            )
            val logFiles = candidateDirs
                .filter { it.exists() && it.isDirectory }
                .flatMap { dir ->
                    dir.listFiles { f -> f.isFile && (f.name.endsWith(".log") || f.name.endsWith(".txt")) }?.toList() ?: emptyList()
                }
                .distinctBy { it.canonicalPath }
                .sortedByDescending { it.lastModified() }

            if (logFiles.isEmpty()) {
                val searchedDirs = candidateDirs.filter { it.exists() }.map { it.absolutePath }
                returnData.setData(listOf("暂无日志文件或日志目录未初始化（已检测目录: $searchedDirs）"))
            } else {
                val allLines = mutableListOf<String>()
                for (file in logFiles.take(3)) {
                    try {
                        val lines = file.readLines(Charsets.UTF_8)
                        allLines.addAll(0, lines)
                        if (allLines.size >= linesParam * 2) break
                    } catch (e: Exception) {
                        logger.warn("读取日志文件失败: ${file.absolutePath}, ${e.message}")
                    }
                }
                val filtered = if (errorOnly) {
                    allLines.filter { it.contains("ERROR") || it.contains("Exception") || it.contains("WARN") }
                } else {
                    allLines
                }
                returnData.setData(filtered.takeLast(linesParam))
            }
        }
        router.get("/reader3/getSystemLogs").coroutineHandler(systemLogsHandler)
        router.get("/getSystemLogs").coroutineHandler(systemLogsHandler)

        /** 用户模块 */
        // 上传文件
        router.post("/reader3/uploadFile").coroutineHandler { userController.uploadFile(it) }
        router.post("/uploadFile").coroutineHandler { userController.uploadFile(it) }

        // 删除文件
        router.post("/reader3/deleteFile").coroutineHandler { userController.deleteFile(it) }
        router.post("/deleteFile").coroutineHandler { userController.deleteFile(it) }

        // 登录 (支持 /reader3 前缀及根路径)
        router.post("/reader3/login").coroutineHandler { userController.login(it) }
        router.post("/login").coroutineHandler { userController.login(it) }
        // 注销登录
        router.post("/reader3/logout").coroutineHandler { userController.logout(it) }
        router.post("/logout").coroutineHandler { userController.logout(it) }

        // 获取用户信息
        router.get("/reader3/getUserInfo").coroutineHandler { userController.getUserInfo(it) }
        router.get("/getUserInfo").coroutineHandler { userController.getUserInfo(it) }

        // 用户备份本地配置
        router.post("/reader3/saveUserConfig").coroutineHandler { userController.saveUserConfig(it) }
        router.post("/saveUserConfig").coroutineHandler { userController.saveUserConfig(it) }

        // 用户恢复本地配置
        router.get("/reader3/getUserConfig").coroutineHandler { userController.getUserConfig(it) }
        router.get("/getUserConfig").coroutineHandler { userController.getUserConfig(it) }

        // 获取用户列表
        router.get("/reader3/getUserList").coroutineHandler { userController.getUserList(it) }
        router.get("/getUserList").coroutineHandler { userController.getUserList(it) }

        // 删除用户
        router.post("/reader3/deleteUsers").coroutineHandler { userController.deleteUsers(it) }
        router.post("/deleteUsers").coroutineHandler { userController.deleteUsers(it) }

        // 添加用户
        router.post("/reader3/addUser").coroutineHandler { userController.addUser(it) }
        router.post("/addUser").coroutineHandler { userController.addUser(it) }

        // 重置用户密码
        router.post("/reader3/resetPassword").coroutineHandler { userController.resetPassword(it) }
        router.post("/resetPassword").coroutineHandler { userController.resetPassword(it) }

        // 更新用户
        router.post("/reader3/updateUser").coroutineHandler { userController.updateUser(it) }
        router.post("/updateUser").coroutineHandler { userController.updateUser(it) }

        // 系统全局配置与清理维护
        router.get("/reader3/getSystemConfig").coroutineHandler { userController.getSystemConfig(it) }
        router.get("/getSystemConfig").coroutineHandler { userController.getSystemConfig(it) }
        router.post("/reader3/saveSystemConfig").coroutineHandler { userController.saveSystemConfig(it) }
        router.post("/saveSystemConfig").coroutineHandler { userController.saveSystemConfig(it) }
        router.post("/reader3/clearSystemCache").coroutineHandler { userController.clearSystemCache(it) }
        router.post("/clearSystemCache").coroutineHandler { userController.clearSystemCache(it) }

        // 个人 WebDAV 配置接口
        router.get("/reader3/getUserWebdavConfig").coroutineHandler { userController.getUserWebdavConfig(it) }
        router.get("/getUserWebdavConfig").coroutineHandler { userController.getUserWebdavConfig(it) }
        router.post("/reader3/saveUserWebdavConfig").coroutineHandler { userController.saveUserWebdavConfig(it) }
        router.post("/saveUserWebdavConfig").coroutineHandler { userController.saveUserWebdavConfig(it) }


        /** webdav模块 */
        // 获取webdav备份列表
        router.get("/reader3/getWebdavFileList").coroutineHandler { webdavController.getWebdavFileList(it) }
        router.get("/getWebdavFileList").coroutineHandler { webdavController.getWebdavFileList(it) }

        // 下载webdav文件
        router.get("/reader3/getWebdavFile").coroutineHandlerWithoutRes { webdavController.getWebdavFile(it) }
        router.get("/getWebdavFile").coroutineHandlerWithoutRes { webdavController.getWebdavFile(it) }

        // 上传webdav文件
        router.post("/reader3/uploadFileToWebdav").coroutineHandler { webdavController.uploadFileToWebdav(it) }
        router.post("/uploadFileToWebdav").coroutineHandler { webdavController.uploadFileToWebdav(it) }

        // 删除webdav文件
        router.get("/reader3/deleteWebdavFile").coroutineHandler { webdavController.deleteWebdavFile(it) }
        router.get("/deleteWebdavFile").coroutineHandler { webdavController.deleteWebdavFile(it) }
        router.post("/reader3/deleteWebdavFile").coroutineHandler { webdavController.deleteWebdavFile(it) }
        router.post("/deleteWebdavFile").coroutineHandler { webdavController.deleteWebdavFile(it) }
        router.post("/reader3/deleteWebdavFileList").coroutineHandler { webdavController.deleteWebdavFileList(it) }
        router.post("/deleteWebdavFileList").coroutineHandler { webdavController.deleteWebdavFileList(it) }

        // 从webdav备份恢复
        router.post("/reader3/restoreFromWebdav").coroutineHandler { webdavController.restoreFromWebdav(it) }
        router.post("/restoreFromWebdav").coroutineHandler { webdavController.restoreFromWebdav(it) }

        // 备份到webdav
        router.post("/reader3/backupToWebdav").coroutineHandler { webdavController.backupToWebdav(it) }
        router.post("/backupToWebdav").coroutineHandler { webdavController.backupToWebdav(it) }


        /** rss模块 */
        // rss
        router.get("/reader3/getRssSources").coroutineHandler { rssSourceController.getRssSources(it) }
        router.post("/reader3/saveRssSource").coroutineHandler { rssSourceController.saveRssSource(it) }
        router.post("/reader3/saveRssSources").coroutineHandler { rssSourceController.saveRssSources(it) }
        router.post("/reader3/deleteRssSource").coroutineHandler { rssSourceController.deleteRssSource(it) }
        // rss 列表
        router.get("/reader3/getRssArticles").coroutineHandler { rssSourceController.getRssArticles(it) }
        router.post("/reader3/getRssArticles").coroutineHandler { rssSourceController.getRssArticles(it) }
        // rss 内容
        router.get("/reader3/getRssContent").coroutineHandler { rssSourceController.getRssContent(it) }
        router.post("/reader3/getRssContent").coroutineHandler { rssSourceController.getRssContent(it) }

        /** 替换规则模块 */
        router.get("/reader3/getReplaceRules").coroutineHandler { replaceRuleController.getReplaceRules(it) }
        router.post("/reader3/saveReplaceRule").coroutineHandler { replaceRuleController.saveReplaceRule(it) }
        router.post("/reader3/saveReplaceRules").coroutineHandler { replaceRuleController.saveReplaceRules(it) }
        router.post("/reader3/deleteReplaceRule").coroutineHandler { replaceRuleController.deleteReplaceRule(it) }
        router.post("/reader3/deleteReplaceRules").coroutineHandler { replaceRuleController.deleteReplaceRules(it) }

        /** 书签模块 */
        router.get("/reader3/getBookmarks").coroutineHandler { bookmarkController.getBookmarks(it) }
        router.post("/reader3/saveBookmark").coroutineHandler { bookmarkController.saveBookmark(it) }
        router.post("/reader3/saveBookmarks").coroutineHandler { bookmarkController.saveBookmarks(it) }
        router.post("/reader3/deleteBookmark").coroutineHandler { bookmarkController.deleteBookmark(it) }
        router.post("/reader3/deleteBookmarks").coroutineHandler { bookmarkController.deleteBookmarks(it) }
    }

    suspend fun setupPort() {
        logger.info("port: {}", port)
        var serverPort = env.getProperty("reader.server.port", Int::class.java)
        logger.info("serverPort: {}", serverPort)
        if (serverPort != null && serverPort > 0) {
            port = serverPort;
        }
    }

    suspend fun migration() {
        try {
            var storageDir = File(getWorkDir("storage"))
            var dataDir = File(getWorkDir("storage", "data", "default"))
            if (!storageDir.exists()) {
                // 直接使用新版本，则创建 default 目录，防止重启之后被迁移
                dataDir.mkdirs()
            } else if (!dataDir.exists()) {
                // 旧版本不管了
                dataDir.mkdirs()
                // 可能存在旧版本，尝试迁移
                // var backupDir = File(getWorkDir("storage-backup"))
                // storageDir.renameTo(backupDir)
                // dataDir.parentFile.mkdirs()
                // backupDir.copyRecursively(dataDir)
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    override fun started() {
        SpringContextUtils.getApplicationContext().publishEvent(SpringEvent(this as java.lang.Object, "READY", ""));
    }

    override fun onStartError() {
        SpringContextUtils.getApplicationContext().publishEvent(SpringEvent(this as java.lang.Object, "START_ERROR", "应用启动失败，请检查" + port + "端口是否被占用"));
    }

    override fun onHandlerError(ctx: RoutingContext, error: Exception) {
        val returnData = ReturnData()
        logger.error("onHandlerError: ", error)
        if (!ctx.response().headWritten()) {
            ctx.success(returnData.setErrorMsg(error.toString()))
        } else {
            ctx.response().end(error.toString())
        }
    }

    private suspend fun getSystemInfo(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        var systemFont = System.getProperty("reader.system.fonts")
        var freeMemory = "" + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "M"
        var totalMemory = "" + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "M"
        var maxMemory = "" + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "M"
        return returnData.setData(mapOf(
            "fonts" to systemFont,
            "freeMemory" to freeMemory,
            "totalMemory" to totalMemory,
            "maxMemory" to maxMemory
        ))
    }

    /**
     * 定时任务
     */

    /**
     * 每十分钟检查一次书架书籍更新
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    fun shelfUpdateJob()
    {
        launch(Dispatchers.IO) {
            try {
                val bookController = BookController(coroutineContext)

                logger.info("开始检查书架书籍更新")
                // 刷新系统默认书架
                bookController.getBookShelfBooks(true, "default")

                // 刷新用户书架
                if (appConfig.secure) {
                    var userMap = mutableMapOf<String, Map<String, Any>>()
                    var userMapJson: JsonObject? = asJsonObject(getStorage("data", "users"))
                    if (userMapJson != null) {
                        userMap = userMapJson.map as MutableMap<String, Map<String, Any>>
                    }
                    userMap.forEach{
                        try {
                            var ns = it.value.getOrDefault("username", "") as String? ?: ""
                            if (ns.isNotEmpty()) {
                                bookController.getBookShelfBooks(true, ns)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                logger.info("书架书籍更新检查结束")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 每天清理不活跃用户
     */
    @Scheduled(cron = "0 59 23 * * ?")
    fun clearUser()
    {
        if (appConfig.autoClearInactiveUser <= 0 || !appConfig.secure) {
            return
        }
        launch(Dispatchers.IO) {
            try {
                logger.info("开始清理 {} 天未登录用户", appConfig.autoClearInactiveUser)

                var userMap = mutableMapOf<String, Map<String, Any>>()
                var userMapJson: JsonObject? = asJsonObject(getStorage("data", "users"))
                if (userMapJson != null) {
                    userMap = userMapJson.map as MutableMap<String, Map<String, Any>>
                }
                val expireTime = System.currentTimeMillis() - appConfig.autoClearInactiveUser * 86400L * 1000L
                userMap.keys.forEach{
                    try {
                        var user = userMap.get(it)
                        if (user != null) {
                            var username = user.getOrDefault("username", "") as String? ?: ""
                            var last_login_at = user.getOrDefault("last_login_at", 0) as Long? ?: 0L
                            if (username.isNotEmpty() && last_login_at < expireTime) {
                                logger.info("delete user: {}", user)
                                // 删除用户信息
                                userMap.remove(username)
                                // 移除用户目录
                                var userHome = File(getWorkDir("storage", "data", username))
                                logger.info("delete userHome: {}", userHome)
                                if (userHome.exists()) {
                                    userHome.deleteRecursively()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                logger.info("不活跃用户自动清理结束")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var lastIncrementalSyncTimestamp: Long = 0L
    private var lastFullBackupTimestamp: Long = 0L

    /**
     * 云同步与定时备份轮询调度任务 (每 5 分钟检查一次配置的定时策略)
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    fun cloudSyncSchedulerJob() {
        launch(Dispatchers.IO) {
            try {
                val configJson = asJsonObject(getStorage("data", "cloudSyncConfig")) ?: return@launch
                val enabled = configJson.getBoolean("enabled") ?: false
                if (!enabled) return@launch

                val now = System.currentTimeMillis()
                val incrementalMinutes = configJson.getInteger("incrementalIntervalMinutes") ?: 30
                val fullHours = configJson.getInteger("fullBackupIntervalHours") ?: 24

                val controller = CloudSyncController(coroutineContext)

                // 检查增量同步间隔 (大于 0 且到达时间)
                if (incrementalMinutes > 0) {
                    val intervalMillis = incrementalMinutes * 60L * 1000L
                    if (now - lastIncrementalSyncTimestamp >= intervalMillis) {
                        logger.info("触发自动云端增量同步...")
                        val res = controller.executeIncrementalSync()
                        lastIncrementalSyncTimestamp = now
                        logger.info("自动云端增量同步结果: {}", res)
                    }
                }

                // 检查全量备份间隔 (大于 0 且到达时间)
                if (fullHours > 0) {
                    val intervalMillis = fullHours * 3600L * 1000L
                    if (now - lastFullBackupTimestamp >= intervalMillis) {
                        logger.info("触发自动云端全量打包备份...")
                        val res = controller.executeFullBackup()
                        lastFullBackupTimestamp = now
                        logger.info("自动云端全量打包备份结果: {}", res)
                    }
                }
            } catch (e: Exception) {
                logger.error("cloudSyncSchedulerJob error", e)
            }
        }
    }
}
package com.htmake.reader.api.controller

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.RssSource
import io.legado.app.data.entities.RssArticle
import io.legado.app.model.webBook.WebBook
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler;
import mu.KotlinLogging
import com.htmake.reader.config.AppConfig
import com.htmake.reader.config.BookConfig
import io.legado.app.constant.DeepinkBookSource
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
import io.legado.app.utils.ACache
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
import kotlin.coroutines.CoroutineContext
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
import java.util.concurrent.TimeUnit
import io.legado.app.help.http.okHttpClient
import okhttp3.Request

private val logger = KotlinLogging.logger {}

class BookSourceController(coroutineContext: CoroutineContext): BaseController(coroutineContext) {
    private var webClient: WebClient

    init {
        webClient = SpringContextUtils.getBean("webClient", WebClient::class.java)
    }

    suspend fun getUserBookSourceJson(userNameSpace: String): JsonArray? {
        val rawStorage = getUserStorage(userNameSpace, "bookSource")
        var bookSourceList: JsonArray? = asJsonArray(rawStorage)
        if (rawStorage == null) {
            if (!userNameSpace.equals("admin") && !userNameSpace.equals("default")) {
                // 用户书源文件不存在，优先拷贝默认管理员(admin)的书源，其次default
                var adminBookSourceList: JsonArray? = asJsonArray(getUserStorage("admin", "bookSource"))
                    ?: asJsonArray(getUserStorage("default", "bookSource"))
                if (adminBookSourceList != null && !adminBookSourceList.isEmpty()) {
                    saveUserStorage(userNameSpace, "bookSource", adminBookSourceList.getList())
                    bookSourceList = adminBookSourceList
                }
            } else if (userNameSpace.equals("admin")) {
                var defaultSources: JsonArray? = asJsonArray(getUserStorage("default", "bookSource"))
                if (defaultSources != null && !defaultSources.isEmpty()) {
                    saveUserStorage("admin", "bookSource", defaultSources.getList())
                    bookSourceList = defaultSources
                }
            }
        }
        return bookSourceList
    }

    suspend fun syncAdminBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val userNameSpace = getUserNameSpace(context)
        val adminSources = asJsonArray(getUserStorage("admin", "bookSource"))
            ?: asJsonArray(getUserStorage("default", "bookSource"))
        if (adminSources == null || adminSources.isEmpty()) {
            return returnData.setErrorMsg("默认管理员尚未配置书源")
        }
        val isPreview = context.queryParam("preview").firstOrNull() == "1" ||
                context.queryParam("dryRun").firstOrNull() == "1"
        if (!isPreview) {
            saveUserStorage(userNameSpace, "bookSource", adminSources.getList())
        }
        return returnData.setData(adminSources.getList())
    }

    suspend fun saveBookSource(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val bookSource = BookSource.fromJson(context.bodyAsString).getOrNull()
        if (bookSource == null) {
            return returnData.setErrorMsg("参数错误")
        }
        // val bookSource = context.bodyAsJson.mapTo(BookSource::class.java)

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        // 遍历判断书本是否存在
        var existIndex: Int = -1
        for (i in 0 until bookSourceList.size()) {
            var _bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
            if (_bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)) {
                existIndex = i
                break;
            }
        }
        if (existIndex >= 0) {
            var sourceList = bookSourceList.getList()
            sourceList.set(existIndex, JsonObject.mapFrom(bookSource))
            bookSourceList = JsonArray(sourceList)
        } else {
            bookSourceList.add(JsonObject.mapFrom(bookSource))
        }

        // logger.info("bookSourceList: {}", bookSourceList)
        saveUserStorage(userNameSpace, "bookSource", bookSourceList)
        if (userNameSpace.equals("admin") || userNameSpace.equals("default")) {
            saveUserStorage("default", "bookSource", bookSourceList)
        }
        return returnData.setData("")
    }

    suspend fun saveBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val bookSourceJsonArray = context.bodyAsJsonArray
        if (bookSourceJsonArray == null) {
            return returnData.setErrorMsg("参数错误")
        }
        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace) ?: JsonArray()

        val isSortOrReorder = context.queryParam("sort").firstOrNull() == "1" ||
                context.queryParam("reorder").firstOrNull() == "1" ||
                context.queryParam("sort").firstOrNull() == "true" ||
                context.queryParam("reorder").firstOrNull() == "true"

        if (isSortOrReorder) {
            val finalBookSourceList = JsonArray()
            val seenUrls = HashSet<String>()
            for (k in 0 until bookSourceJsonArray.size()) {
                val item = bookSourceJsonArray.getJsonObject(k) ?: continue
                val bookSource = BookSource.fromJson(item.toString()).getOrNull() ?: continue
                if (seenUrls.add(bookSource.bookSourceUrl)) {
                    finalBookSourceList.add(JsonObject.mapFrom(bookSource))
                }
            }
            // 补全不在排序清单中的其余书源
            for (i in 0 until bookSourceList.size()) {
                val item = bookSourceList.getJsonObject(i)
                val url = item?.getString("bookSourceUrl") ?: ""
                if (url.isNotEmpty() && !seenUrls.contains(url)) {
                    finalBookSourceList.add(item)
                }
            }
            bookSourceList = finalBookSourceList
        } else {
            for (k in 0 until bookSourceJsonArray.size()) {
                val bookSource = BookSource.fromJson(bookSourceJsonArray.getJsonObject(k).toString()).getOrNull()
                if (bookSource == null) {
                    continue
                }
                var existIndex: Int = -1
                for (i in 0 until bookSourceList.size()) {
                    var _bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
                    if (_bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)) {
                        existIndex = i
                        break
                    }
                }
                if (existIndex >= 0) {
                    var sourceList = bookSourceList.getList()
                    sourceList.set(existIndex, JsonObject.mapFrom(bookSource))
                    bookSourceList = JsonArray(sourceList)
                } else {
                    bookSourceList.add(JsonObject.mapFrom(bookSource))
                }
            }
        }

        saveUserStorage(userNameSpace, "bookSource", bookSourceList)
        if (userNameSpace.equals("admin") || userNameSpace.equals("default")) {
            saveUserStorage("default", "bookSource", bookSourceList)
        }
        return returnData.setData("")
    }

    suspend fun getBookSource(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        checkAuth(context)
        var bookSourceUrl: String
        if (context.request().method() == HttpMethod.POST) {
            // post 请求
            bookSourceUrl = context.bodyAsJson.getString("bookSourceUrl")
        } else {
            // get 请求
            bookSourceUrl = context.queryParam("bookSourceUrl").firstOrNull() ?: ""
        }
        if (bookSourceUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("书源链接不能为空")
        }

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        // 遍历判断书本是否存在
        var existIndex: Int = -1
        for (i in 0 until bookSourceList.size()) {
            var _bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
            if (_bookSource.bookSourceUrl.equals(bookSourceUrl)) {
                existIndex = i
                break;
            }
        }
        if (existIndex < 0) {
            return returnData.setErrorMsg("书源信息不存在")
        }

        return returnData.setData(bookSourceList.getJsonObject(existIndex).map)
    }

    suspend fun getBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        checkAuth(context)
        var simple: Int = 0
        if (context.request().method() == HttpMethod.POST) {
            // post 请求
            simple = context.bodyAsJson.getInteger("simple", 0)
        } else {
            // get 请求
            simple = context.queryParam("simple").firstOrNull()?.toInt() ?: 0
        }
        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList != null) {
            if (simple > 0) {
                var list = arrayListOf<Map<String, Any?>>()
                for (i in 0 until bookSourceList.size()) {
                    var bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
                    list.add(mapOf<String, Any?>(
                        "bookSourceGroup" to bookSource.bookSourceGroup,
                        "bookSourceName" to bookSource.bookSourceName,
                        "bookSourceUrl" to bookSource.bookSourceUrl,
                        "exploreUrl" to bookSource.exploreUrl
                    ))
                }
                return returnData.setData(list)
            }
            return returnData.setData(bookSourceList.getList())
        }
        return returnData.setData(arrayListOf<Int>())
    }

    suspend fun deleteBookSource(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val bookSource = context.bodyAsJson.mapTo(BookSource::class.java)

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace)
        if (bookSourceList == null) {
            bookSourceList = JsonArray()
        }
        // 遍历判断书本是否存在
        var existIndex: Int = -1
        for (i in 0 until bookSourceList.size()) {
            var _bookSource = bookSourceList.getJsonObject(i).mapTo(BookSource::class.java)
            if (_bookSource.bookSourceUrl.equals(bookSource.bookSourceUrl)) {
                existIndex = i
                break;
            }
        }
        if (existIndex >= 0) {
            bookSourceList.remove(existIndex)
        }

        // logger.info("bookSourceList: {}", bookSourceList)
        saveUserStorage(userNameSpace, "bookSource", bookSourceList)
        if (userNameSpace.equals("admin") || userNameSpace.equals("default")) {
            saveUserStorage("default", "bookSource", bookSourceList)
        }
        return returnData.setData("")
    }

    suspend fun deleteBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val bookSourceJsonArray = context.bodyAsJsonArray
            ?: (try { context.bodyAsJson?.getJsonArray("sources") } catch (e: Exception) { null })
            ?: (try { context.bodyAsJson?.let { JsonArray().add(it) } } catch (e: Exception) { null })
            ?: JsonArray()

        var userNameSpace = getUserNameSpace(context)
        var bookSourceList = getUserBookSourceJson(userNameSpace) ?: JsonArray()

        val urlsToDelete = HashSet<String>()
        for (k in 0 until bookSourceJsonArray.size()) {
            val item = bookSourceJsonArray.getValue(k)
            if (item is JsonObject) {
                val url = item.getString("bookSourceUrl") ?: item.getString("sourceUrl") ?: item.getString("url")
                if (!url.isNullOrEmpty()) urlsToDelete.add(url)
            } else if (item is String && item.isNotEmpty()) {
                urlsToDelete.add(item)
            }
        }

        val newBookSourceList = JsonArray()
        for (i in 0 until bookSourceList.size()) {
            val obj = bookSourceList.getJsonObject(i)
            val url = obj?.getString("bookSourceUrl") ?: ""
            if (!urlsToDelete.contains(url)) {
                newBookSourceList.add(obj)
            }
        }

        // 同步清理失效书源缓存中对应的条目
        try {
            val cacheDir = File(getWorkDir("storage", "cache", "invalidBookSourceCache", userNameSpace))
            val cache = ACache.get(cacheDir, 1000 * 1000 * 5L, 1000000)
            for (url in urlsToDelete) {
                cache.remove(url)
            }
        } catch (e: Exception) {
            logger.warn("清理失效书源缓存条目异常: {}", e.message)
        }

        saveUserStorage(userNameSpace, "bookSource", newBookSourceList)
        if (userNameSpace.equals("admin") || userNameSpace.equals("default")) {
            saveUserStorage("default", "bookSource", newBookSourceList)
        }
        return returnData.setData("")
    }

    suspend fun deleteAllBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        var userNameSpace = getUserNameSpace(context)
        saveUserStorage(userNameSpace, "bookSource", JsonArray())
        if (userNameSpace.equals("admin") || userNameSpace.equals("default")) {
            saveUserStorage("default", "bookSource", JsonArray())
        }
        return returnData.setData("")
    }

    suspend fun setAsDefaultBookSources(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        if (!checkManagerAuth(context)) {
            return returnData.setData("NEED_SECURE_KEY").setErrorMsg("请输入管理密码")
        }
        var username = context.bodyAsJson.getString("username")
        var bookSourceList: JsonArray? = asJsonArray(getUserStorage(username, "bookSource"))
        if (bookSourceList == null) {
            return returnData.setErrorMsg("用户书源不存在")
        }

        // 保存为默认书源
        saveUserStorage("default", "bookSource", bookSourceList.getList())
        return returnData.setData("设置默认书源成功")
    }

    suspend fun readSourceFile(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (context.fileUploads() == null || context.fileUploads().isEmpty()) {
            return returnData.setErrorMsg("请上传文件")
        }
        var sourceList = JsonArray()
        context.fileUploads().forEach {
            // logger.info("readSourceFile: {}", it.uploadedFileName())
            var file = File(it.uploadedFileName())
            if (file.exists()) {
                sourceList.add(file.readText())
                file.delete()
            }
        }
        return returnData.setData(sourceList.getList())
    }

    suspend fun readRemoteSourceFile(context: RoutingContext) {
        val returnData = ReturnData()
        var url: String
        if (context.request().method() == HttpMethod.POST) {
            // post 请求
            url = context.bodyAsJson.getString("url") ?: ""
        } else {
            // get 请求
            url = context.queryParam("url").firstOrNull() ?: ""
        }
        if (url.isNullOrEmpty()) {
            context.success(returnData.setErrorMsg("请输入远程书源链接"))
            return
        }

        launch(Dispatchers.IO) {
            webClient.getAbs(url).timeout(3000).send {
                var body = it.result()?.bodyAsString()
                if (body != null) {
                    context.success(returnData.setData(arrayListOf(body)))
                } else {
                    context.success(returnData.setErrorMsg("远程书源链接错误"))
                }
            }
        }
    }

    suspend fun deleteUserBookSource(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        if (!checkManagerAuth(context)) {
            return returnData.setData("NEED_SECURE_KEY").setErrorMsg("请输入管理密码")
        }
        val userJsonArray = context.bodyAsJsonArray
        for (i in 0 until userJsonArray.size()) {
            var username = userJsonArray.getString(i)
            var userBookSourceFile = File(getWorkDir("storage", "data", username, "bookSource.json"))
            // 删除用户书源文件，恢复默认书源
            if (userBookSourceFile.exists()) {
                userBookSourceFile.deleteRecursively()
            }
        }
        return returnData.setData("删除书源成功")
    }

    suspend fun deleteBookSourcesFile(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        var userNameSpace = getUserNameSpace(context)
        var userBookSourceFile = File(getWorkDir("storage", "data", userNameSpace, "bookSource.json"))
        // 删除用户书源文件，恢复默认书源
        if (userBookSourceFile.exists()) {
            userBookSourceFile.deleteRecursively()
        }
        return returnData.setData("")
    }

    suspend fun testBookSourceSpeed(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val url = if (context.request().method() == HttpMethod.POST) {
            context.bodyAsJson?.getString("url") ?: context.bodyAsJson?.getString("bookSourceUrl") ?: ""
        } else {
            context.queryParam("url").firstOrNull() ?: context.queryParam("bookSourceUrl").firstOrNull() ?: ""
        }
        if (url.isNullOrEmpty()) {
            return returnData.setErrorMsg("书源链接不能为空")
        }

        val start = System.currentTimeMillis()
        return try {
            withContext(Dispatchers.IO) {
                val client = okHttpClient.newBuilder()
                    .connectTimeout(4500, TimeUnit.MILLISECONDS)
                    .readTimeout(4500, TimeUnit.MILLISECONDS)
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
                client.newCall(request).execute().use { response ->
                    val cost = System.currentTimeMillis() - start
                    val code = response.code
                    if (code in 200..399) {
                        returnData.setData(mapOf("cost" to cost, "status" to "ok", "code" to code))
                    } else {
                        returnData.setErrorMsg("HTTP $code")
                            .setData(mapOf("cost" to cost, "status" to "error", "code" to code))
                    }
                }
            }
        } catch (e: Exception) {
            val cost = System.currentTimeMillis() - start
            returnData.setErrorMsg(e.message ?: "连接失败")
                .setData(mapOf("cost" to cost, "status" to "error"))
        }
    }

    suspend fun testBookSourcesSpeed(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val array = context.bodyAsJsonArray ?: JsonArray()
        val results = mutableListOf<Map<String, Any>>()
        val testClient = okHttpClient.newBuilder()
            .connectTimeout(4500, TimeUnit.MILLISECONDS)
            .readTimeout(4500, TimeUnit.MILLISECONDS)
            .build()
        withContext(Dispatchers.IO) {
            val jobs = (0 until array.size()).map { i ->
                async {
                    val item = array.getJsonObject(i)
                    val url = item.getString("bookSourceUrl") ?: item.getString("url") ?: ""
                    if (url.isEmpty()) {
                        mapOf("url" to url, "cost" to 9999L, "status" to "error", "code" to 400)
                    } else {
                        val start = System.currentTimeMillis()
                        try {
                            val request = Request.Builder()
                                .url(url)
                                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                .build()
                            testClient.newCall(request).execute().use { response ->
                                val cost = System.currentTimeMillis() - start
                                val code = response.code
                                val status = if (code in 200..399) "ok" else "error"
                                mapOf("url" to url, "cost" to cost, "status" to status, "code" to code)
                            }
                        } catch (e: Exception) {
                            val cost = System.currentTimeMillis() - start
                            mapOf("url" to url, "cost" to cost, "status" to "error", "errorMsg" to (e.message ?: "连接失败"))
                        }
                    }
                }
            }
            results.addAll(jobs.map { it.await() })
        }
        return returnData.setData(results)
    }
}
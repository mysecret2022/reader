package com.htmake.reader.api.controller

import com.htmake.reader.config.AppConfig
import com.htmake.reader.api.ReturnData
import com.htmake.reader.entity.User
import com.htmake.reader.utils.*
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.CoroutineContext

class CloudSyncController(override val coroutineContext: CoroutineContext) : BaseController(coroutineContext) {
    private val logger = KotlinLogging.logger {}

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * 获取云同步配置
     */
    suspend fun getCloudSyncConfig(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val userInfo = context.get("userInfo") as User?
        if (userInfo == null || (!userInfo.is_admin && !userInfo.username.equals("admin", ignoreCase = true))) {
            return returnData.setErrorMsg("只有管理员才能访问云同步配置")
        }

        val configJson = asJsonObject(getStorage("data", "cloudSyncConfig")) ?: JsonObject()
        val config = mutableMapOf<String, Any>(
            "enabled" to (configJson.getBoolean("enabled") ?: false),
            "target" to (configJson.getString("target") ?: "both"), // github, webdav, both
            "incrementalIntervalMinutes" to (configJson.getInteger("incrementalIntervalMinutes") ?: 30),
            "fullBackupIntervalHours" to (configJson.getInteger("fullBackupIntervalHours") ?: 24),
            "githubOwner" to (configJson.getString("githubOwner") ?: ""),
            "githubRepo" to (configJson.getString("githubRepo") ?: ""),
            "githubBranch" to (configJson.getString("githubBranch") ?: "main"),
            "githubPath" to (configJson.getString("githubPath") ?: "reader-backup"),
            "githubToken" to (configJson.getString("githubToken") ?: ""),
            "encryptionPassword" to (configJson.getString("encryptionPassword") ?: ""),
            "webdavUrl" to (configJson.getString("webdavUrl") ?: ""),
            "webdavUsername" to (configJson.getString("webdavUsername") ?: ""),
            "webdavPassword" to (configJson.getString("webdavPassword") ?: ""),
            "webdavRemotePath" to (configJson.getString("webdavRemotePath") ?: "/web"),
            "lastIncrementalSyncTime" to (configJson.getString("lastIncrementalSyncTime") ?: ""),
            "lastIncrementalStatus" to (configJson.getString("lastIncrementalStatus") ?: "暂无记录"),
            "lastFullBackupTime" to (configJson.getString("lastFullBackupTime") ?: ""),
            "lastFullStatus" to (configJson.getString("lastFullStatus") ?: "暂无记录")
        )

        return returnData.setData(config)
    }

    /**
     * 保存云同步配置
     */
    suspend fun saveCloudSyncConfig(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val userInfo = context.get("userInfo") as User?
        if (userInfo == null || (!userInfo.is_admin && !userInfo.username.equals("admin", ignoreCase = true))) {
            return returnData.setErrorMsg("只有管理员才能修改云同步配置")
        }

        val body = context.bodyAsJson ?: JsonObject()
        val oldConfigJson = asJsonObject(getStorage("data", "cloudSyncConfig")) ?: JsonObject()

        val newConfig = mutableMapOf<String, Any>(
            "enabled" to (body.getBoolean("enabled") ?: false),
            "target" to (body.getString("target") ?: "both"),
            "incrementalIntervalMinutes" to (body.getInteger("incrementalIntervalMinutes") ?: 30),
            "fullBackupIntervalHours" to (body.getInteger("fullBackupIntervalHours") ?: 24),
            "githubOwner" to (body.getString("githubOwner") ?: "").trim(),
            "githubRepo" to (body.getString("githubRepo") ?: "").trim(),
            "githubBranch" to (body.getString("githubBranch") ?: "main").trim(),
            "githubPath" to (body.getString("githubPath") ?: "reader-backup").trim().trim('/'),
            "githubToken" to (body.getString("githubToken") ?: "").trim(),
            "encryptionPassword" to (body.getString("encryptionPassword") ?: "").trim(),
            "webdavUrl" to (body.getString("webdavUrl") ?: "").trim().trimEnd('/'),
            "webdavUsername" to (body.getString("webdavUsername") ?: "").trim(),
            "webdavPassword" to (body.getString("webdavPassword") ?: "").trim(),
            "webdavRemotePath" to (body.getString("webdavRemotePath") ?: "/web").trim(),
            "lastIncrementalSyncTime" to (oldConfigJson.getString("lastIncrementalSyncTime") ?: ""),
            "lastIncrementalStatus" to (oldConfigJson.getString("lastIncrementalStatus") ?: "暂无记录"),
            "lastFullBackupTime" to (oldConfigJson.getString("lastFullBackupTime") ?: ""),
            "lastFullStatus" to (oldConfigJson.getString("lastFullStatus") ?: "暂无记录")
        )

        saveStorage("data", "cloudSyncConfig", value = newConfig)
        return returnData.setData(newConfig)
    }

    /**
     * 手动/定时触发增量同步
     * 同步当前所有关键数据文件 (users.json, systemConfig.json, 用户的书源、书架、规则等)
     */
    suspend fun triggerCloudSync(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val userInfo = context.get("userInfo") as User?
        if (userInfo == null || (!userInfo.is_admin && !userInfo.username.equals("admin", ignoreCase = true))) {
            return returnData.setErrorMsg("无权执行云同步")
        }

        val resultMsg = executeIncrementalSync()
        return returnData.setData(resultMsg)
    }

    /**
     * 手动/定时触发全量打包备份
     * 将整个 storage/data 目录打包成带时间戳的 zip 归档，并上传到 GitHub / WebDAV
     */
    suspend fun triggerCloudBackup(context: RoutingContext): ReturnData {
        val returnData = ReturnData()
        if (!checkAuth(context)) {
            return returnData.setData("NEED_LOGIN").setErrorMsg("请登录后使用")
        }
        val userInfo = context.get("userInfo") as User?
        if (userInfo == null || (!userInfo.is_admin && !userInfo.username.equals("admin", ignoreCase = true))) {
            return returnData.setErrorMsg("无权执行全量备份")
        }

        val resultMsg = executeFullBackup()
        return returnData.setData(resultMsg)
    }

    /**
     * 执行增量同步的核心方法
     */
    suspend fun executeIncrementalSync(): String = withContext(Dispatchers.IO) {
        val configJson = asJsonObject(getStorage("data", "cloudSyncConfig"))
        if (configJson == null) {
            return@withContext "未配置云同步参数"
        }
        val target = configJson.getString("target") ?: "both"
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val details = mutableListOf<String>()

        try {
            // 收集所有需要增量同步的数据文件 (相对 storage/data 路径)
            val dataDir = File(getWorkDir("storage", "data"))
            if (!dataDir.exists()) {
                return@withContext "存储数据目录不存在"
            }

            val syncFiles = mutableMapOf<String, ByteArray>()
            fun collectFiles(dir: File, baseRelative: String) {
                dir.listFiles()?.forEach { file ->
                    val relPath = if (baseRelative.isEmpty()) file.name else "$baseRelative/${file.name}"
                    if (file.isDirectory) {
                        // 忽略临时缓存目录
                        if (file.name != "tmp" && file.name != "cache") {
                            collectFiles(file, relPath)
                        }
                    } else if (file.isFile) {
                        // 增量同步只同步 JSON 配置数据与规则，避免大二进制文件导致阻塞
                        if (file.name.endsWith(".json")) {
                            syncFiles[relPath] = file.readBytes()
                        }
                    }
                }
            }
            collectFiles(dataDir, "")

            if (syncFiles.isEmpty()) {
                return@withContext "没有找到需要增量同步的配置文件"
            }

            // 同步到 GitHub (强制加密以保障数据安全)
            if (target == "github" || target == "both") {
                val token = configJson.getString("githubToken") ?: ""
                val owner = configJson.getString("githubOwner") ?: ""
                val repo = configJson.getString("githubRepo") ?: ""
                val branch = configJson.getString("githubBranch") ?: "main"
                val basePath = (configJson.getString("githubPath") ?: "reader-backup").trim('/')
                val encPassword = configJson.getString("encryptionPassword") ?: ""

                if (token.isNotEmpty() && owner.isNotEmpty() && repo.isNotEmpty()) {
                    if (encPassword.isEmpty()) {
                        details.add("GitHub增量同步中止: 必须配置备份加密密码(防止公开泄露)")
                    } else {
                        var ghSuccess = 0
                        var ghFail = 0
                        var authFailed = false
                        for ((relPath, bytes) in syncFiles) {
                            val encryptedBytes = encryptBytes(bytes, encPassword)
                            val remoteFilePath = if (basePath.isEmpty()) "sync/${relPath}.enc" else "$basePath/sync/${relPath}.enc"
                            val code = uploadFileToGitHub(owner, repo, branch, remoteFilePath, encryptedBytes, "Encrypted incremental sync: $relPath", token)
                            if (code == 200 || code == 201) {
                                ghSuccess++
                            } else {
                                ghFail++
                                if (code == 401 || code == 403) {
                                    authFailed = true
                                    break
                                }
                            }
                        }
                        if (authFailed) {
                            details.add("GitHub增量同步失败: Token认证未通过(HTTP 401/403)")
                        } else {
                            details.add("GitHub增量同步(AES-256已加密): 成功 ${ghSuccess} 个, 失败 ${ghFail} 个")
                        }
                    }
                } else {
                    details.add("GitHub增量同步跳过: 配置不完整")
                }
            }

            // 同步到 WebDAV
            if (target == "webdav" || target == "both") {
                val url = (configJson.getString("webdavUrl") ?: "").trimEnd('/')
                val user = configJson.getString("webdavUsername") ?: ""
                val pass = configJson.getString("webdavPassword") ?: ""
                val remoteDir = configJson.getString("webdavRemotePath") ?: "/web"

                if (url.isNotEmpty() && user.isNotEmpty()) {
                    var wdSuccess = 0
                    var wdFail = 0
                    syncFiles.forEach { (relPath, bytes) ->
                        val fullRemoteUrl = "$url$remoteDir/sync/$relPath"
                        val ok = uploadFileToWebDAV(fullRemoteUrl, bytes, user, pass)
                        if (ok) wdSuccess++ else wdFail++
                    }
                    details.add("WebDAV增量同步: 成功 ${wdSuccess} 个, 失败 ${wdFail} 个")
                } else {
                    details.add("WebDAV增量同步跳过: 配置不完整")
                }
            }

            val finalStatus = details.joinToString("; ")
            configJson.put("lastIncrementalSyncTime", nowStr)
            configJson.put("lastIncrementalStatus", finalStatus)
            saveStorage("data", "cloudSyncConfig", value = configJson.map)
            return@withContext finalStatus
        } catch (e: Exception) {
            logger.error("executeIncrementalSync error", e)
            val errStatus = "失败: ${e.message}"
            configJson.put("lastIncrementalSyncTime", nowStr)
            configJson.put("lastIncrementalStatus", errStatus)
            saveStorage("data", "cloudSyncConfig", value = configJson.map)
            return@withContext errStatus
        }
    }

    /**
     * 执行全量备份的核心方法 (打包 storage/data 全目录为 ZIP 并上传)
     */
    suspend fun executeFullBackup(): String = withContext(Dispatchers.IO) {
        val configJson = asJsonObject(getStorage("data", "cloudSyncConfig"))
        if (configJson == null) {
            return@withContext "未配置云同步参数"
        }
        val target = configJson.getString("target") ?: "both"
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val backupFileName = "reader_full_backup_$dateStamp.zip"
        val details = mutableListOf<String>()

        try {
            val dataDir = File(getWorkDir("storage", "data"))
            if (!dataDir.exists()) {
                return@withContext "存储数据目录不存在"
            }

            // 递归压缩整个 storage/data 目录到内存中
            val zipBytes = zipDirectoryToBytes(dataDir)
            if (zipBytes.isEmpty()) {
                return@withContext "压缩备份失败，文件为空"
            }

            // 上传到 GitHub (强制加密以保障数据安全)
            if (target == "github" || target == "both") {
                val token = configJson.getString("githubToken") ?: ""
                val owner = configJson.getString("githubOwner") ?: ""
                val repo = configJson.getString("githubRepo") ?: ""
                val branch = configJson.getString("githubBranch") ?: "main"
                val basePath = (configJson.getString("githubPath") ?: "reader-backup").trim('/')
                val encPassword = configJson.getString("encryptionPassword") ?: ""

                if (token.isNotEmpty() && owner.isNotEmpty() && repo.isNotEmpty()) {
                    if (encPassword.isEmpty()) {
                        details.add("GitHub全量备份中止: 必须配置备份加密密码(防止公开泄露)")
                    } else {
                        val encryptedBytes = encryptBytes(zipBytes, encPassword)
                        val encryptedFileName = backupFileName.replace(".zip", ".enc")
                        val remotePath = if (basePath.isEmpty()) "backups/$encryptedFileName" else "$basePath/backups/$encryptedFileName"
                        val code = uploadFileToGitHub(owner, repo, branch, remotePath, encryptedBytes, "Encrypted full site backup: $encryptedFileName", token)
                        if (code == 200 || code == 201) {
                            details.add("GitHub全量备份(AES-256已加密): 成功 ($encryptedFileName, ${(encryptedBytes.size / 1024)} KB)")
                        } else if (code == 401 || code == 403) {
                            details.add("GitHub全量备份失败: Token认证未通过(HTTP 401/403)")
                        } else {
                            details.add("GitHub全量备份: 上传失败(HTTP $code)")
                        }
                    }
                } else {
                    details.add("GitHub全量备份跳过: 配置不完整")
                }
            }

            // 上传到 WebDAV
            if (target == "webdav" || target == "both") {
                val url = (configJson.getString("webdavUrl") ?: "").trimEnd('/')
                val user = configJson.getString("webdavUsername") ?: ""
                val pass = configJson.getString("webdavPassword") ?: ""
                val remoteDir = configJson.getString("webdavRemotePath") ?: "/web"

                if (url.isNotEmpty() && user.isNotEmpty()) {
                    val fullRemoteUrl = "$url$remoteDir/backups/$backupFileName"
                    val ok = uploadFileToWebDAV(fullRemoteUrl, zipBytes, user, pass)
                    if (ok) {
                        details.add("WebDAV全量备份: 成功 ($backupFileName, ${(zipBytes.size / 1024)} KB)")
                    } else {
                        details.add("WebDAV全量备份: 上传失败")
                    }
                } else {
                    details.add("WebDAV全量备份跳过: 配置不完整")
                }
            }

            val finalStatus = details.joinToString("; ")
            configJson.put("lastFullBackupTime", nowStr)
            configJson.put("lastFullStatus", finalStatus)
            saveStorage("data", "cloudSyncConfig", value = configJson.map)
            return@withContext finalStatus
        } catch (e: Exception) {
            logger.error("executeFullBackup error", e)
            val errStatus = "全量备份失败: ${e.message}"
            configJson.put("lastFullBackupTime", nowStr)
            configJson.put("lastFullStatus", errStatus)
            saveStorage("data", "cloudSyncConfig", value = configJson.map)
            return@withContext errStatus
        }
    }

    /**
     * 上传文件到 GitHub 仓库 (REST API: PUT /repos/{owner}/{repo}/contents/{path})
     */
    private fun uploadFileToGitHub(owner: String, repo: String, branch: String, path: String, content: ByteArray, message: String, token: String): Int {
        try {
            val getUrl = "https://api.github.com/repos/$owner/$repo/contents/$path?ref=$branch"
            val getReq = Request.Builder()
                .url(getUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Reader-CloudSync/1.0.1")
                .build()

            var sha: String? = null
            httpClient.newCall(getReq).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrEmpty()) {
                        val json = JsonObject(bodyStr)
                        sha = json.getString("sha")
                    }
                }
            }

            val putUrl = "https://api.github.com/repos/$owner/$repo/contents/$path"
            val base64Content = Base64.getEncoder().encodeToString(content)
            val reqPayload = JsonObject()
                .put("message", message)
                .put("content", base64Content)
                .put("branch", branch)

            if (sha != null) {
                reqPayload.put("sha", sha)
            }

            val putReq = Request.Builder()
                .url(putUrl)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Reader-CloudSync/1.0.1")
                .put(reqPayload.encode().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(putReq).execute().use { response ->
                if (response.isSuccessful || response.code == 201 || response.code == 200) {
                    return response.code
                } else {
                    logger.warn("GitHub upload failed: code={}, body={}", response.code, response.body?.string())
                    return response.code
                }
            }
        } catch (e: Exception) {
            logger.error("uploadFileToGitHub exception", e)
            return -1
        }
    }

    /**
     * 上传文件到 WebDAV (HTTP PUT, 自动处理目录 MKCOL)
     */
    fun uploadFileToWebDAV(url: String, content: ByteArray, username: String, pass: String): Boolean {
        try {
            val credential = Credentials.basic(username, pass)

            // 先尝试递归确保父级目录存在
            val uri = java.net.URI(url)
            val parentPath = uri.path.substringBeforeLast('/')
            if (parentPath.isNotEmpty() && parentPath != "/") {
                val segments = parentPath.split('/').filter { it.isNotEmpty() }
                var currentPath = ""
                for (seg in segments) {
                    currentPath += "/$seg"
                    val mkcolUrl = "${uri.scheme}://${uri.authority}$currentPath/"
                    val mkcolReq = Request.Builder()
                        .url(mkcolUrl)
                        .header("Authorization", credential)
                        .method("MKCOL", null)
                        .build()
                    try {
                        httpClient.newCall(mkcolReq).execute().close()
                    } catch (_: Exception) {}
                }
            }

            val putReq = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .put(content.toRequestBody("application/octet-stream".toMediaType()))
                .build()

            httpClient.newCall(putReq).execute().use { response ->
                return response.isSuccessful || response.code in 200..204
            }
        } catch (e: Exception) {
            logger.error("uploadFileToWebDAV exception", e)
            return false
        }
    }

    /**
     * 将整个目录递归打成内存 ZIP 字节流
     */
    private fun zipDirectoryToBytes(directory: File): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            fun addFileToZip(file: File, parentPath: String) {
                val entryPath = if (parentPath.isEmpty()) file.name else "$parentPath/${file.name}"
                if (file.isDirectory) {
                    // 严格白名单与排除：排除缓存、日志、TTS音频、章节正文小文件目录，防止内存暴增与 OOM
                    val name = file.name.lowercase()
                    if (name == "tmp" || name == "cache" || name == "logs" || name == "tts" || 
                        name == "bookinfocache" || name == "invalidbooksourcelache" || name.contains("_")) {
                        return
                    }
                    file.listFiles()?.forEach { child ->
                        addFileToZip(child, entryPath)
                    }
                } else if (file.isFile) {
                    // 仅备份配置与规则 JSON，忽略超大二进制文件与零散缓存
                    if (!file.name.endsWith(".json", ignoreCase = true)) return
                    if (file.length() > 10 * 1024 * 1024) return
                    val entry = ZipEntry(entryPath)
                    zos.putNextEntry(entry)
                    FileInputStream(file).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
            directory.listFiles()?.forEach { file ->
                addFileToZip(file, "")
            }
        }
        return baos.toByteArray()
    }

    /**
     * 对二进制数据采用 AES-256-GCM 强加密 (包含 12 字节随机 Nonce，前缀拼接)
     */
    private fun encryptBytes(plainBytes: ByteArray, secret: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(secret.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val nonce = ByteArray(12)
        SecureRandom().nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, nonce))
        val cipherText = cipher.doFinal(plainBytes)

        val combined = ByteArray(nonce.size + cipherText.size)
        System.arraycopy(nonce, 0, combined, 0, nonce.size)
        System.arraycopy(cipherText, 0, combined, nonce.size, cipherText.size)
        return combined
    }
}

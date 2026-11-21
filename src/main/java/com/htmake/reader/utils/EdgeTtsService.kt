package com.htmake.reader.utils

import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * 内置微软 Edge TTS 神经语音合成服务
 * 直接通过 WebSocket 连接微软在线语音接口生成 MP3 音频流并落盘缓存，无需外部 8089 端口代理。
 */
object EdgeTtsService {

    private val logger = KotlinLogging.logger {}

    private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
    private const val SEC_MS_GEC_VERSION = "1-$CHROMIUM_FULL_VERSION"
    private const val WIN_EPOCH_OFFSET_SECONDS = 11644473600L
    private const val MAX_CACHE_FILES = 300
    private const val MAX_TEXT_LENGTH = 1500

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 合成指定文本的 MP3 音频，优先命中磁盘缓存
     */
    fun synthesize(
        text: String,
        voice: String = "zh-CN-XiaoxiaoNeural",
        rate: Double = 1.0,
        pitch: Double = 1.0,
        cacheDir: File
    ): ByteArray {
        val cleanText = text.trim().let {
            if (it.length > MAX_TEXT_LENGTH) it.substring(0, MAX_TEXT_LENGTH) else it
        }
        if (cleanText.isEmpty()) {
            return ByteArray(0)
        }
        val safeVoice = if (voice.matches(Regex("^[a-zA-Z0-9-]+$"))) voice else "zh-CN-XiaoxiaoNeural"
        val safeRate = rate.coerceIn(0.5, 2.5)
        val safePitch = pitch.coerceIn(0.5, 2.0)

        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val cacheKey = sha256Hex("$safeVoice|$safeRate|$safePitch|$cleanText")
        val cacheFile = File(cacheDir, "$cacheKey.mp3")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            cacheFile.setLastModified(System.currentTimeMillis())
            return cacheFile.readBytes()
        }

        val audioBytes = fetchFromEdgeWs(cleanText, safeVoice, safeRate, safePitch)
        if (audioBytes.isNotEmpty()) {
            val tmpFile = File(cacheDir, "$cacheKey.tmp.${UUID.randomUUID()}")
            try {
                tmpFile.writeBytes(audioBytes)
                if (!tmpFile.renameTo(cacheFile)) {
                    cacheFile.writeBytes(audioBytes)
                    tmpFile.delete()
                }
                pruneCacheIfNeeded(cacheDir)
            } catch (e: Exception) {
                tmpFile.delete()
                logger.warn("EdgeTTS write cache error: ${e.message}")
            }
        }
        return audioBytes
    }

    private fun fetchFromEdgeWs(
        text: String,
        voice: String,
        rate: Double,
        pitch: Double
    ): ByteArray {
        val gec = generateSecMsGec()
        val connId = UUID.randomUUID().toString().replace("-", "")
        val muid = UUID.randomUUID().toString().replace("-", "").uppercase(Locale.US)
        val wsUrl = "wss://api.msedgeservices.com/tts/cognitiveservices/websocket/v1" +
            "?Ocp-Apim-Subscription-Key=$TRUSTED_CLIENT_TOKEN" +
            "&Sec-MS-GEC=$gec" +
            "&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION" +
            "&ConnectionId=$connId"

        val request = Request.Builder()
            .url(wsUrl)
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Pragma", "no-cache")
            .header("Cache-Control", "no-cache")
            .header("Cookie", "muid=$muid;")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"
            )
            .build()

        val resultFuture = CompletableFuture<ByteArray>()
        val audioBuffer = ByteArrayOutputStream()
        val ratePercent = Math.round((rate - 1.0) * 100.0).toInt()
        val rateStr = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"
        val pitchPercent = Math.round((pitch - 1.0) * 50.0).toInt()
        val pitchStr = if (pitchPercent >= 0) "+$pitchPercent%" else "$pitchPercent%"
        val requestId = UUID.randomUUID().toString().replace("-", "")
        val timestamp = formatEdgeTimestamp()
        val escapedText = escapeXml(text)

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val configMsg = "X-Timestamp:$timestamp\r\n" +
                    "Content-Type:application/json; charset=utf-8\r\n" +
                    "Path:speech.config\r\n\r\n" +
                    """{"context":{"synthesis":{"audio":{"metadataoptions":{"sentenceBoundaryEnabled":false,"wordBoundaryEnabled":false},"outputFormat":"audio-24khz-48kbitrate-mono-mp3"}}}}"""
                webSocket.send(configMsg)

                val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='zh-CN'>" +
                    "<voice name='$voice'>" +
                    "<prosody pitch='$pitchStr' rate='$rateStr'>$escapedText</prosody>" +
                    "</voice></speak>"
                val ssmlMsg = "X-RequestId:$requestId\r\n" +
                    "Content-Type:application/ssml+xml\r\n" +
                    "X-Timestamp:${timestamp}Z\r\n" +
                    "Path:ssml\r\n\r\n$ssml"
                webSocket.send(ssmlMsg)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (bytes.size < 2) return
                val headerLen = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
                val headerEnd = 2 + headerLen
                if (bytes.size >= headerEnd) {
                    val headerStr = bytes.substring(2, headerEnd).utf8()
                    if (headerStr.contains("Path:audio") && bytes.size > headerEnd) {
                        val audioSlice = bytes.substring(headerEnd).toByteArray()
                        synchronized(audioBuffer) {
                            audioBuffer.write(audioSlice)
                        }
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("Path:turn.end")) {
                    val data = synchronized(audioBuffer) { audioBuffer.toByteArray() }
                    resultFuture.complete(data)
                    webSocket.close(1000, "done")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!resultFuture.isDone) {
                    resultFuture.completeExceptionally(
                        RuntimeException("EdgeTTS ws failed (${response?.code}): ${t.message}", t)
                    )
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!resultFuture.isDone) {
                    val data = synchronized(audioBuffer) { audioBuffer.toByteArray() }
                    if (data.isNotEmpty()) {
                        resultFuture.complete(data)
                    } else {
                        resultFuture.completeExceptionally(
                            RuntimeException("EdgeTTS ws closed without audio: $code $reason")
                        )
                    }
                }
            }
        })

        return try {
            resultFuture.get(35, TimeUnit.SECONDS)
        } catch (e: Exception) {
            ws.cancel()
            throw e
        }
    }

    private fun generateSecMsGec(): String {
        val currentUnixSeconds = System.currentTimeMillis() / 1000L
        val ticksSeconds = ((currentUnixSeconds + WIN_EPOCH_OFFSET_SECONDS) / 300L) * 300L
        val ticks = ticksSeconds * 10_000_000L
        return sha256Hex("${ticks}$TRUSTED_CLIENT_TOKEN").uppercase(Locale.US)
    }

    private fun formatEdgeTimestamp(): String {
        val sdf = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z '(Coordinated Universal Time)'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun escapeXml(text: String): String {
        val sb = StringBuilder(text.length + 16)
        for (ch in text) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    // 过滤非法 XML 控制字符
                    if (ch.code == 0x9 || ch.code == 0xA || ch.code == 0xD || ch.code >= 0x20) {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun pruneCacheIfNeeded(cacheDir: File) {
        val files = cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".mp3") } ?: return
        if (files.size <= MAX_CACHE_FILES) return
        files.sortBy { it.lastModified() }
        val toDelete = files.size - MAX_CACHE_FILES
        for (i in 0 until toDelete) {
            files[i].delete()
        }
    }
}

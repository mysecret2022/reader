package com.htmake.reader

import com.htmake.reader.utils.EdgeTtsService
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class EdgeTtsServiceTest {

    @Test
    fun testSynthesizeAndCache() {
        val tempDir: File = Files.createTempDirectory("edge-tts-test").toFile()
        try {
            val bytes = EdgeTtsService.synthesize(
                text = "你好，测试锁屏听书语音合成。",
                voice = "zh-CN-XiaoxiaoNeural",
                rate = 1.0,
                pitch = 1.0,
                cacheDir = tempDir
            )
            assertTrue("Expected valid MP3 audio bytes, got ${bytes.size}", bytes.size > 500)
            // Verify second call hits disk cache identically
            val cachedBytes = EdgeTtsService.synthesize(
                text = "你好，测试锁屏听书语音合成。",
                voice = "zh-CN-XiaoxiaoNeural",
                rate = 1.0,
                pitch = 1.0,
                cacheDir = tempDir
            )
            assertArrayEquals(bytes, cachedBytes)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

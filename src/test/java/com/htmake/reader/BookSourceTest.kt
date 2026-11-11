package com.htmake.reader

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookSourceTest {

    @Test
    fun testTitleFuzzyMatching() {
        fun clean(str: String?): String {
            return (str ?: "")
                .replace(Regex("[（(【\\[].*?[）)\\]】]"), "")
                .replace(Regex("[\\s\\-_—·、:：，,。.!！?？]"), "")
                .lowercase()
        }

        fun extractNum(str: String?): String? {
            val m = Regex("(?:第\\s*([0-9零一二两三四五六七八九十百千万]+)\\s*[章回节集])|(?:^([0-9]+)[.\\s、])").find(str ?: "")
            return m?.groups?.get(1)?.value ?: m?.groups?.get(2)?.value
        }

        val title1 = "第123章 精彩决战 (求月票)"
        val title2 = "第123章 精彩决战"
        val title3 = "第123章 精彩决战【加更】"
        val title4 = "123. 精彩决战"

        assertEquals(clean(title1), clean(title2))
        assertEquals(clean(title2), clean(title3))
        assertEquals("123", extractNum(title1))
        assertEquals("123", extractNum(title2))
        assertEquals("123", extractNum(title4))
    }

    @Test
    fun testSpeedBucketing() {
        fun getStatus(cost: Long, success: Boolean): String {
            return if (success) {
                when {
                    cost < 1500 -> "fast"
                    cost <= 4000 -> "slow"
                    else -> "failed"
                }
            } else {
                "failed"
            }
        }

        assertEquals("fast", getStatus(350, true))
        assertEquals("fast", getStatus(1499, true))
        assertEquals("slow", getStatus(1500, true))
        assertEquals("slow", getStatus(2800, true))
        assertEquals("slow", getStatus(4000, true))
        assertEquals("failed", getStatus(4001, true))
        assertEquals("failed", getStatus(6000, true))
        assertEquals("failed", getStatus(500, false))
    }

    @Test
    fun testBatchDeleteJsonParsing() {
        val jsonArray = JsonArray()
            .add(JsonObject().put("bookSourceUrl", "https://source1.com"))
            .add(JsonObject().put("sourceUrl", "https://source2.com"))
            .add("https://source3.com")

        val urlsToDelete = HashSet<String>()
        for (k in 0 until jsonArray.size()) {
            val item = jsonArray.getValue(k)
            if (item is JsonObject) {
                val url = item.getString("bookSourceUrl") ?: item.getString("sourceUrl")
                if (!url.isNullOrEmpty()) urlsToDelete.add(url)
            } else if (item is String && item.isNotEmpty()) {
                urlsToDelete.add(item)
            }
        }

        assertEquals(3, urlsToDelete.size)
        assertTrue(urlsToDelete.contains("https://source1.com"))
        assertTrue(urlsToDelete.contains("https://source2.com"))
        assertTrue(urlsToDelete.contains("https://source3.com"))
        assertFalse(urlsToDelete.contains("https://source4.com"))
    }
}

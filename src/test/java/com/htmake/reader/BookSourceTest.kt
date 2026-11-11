package com.htmake.reader

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookSourceTest {

    private fun parseChapterNumber(str: String?): Int? {
        if (str.isNullOrBlank()) return null
        val m = Regex("(?:第\\s*([0-9零一二两三四五六七八九十百千万]+)\\s*[章回节集])|(?:^([0-9]+)[.\\s、])").find(str)
            ?: return null
        val raw = m.groups[1]?.value ?: m.groups[2]?.value ?: return null
        if (raw.all { it.isDigit() }) {
            return raw.toIntOrNull()
        }
        val digits = mapOf(
            '零' to 0, '〇' to 0, '一' to 1, '二' to 2, '两' to 2,
            '三' to 3, '四' to 4, '五' to 5, '六' to 6, '七' to 7,
            '八' to 8, '九' to 9
        )
        var total = 0
        var section = 0
        var num = 0
        for (char in raw) {
            when {
                digits.containsKey(char) -> num = digits[char]!!
                char == '十' -> {
                    if (num == 0) num = 1
                    section += num * 10
                    num = 0
                }
                char == '百' -> {
                    section += (if (num == 0) 1 else num) * 100
                    num = 0
                }
                char == '千' -> {
                    section += (if (num == 0) 1 else num) * 1000
                    num = 0
                }
                char == '万' -> {
                    section += num
                    total += (if (section == 0) 1 else section) * 10000
                    section = 0
                    num = 0
                }
            }
        }
        return total + section + num
    }

    @Test
    fun testTitleFuzzyMatching() {
        fun clean(str: String?): String {
            return (str ?: "")
                .replace(Regex("[（(【\\[].*?[）)\\]】]"), "")
                .replace(Regex("[\\s\\-_—·、:：，,。.!！?？]"), "")
                .lowercase()
        }

        val title1 = "第123章 精彩决战 (求月票)"
        val title2 = "第123章 精彩决战"
        val title3 = "第123章 精彩决战【加更】"
        val title4 = "123. 精彩决战"
        val titleChinese = "第一百二十三章 精彩决战"

        assertEquals(clean(title1), clean(title2))
        assertEquals(clean(title2), clean(title3))
        assertEquals(123, parseChapterNumber(title1))
        assertEquals(123, parseChapterNumber(title2))
        assertEquals(123, parseChapterNumber(title4))
        assertEquals(123, parseChapterNumber(titleChinese))
        assertEquals(parseChapterNumber(title1), parseChapterNumber(titleChinese))
        assertEquals(1005, parseChapterNumber("第一千零五章 大结局"))
        assertEquals(21, parseChapterNumber("第二十一章 暗潮涌动"))
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
            .add(JsonObject().put("url", "https://source3.com"))
            .add("https://source4.com")

        val urlsToDelete = HashSet<String>()
        for (k in 0 until jsonArray.size()) {
            val item = jsonArray.getValue(k)
            if (item is JsonObject) {
                val url = item.getString("bookSourceUrl") ?: item.getString("sourceUrl") ?: item.getString("url")
                if (!url.isNullOrEmpty()) urlsToDelete.add(url)
            } else if (item is String && item.isNotEmpty()) {
                urlsToDelete.add(item)
            }
        }

        assertEquals(4, urlsToDelete.size)
        assertTrue(urlsToDelete.contains("https://source1.com"))
        assertTrue(urlsToDelete.contains("https://source2.com"))
        assertTrue(urlsToDelete.contains("https://source3.com"))
        assertTrue(urlsToDelete.contains("https://source4.com"))
        assertFalse(urlsToDelete.contains("https://source5.com"))
    }

    @Test
    fun testReorderingLogic() {
        val existingList = JsonArray()
            .add(JsonObject().put("bookSourceUrl", "https://sourceA.com").put("bookSourceName", "A"))
            .add(JsonObject().put("bookSourceUrl", "https://sourceB.com").put("bookSourceName", "B"))
            .add(JsonObject().put("bookSourceUrl", "https://sourceC.com").put("bookSourceName", "C"))

        val reorderedInput = JsonArray()
            .add(JsonObject().put("bookSourceUrl", "https://sourceC.com").put("bookSourceName", "C"))
            .add(JsonObject().put("bookSourceUrl", "https://sourceA.com").put("bookSourceName", "A"))

        val finalBookSourceList = JsonArray()
        val seenUrls = HashSet<String>()
        for (k in 0 until reorderedInput.size()) {
            val item = reorderedInput.getJsonObject(k) ?: continue
            val url = item.getString("bookSourceUrl") ?: continue
            if (seenUrls.add(url)) {
                finalBookSourceList.add(item)
            }
        }
        for (i in 0 until existingList.size()) {
            val item = existingList.getJsonObject(i)
            val url = item?.getString("bookSourceUrl") ?: ""
            if (url.isNotEmpty() && !seenUrls.contains(url)) {
                finalBookSourceList.add(item)
            }
        }

        assertEquals(3, finalBookSourceList.size())
        assertEquals("https://sourceC.com", finalBookSourceList.getJsonObject(0).getString("bookSourceUrl"))
        assertEquals("https://sourceA.com", finalBookSourceList.getJsonObject(1).getString("bookSourceUrl"))
        assertEquals("https://sourceB.com", finalBookSourceList.getJsonObject(2).getString("bookSourceUrl"))
    }

    @Test
    fun testUserDataIsolationOnEmptySources() {
        val mockStorage = mutableMapOf<String, String>()
        mockStorage["admin/bookSource"] = JsonArray().add(JsonObject().put("bookSourceUrl", "https://default.com")).encode()

        fun resolveUserSources(userNameSpace: String): JsonArray {
            val rawStorage = mockStorage["$userNameSpace/bookSource"]
            var list: JsonArray? = if (rawStorage != null) JsonArray(rawStorage) else null
            if (rawStorage == null && userNameSpace != "admin") {
                val adminRaw = mockStorage["admin/bookSource"]
                if (adminRaw != null) {
                    val adminList = JsonArray(adminRaw)
                    mockStorage["$userNameSpace/bookSource"] = adminList.encode()
                    list = adminList
                }
            }
            return list ?: JsonArray()
        }

        // 1. Initial access: user1 has no file yet, should initialize from admin
        val user1Initial = resolveUserSources("user1")
        assertEquals(1, user1Initial.size())
        assertEquals("https://default.com", user1Initial.getJsonObject(0).getString("bookSourceUrl"))

        // 2. User1 deletes all sources (saves empty array)
        mockStorage["user1/bookSource"] = JsonArray().encode()

        // 3. Subsequent access: should return empty list, NOT re-populate from admin!
        val user1AfterDelete = resolveUserSources("user1")
        assertEquals(0, user1AfterDelete.size())

        // 4. Admin sources remain untouched
        val adminSources = resolveUserSources("admin")
        assertEquals(1, adminSources.size())
    }

    @Test
    fun testUserSaveIsolation() {
        val mockStorage = mutableMapOf<String, JsonArray>()
        mockStorage["default"] = JsonArray().add(JsonObject().put("name", "defaultSource"))

        fun saveSources(userNameSpace: String, sources: JsonArray) {
            mockStorage[userNameSpace] = sources
            if (userNameSpace == "admin" || userNameSpace == "default") {
                mockStorage["default"] = sources
            }
        }

        // Normal user "bob" saves their sources
        val bobSources = JsonArray().add(JsonObject().put("name", "bobCustomSource"))
        saveSources("bob", bobSources)

        assertEquals("bobCustomSource", mockStorage["bob"]!!.getJsonObject(0).getString("name"))
        // default remains unchanged!
        assertEquals("defaultSource", mockStorage["default"]!!.getJsonObject(0).getString("name"))

        // Admin saves sources
        val adminSources = JsonArray().add(JsonObject().put("name", "adminSource"))
        saveSources("admin", adminSources)
        assertEquals("adminSource", mockStorage["admin"]!!.getJsonObject(0).getString("name"))
        assertEquals("adminSource", mockStorage["default"]!!.getJsonObject(0).getString("name"))
    }

    @Test
    fun testScopedPwaDismissKeyCleanup() {
        val mockLocalStorage = mutableMapOf<String, String>()
        mockLocalStorage["pwa_banner_dismissed_anonymous"] = "1"
        mockLocalStorage["pwa_banner_dismissed_userA"] = "1"
        mockLocalStorage["pwa_banner_dismissed_userA:token123"] = "1"
        mockLocalStorage["pwa_banner_dismissed_userB"] = "1"
        mockLocalStorage["pwa_banner_dismissed_userB:token456"] = "1"

        // Simulate userA login: should only clear userA, userA's token keys, and anonymous
        val loggingInUser = "userA"
        val userToken = "userA:token123"
        val keysToRemove = mockLocalStorage.keys.filter { k ->
            k == "pwa_banner_dismissed_anonymous" ||
            k == "pwa_banner_dismissed_$loggingInUser" ||
            k.startsWith("pwa_banner_dismissed_$loggingInUser:") ||
            k == "pwa_banner_dismissed_$userToken"
        }
        keysToRemove.forEach { mockLocalStorage.remove(it) }

        // userA and anonymous cleared
        assertFalse(mockLocalStorage.containsKey("pwa_banner_dismissed_userA"))
        assertFalse(mockLocalStorage.containsKey("pwa_banner_dismissed_userA:token123"))
        assertFalse(mockLocalStorage.containsKey("pwa_banner_dismissed_anonymous"))

        // userB keys MUST BE PRESERVED
        assertTrue(mockLocalStorage.containsKey("pwa_banner_dismissed_userB"))
        assertTrue(mockLocalStorage.containsKey("pwa_banner_dismissed_userB:token456"))
        assertEquals("1", mockLocalStorage["pwa_banner_dismissed_userB"])
    }

    @Test
    fun testImportSourceUnpacking() {
        fun unpack(input: Any): List<Map<String, Any>> {
            return when (input) {
                is List<*> -> input.filterIsInstance<Map<String, Any>>()
                is Map<*, *> -> {
                    val dataList = input["data"] as? List<*> ?: input["sources"] as? List<*> ?: input["list"] as? List<*>
                    if (dataList != null) {
                        dataList.filterIsInstance<Map<String, Any>>()
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        listOf(input as Map<String, Any>)
                    }
                }
                else -> emptyList()
            }
        }

        val directArray = listOf(mapOf("bookSourceName" to "Source 1"))
        assertEquals(1, unpack(directArray).size)

        val wrappedData = mapOf("data" to listOf(mapOf("bookSourceName" to "Source 2"), mapOf("bookSourceName" to "Source 3")))
        assertEquals(2, unpack(wrappedData).size)

        val wrappedSources = mapOf("sources" to listOf(mapOf("bookSourceName" to "Source 4")))
        assertEquals(1, unpack(wrappedSources).size)

        val singleObject = mapOf("bookSourceName" to "Source 5")
        assertEquals(1, unpack(singleObject).size)
    }
}

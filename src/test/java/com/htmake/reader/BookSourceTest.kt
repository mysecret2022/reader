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

    @Test
    fun testAdvancedImportSourceUnpackingWithStringsAndBOM() {
        fun unpackAdvanced(input: Any): List<JsonObject> {
            val list = mutableListOf<JsonObject>()
            when (input) {
                is String -> {
                    val clean = input.removePrefix("\uFEFF").trim()
                    if (clean.startsWith("[")) {
                        val arr = JsonArray(clean)
                        for (i in 0 until arr.size()) {
                            list.addAll(unpackAdvanced(arr.getValue(i)))
                        }
                    } else if (clean.startsWith("{")) {
                        list.addAll(unpackAdvanced(JsonObject(clean)))
                    }
                }
                is List<*> -> {
                    for (item in input) {
                        if (item != null) list.addAll(unpackAdvanced(item))
                    }
                }
                is JsonArray -> {
                    for (i in 0 until input.size()) {
                        val v = input.getValue(i)
                        if (v != null) list.addAll(unpackAdvanced(v))
                    }
                }
                is JsonObject -> {
                    when {
                        input.containsKey("data") && input.getValue("data") is JsonArray ->
                            list.addAll(unpackAdvanced(input.getJsonArray("data")))
                        input.containsKey("sources") && input.getValue("sources") is JsonArray ->
                            list.addAll(unpackAdvanced(input.getJsonArray("sources")))
                        input.containsKey("list") && input.getValue("list") is JsonArray ->
                            list.addAll(unpackAdvanced(input.getJsonArray("list")))
                        input.containsKey("bookSourceName") || input.containsKey("bookSourceUrl") ->
                            list.add(input)
                    }
                }
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    list.addAll(unpackAdvanced(JsonObject(input as Map<String, Any>)))
                }
            }
            return list
        }

        // Test 1: Array of raw JSON strings from remote proxy (readRemoteSourceFile)
        val remoteProxyStrings = listOf("[{\"bookSourceName\":\"Source From Remote 1\"},{\"bookSourceName\":\"Source From Remote 2\"}]")
        val unpackedRemote = unpackAdvanced(remoteProxyStrings)
        assertEquals(2, unpackedRemote.size)
        assertEquals("Source From Remote 1", unpackedRemote[0].getString("bookSourceName"))

        // Test 2: JSON String with UTF-8 BOM
        val bomString = "\uFEFF[{\"bookSourceName\":\"BOM Source\"}]"
        val unpackedBom = unpackAdvanced(bomString)
        assertEquals(1, unpackedBom.size)
        assertEquals("BOM Source", unpackedBom[0].getString("bookSourceName"))

        // Test 3: Nested Legado 3.0 wrapper in string
        val nestedString = "{\"data\":[{\"bookSourceName\":\"Legado Nested Source\"}]}"
        val unpackedNested = unpackAdvanced(nestedString)
        assertEquals(1, unpackedNested.size)
        assertEquals("Legado Nested Source", unpackedNested[0].getString("bookSourceName"))
    }

    @Test
    fun testSandboxFilteringAndAutoExclusion() {
        data class SandboxSource(
            val bookSourceName: String,
            val bookSourceUrl: String,
            var speedStatus: String? = null,
            var speedCost: Long? = null,
            var speedError: String? = null,
            var isTesting: Boolean = false,
            var sandboxChecked: Boolean = false // 待测状态默认不勾选，体检通过后自动勾选优质源
        )

        val sandboxList = mutableListOf(
            SandboxSource("极速源 1", "https://fast1.com"),
            SandboxSource("极速源 2", "https://fast2.com"),
            SandboxSource("较慢源 1", "https://slow1.com"),
            SandboxSource("超时死源", "https://timeout-dead.com"),
            SandboxSource("HTTP500死源", "https://error500.com"),
            SandboxSource("缺失地址畸形源", ""),
            SandboxSource("尚未体检待测源", "https://pending.com")
        )

        // Simulate concurrent speed test in sandbox
        fun applyTestResult(source: SandboxSource, cost: Long, isOk: Boolean, errorMsg: String? = null) {
            if (source.bookSourceUrl.isBlank()) {
                source.speedStatus = "failed"
                source.speedCost = 0
                source.speedError = "缺少有效书源地址"
                source.sandboxChecked = false
                return
            }
            source.speedCost = cost
            if (isOk) {
                if (cost < 1500) {
                    source.speedStatus = "fast"
                    source.sandboxChecked = true // 自动勾选优质极速源
                } else if (cost <= 4000) {
                    source.speedStatus = "slow"
                    source.sandboxChecked = true // 自动勾选较慢可用源
                } else {
                    source.speedStatus = "failed"
                    source.sandboxChecked = false // 自动剔除超时死源
                    source.speedError = "响应超时 (>4s)"
                }
            } else {
                source.speedStatus = "failed"
                source.sandboxChecked = false // 自动剔除错误死源
                source.speedError = errorMsg ?: "连接失败"
            }
        }

        applyTestResult(sandboxList[0], 280, true)
        applyTestResult(sandboxList[1], 850, true)
        applyTestResult(sandboxList[2], 2300, true)
        applyTestResult(sandboxList[3], 4500, true)
        applyTestResult(sandboxList[4], 300, false, "HTTP 500")
        applyTestResult(sandboxList[5], 0, false)
        // sandboxList[6] remains pending/untested

        // 1. Verify health status buckets
        assertEquals("fast", sandboxList[0].speedStatus)
        assertEquals("fast", sandboxList[1].speedStatus)
        assertEquals("slow", sandboxList[2].speedStatus)
        assertEquals("failed", sandboxList[3].speedStatus)
        assertEquals("failed", sandboxList[4].speedStatus)
        assertEquals("failed", sandboxList[5].speedStatus)
        assertEquals(null, sandboxList[6].speedStatus)

        // 2. Verify auto-selection: quality/usable sources checked, dead & invalid & untested sources unchecked
        assertTrue(sandboxList[0].sandboxChecked)
        assertTrue(sandboxList[1].sandboxChecked)
        assertTrue(sandboxList[2].sandboxChecked)
        assertFalse(sandboxList[3].sandboxChecked)
        assertFalse(sandboxList[4].sandboxChecked)
        assertFalse(sandboxList[5].sandboxChecked)
        assertFalse(sandboxList[6].sandboxChecked)

        // 3. Confirm import: only checked sources are persisted, dead/untested sources discarded in memory (zero pollution)
        val selectedToPersist = sandboxList.filter { it.sandboxChecked }
        val discardedCount = sandboxList.size - selectedToPersist.size

        assertEquals(3, selectedToPersist.size)
        assertEquals(4, discardedCount)
        assertEquals("极速源 1", selectedToPersist[0].bookSourceName)
        assertEquals("极速源 2", selectedToPersist[1].bookSourceName)
        assertEquals("较慢源 1", selectedToPersist[2].bookSourceName)
    }

    @Test
    fun testSyncAdminSourcesPreviewMode() {
        val mockStorage = mutableMapOf<String, JsonArray>()
        val defaultSources = JsonArray()
            .add(JsonObject().put("bookSourceName", "Default Source 1"))
            .add(JsonObject().put("bookSourceName", "Default Source 2"))
        mockStorage["default/bookSource"] = defaultSources

        // Existing user has their own custom sources
        val userInitialSources = JsonArray().add(JsonObject().put("bookSourceName", "User Custom Source"))
        mockStorage["testUser/bookSource"] = userInitialSources

        fun syncAdminSources(userNameSpace: String, isPreview: Boolean): JsonArray {
            val adminSources = mockStorage["admin/bookSource"] ?: mockStorage["default/bookSource"] ?: JsonArray()
            if (!isPreview) {
                mockStorage["$userNameSpace/bookSource"] = adminSources.copy()
            }
            return adminSources
        }

        // 1. When preview=true (for loading into sandbox), user storage MUST NOT be overwritten
        val previewResult = syncAdminSources("testUser", isPreview = true)
        assertEquals(2, previewResult.size())
        assertEquals(1, mockStorage["testUser/bookSource"]!!.size())
        assertEquals("User Custom Source", mockStorage["testUser/bookSource"]!!.getJsonObject(0).getString("bookSourceName"))

        // 2. When preview=false (direct sync), user storage is overwritten
        val directResult = syncAdminSources("testUser", isPreview = false)
        assertEquals(2, directResult.size())
        assertEquals(2, mockStorage["testUser/bookSource"]!!.size())
        assertEquals("Default Source 1", mockStorage["testUser/bookSource"]!!.getJsonObject(0).getString("bookSourceName"))
    }

    @Test
    fun testCleanSourceForSave() {
        val frontendSandboxObject = JsonObject()
            .put("bookSourceName", "优质源")
            .put("bookSourceUrl", "https://good-source.com")
            .put("ruleSearch", "$.data[*]")
            .put("_sandboxChecked", true)
            .put("_sandboxUid", "sb_1234567")
            .put("speedStatus", "fast")
            .put("speedCost", 320)
            .putNull("speedError")
            .put("isTesting", false)
            .put("isSingleTesting", false)

        fun cleanForSave(source: JsonObject): JsonObject {
            val cleaned = source.copy()
            cleaned.remove("_sandboxChecked")
            cleaned.remove("_sandboxUid")
            cleaned.remove("speedStatus")
            cleaned.remove("speedCost")
            cleaned.remove("speedError")
            cleaned.remove("isTesting")
            cleaned.remove("isSingleTesting")
            return cleaned
        }

        val cleaned = cleanForSave(frontendSandboxObject)
        assertFalse(cleaned.containsKey("_sandboxChecked"))
        assertFalse(cleaned.containsKey("_sandboxUid"))
        assertFalse(cleaned.containsKey("speedStatus"))
        assertFalse(cleaned.containsKey("speedCost"))
        assertFalse(cleaned.containsKey("speedError"))
        assertFalse(cleaned.containsKey("isTesting"))
        assertFalse(cleaned.containsKey("isSingleTesting"))

        // Pure book source attributes preserved intact
        assertEquals("优质源", cleaned.getString("bookSourceName"))
        assertEquals("https://good-source.com", cleaned.getString("bookSourceUrl"))
        assertEquals("$.data[*]", cleaned.getString("ruleSearch"))
    }

    @Test
    fun testWebdavBackupCandidateDirsPriorityAndSorting() {
        val tempBase = java.io.File(System.getProperty("java.io.tmpdir"), "reader-test-webdav-" + System.currentTimeMillis())
        try {
            val webDir = java.io.File(tempBase, "web").apply { mkdirs() }
            val legadoDir = java.io.File(tempBase, "legado").apply { mkdirs() }
            val rootDir = tempBase

            val candidateDirs = listOf(webDir, legadoDir, rootDir)
            val zipFileReg = Regex("^backup[0-9-]+.zip$", RegexOption.IGNORE_CASE)

            fun findLatestBackup(): java.io.File? {
                val allMatching = mutableListOf<java.io.File>()
                for (dir in candidateDirs) {
                    if (dir.exists()) {
                        val files = dir.listFiles() ?: continue
                        for (it in files) {
                            if (zipFileReg.matches(it.name)) {
                                allMatching.add(it)
                            }
                        }
                    }
                }
                allMatching.sortWith(
                    compareByDescending<java.io.File> { it.lastModified() }
                        .thenByDescending { if (it.parentFile?.name == "web") 1 else 0 }
                )
                return allMatching.firstOrNull()
            }

            // Case 1: Empty directories
            assertEquals(null, findLatestBackup())

            // Case 2: Only legado has a backup
            val legadoFile = java.io.File(legadoDir, "backup2026-11-10.zip").apply {
                createNewFile()
                setLastModified(1000000L)
            }
            assertEquals(legadoFile.absolutePath, findLatestBackup()?.absolutePath)

            // Case 3: Web has a newer backup
            val webFile = java.io.File(webDir, "backup2026-11-12.zip").apply {
                createNewFile()
                setLastModified(2000000L)
            }
            assertEquals(webFile.absolutePath, findLatestBackup()?.absolutePath)

            // Case 4: Equal timestamps - web is preferred
            legadoFile.setLastModified(2000000L)
            webFile.setLastModified(2000000L)
            assertEquals("web", findLatestBackup()?.parentFile?.name)

            // Case 5: Legado has newer backup than older web backup
            legadoFile.setLastModified(3000000L)
            assertEquals(legadoFile.absolutePath, findLatestBackup()?.absolutePath)
        } finally {
            tempBase.deleteRecursively()
        }
    }

    @Test
    fun testApiProtocolResolution() {
        fun resolveApi(stateApi: String): String {
            if (
                stateApi.startsWith("http://") ||
                stateApi.startsWith("https://") ||
                stateApi.startsWith("//")
            ) {
                return stateApi
            }
            return "//$stateApi"
        }

        // Without scheme: must add // to prevent Axios relative path 404
        assertEquals("//localhost:8080/reader3", resolveApi("localhost:8080/reader3"))
        assertEquals("//127.0.0.1:8080/reader3", resolveApi("127.0.0.1:8080/reader3"))
        assertEquals("//example.com/reader3", resolveApi("example.com/reader3"))

        // With scheme or protocol-relative: preserved as-is
        assertEquals("//localhost:8080/reader3", resolveApi("//localhost:8080/reader3"))
        assertEquals("http://localhost:8080/reader3", resolveApi("http://localhost:8080/reader3"))
        assertEquals("https://reader.example.com", resolveApi("https://reader.example.com"))
    }

    @Test
    fun testAdFilteringPatternsAndScoping() {
        val escapeRegex: (String) -> String = { str ->
            java.util.regex.Pattern.quote(str)
        }

        // 1. Exact text filtering (multiple occurrences)
        val text = "第一章\n求书请关注微信公众号：【大千书屋】\n萧炎一言不发。\n求书请关注微信公众号：【大千书屋】"
        val exactTarget = "求书请关注微信公众号：【大千书屋】"
        val filteredExact = text.split(exactTarget).joinToString("")
        assertFalse(filteredExact.contains(exactTarget))
        assertEquals("第一章\n\n萧炎一言不发。\n", filteredExact)

        // 2. Full line ad filtering (CRLF and LF compatibility with [^\r\n])
        val rawChapterLF = "第一章 初始\n【最新章节请访问 顶点小说网：www.dingdian.com，无弹窗】\n主角深吸一口气。\n"
        val linePattern = "[^\\r\\n]*" + escapeRegex("www.dingdian.com") + "[^\\r\\n]*(?:\\r?\\n|$)"
        val filteredLineLF = Regex(linePattern).replace(rawChapterLF, "")
        assertEquals("第一章 初始\n主角深吸一口气。\n", filteredLineLF)
        assertFalse(filteredLineLF.contains("顶点小说网"))
        assertFalse(filteredLineLF.contains("www.dingdian.com"))

        val rawChapterCRLF = "第一章 初始\r\n【最新章节请访问 顶点小说网：www.dingdian.com，无弹窗】\r\n主角深吸一口气。\r\n"
        val filteredLineCRLF = Regex(linePattern).replace(rawChapterCRLF, "")
        assertEquals("第一章 初始\r\n主角深吸一口气。\r\n", filteredLineCRLF)
        assertFalse(filteredLineCRLF.contains("顶点小说网"))

        // Multi-line ad pattern test
        val multilineText = "关注公众号：书友俱乐部\n获取完整无弹窗"
        val safeMultiPattern = multilineText.split(Regex("\\r?\\n")).map { escapeRegex(it) }.joinToString("(?:\\r?\\n)")
        val multiLineRegex = Regex("[^\\r\\n]*$safeMultiPattern[^\\r\\n]*(?:\\r?\\n|$)")
        val chapterWithMultiAd = "正文开头\r\n请关注公众号：书友俱乐部\r\n获取完整无弹窗小说。\r\n正文结尾"
        val filteredMulti = multiLineRegex.replace(chapterWithMultiAd, "")
        assertEquals("正文开头\r\n正文结尾", filteredMulti)

        // 3. Smart domain URL filtering (matches all links under domain, including subdomains and compound TLDs)
        val domain = "biquge.tv"
        val domainPattern = "(?:https?:\\/\\/)?(?:[a-zA-Z0-9-]+\\.)*" + escapeRegex(domain) + "(?::\\d+)?(?:\\/[^\\s\\u4e00-\\u9fa5<>\"']*)?"
        val domainRegex = Regex(domainPattern, RegexOption.IGNORE_CASE)

        val chapterWithUrls = """
            正文开始。
            访问链接一：https://www.biquge.tv/read/123.html
            访问链接二：http://m.biquge.tv:8080/chapter/456?ad=1
            访问链接三：wap.biquge.tv/book/789
            请大家记住域名：biquge.tv
            正文结束。
        """.trimIndent()

        val filteredUrls = domainRegex.replace(chapterWithUrls, "")
        assertFalse(filteredUrls.contains("biquge.tv"))
        assertTrue(filteredUrls.contains("正文开始。"))
        assertTrue(filteredUrls.contains("正文结束。"))

        // Compound TLD: .com.cn
        val cnDomain = "biquge.com.cn"
        val cnPattern = "(?:https?:\\/\\/)?(?:[a-zA-Z0-9-]+\\.)*" + escapeRegex(cnDomain) + "(?::\\d+)?(?:\\/[^\\s\\u4e00-\\u9fa5<>\"']*)?"
        val cnRegex = Regex(cnPattern, RegexOption.IGNORE_CASE)
        val cnContent = "请访问 https://wap.biquge.com.cn/123 获取更新"
        val filteredCn = cnRegex.replace(cnContent, "")
        assertFalse(filteredCn.contains("biquge.com.cn"))
        assertFalse(filteredCn.contains("wap"))

        // 4. Scope matching verification
        fun matchScope(ruleScope: String?, currentBookName: String, currentBookUrl: String): Boolean {
            val scopeStr = ruleScope?.trim() ?: ""
            if (scopeStr.isEmpty() || scopeStr == "*") return true
            val parts = scopeStr.split(";")
            if (parts[0] != "*" && parts[0] != currentBookName) return false
            if (parts.size > 1 && parts[1].isNotEmpty() && parts[1] != currentBookUrl) return false
            return true
        }

        assertTrue(matchScope("*", "斗破苍穹", "https://source1.com/book/1"))
        assertTrue(matchScope("斗破苍穹", "斗破苍穹", "https://source1.com/book/1"))
        assertTrue(matchScope("斗破苍穹;https://source1.com/book/1", "斗破苍穹", "https://source1.com/book/1"))
        assertFalse(matchScope("完美世界", "斗破苍穹", "https://source1.com/book/1"))
        assertFalse(matchScope("斗破苍穹;https://source2.com/book/2", "斗破苍穹", "https://source1.com/book/1"))
    }

    @Test
    fun testRemoteSourceFetchWithRedirect() {
        val url = "https://www.yckceo.com/yuedu/shuyuans/json/id/1335.json"
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "application/json, text/plain, */*")
            .build()
        val response = io.legado.app.help.http.okHttpClient.newCall(request).execute()
        assertTrue("Redirected remote source request should succeed", response.isSuccessful)
        val body = response.body?.string()
        assertTrue("Body should contain book sources array", body != null && body.trim().startsWith("["))
        assertTrue("Body should contain actual book source definitions", body != null && body.contains("bookSourceName"))
    }
}

package io.legado.app.help

import com.script.CompiledScript
import com.script.Bindings
import io.legado.app.constant.AppConst
import java.security.MessageDigest
import java.util.LinkedHashMap

/**
 * Rhino JS 脚本预编译 LRU 缓存池
 * 缓存编译后的 CompiledScript，避免在列表规则中重复编译相同 JS 源码的 AST。
 */
object RhinoScriptCache {

    private const val MAX_ENTRIES = 256

    private val cache = object : LinkedHashMap<String, CompiledScript>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CompiledScript>?): Boolean {
            return size > MAX_ENTRIES
        }
    }

    private fun md5(str: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(str.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 执行 JS，优先从预编译 LRU 缓存获取 CompiledScript 执行
     */
    fun eval(jsStr: String, bindings: Bindings): Any? {
        val key = md5(jsStr)
        val compiledScript = synchronized(cache) {
            cache[key] ?: run {
                try {
                    val script = AppConst.SCRIPT_ENGINE.compile(jsStr)
                    cache[key] = script
                    script
                } catch (e: Exception) {
                    null
                }
            }
        }

        return if (compiledScript != null) {
            compiledScript.eval(bindings)
        } else {
            AppConst.SCRIPT_ENGINE.eval(jsStr, bindings)
        }
    }
}

package io.legado.app.constant

import java.text.SimpleDateFormat
import com.script.javascript.RhinoScriptEngine

object AppConst {

    init {
        try {
            if (!org.mozilla.javascript.ContextFactory.hasExplicitGlobal()) {
                org.mozilla.javascript.ContextFactory.initGlobal(object : org.mozilla.javascript.ContextFactory() {
                    override fun makeContext(): org.mozilla.javascript.Context {
                        val cx = super.makeContext()
                        cx.optimizationLevel = -1
                        cx.instructionObserverThreshold = 50000
                        return cx
                    }

                    override fun observeInstructionCount(cx: org.mozilla.javascript.Context, instructionCount: Int) {
                        if (instructionCount > 1000000) {
                            throw RuntimeException("Rhino JS 脚本指令超限(>1,000,000条)，判定为死循环已强行中断")
                        }
                    }
                })
            }
        } catch (e: Throwable) {
            // Already initialized or ignored
        }
    }

    const val UA_NAME = "User-Agent"

    val userAgent: String by lazy {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36"
    }

    val SCRIPT_ENGINE: RhinoScriptEngine by lazy {
        RhinoScriptEngine()
    }

    val TIME_FORMAT: SimpleDateFormat by lazy {
        SimpleDateFormat("HH:mm")
    }

    val timeFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("HH:mm")
    }

    val dateFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy/MM/dd HH:mm")
    }

    val fileNameFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("yy-MM-dd-HH-mm-ss")
    }

    val keyboardToolChars: List<String> by lazy {
        arrayListOf(
            "@", "&", "|", "%", "/", ":", "[", "]", "{", "}", "<", ">", "\\", "$", "#", "!", ".",
            "href", "src", "textNodes", "xpath", "json", "css", "id", "class", "tag"
        )
    }

    const val JS_INJECT_HELPERS = """function getVariable(k){try{if(k!==undefined&&k!==null){if(typeof java!=='undefined'&&java&&java.getVariable)return java.getVariable(k)||'';if(typeof source!=='undefined'&&source&&source.getVariable)return source.getVariable(k)||'';}else{if(typeof java!=='undefined'&&java&&java.getVariable)return java.getVariable()||'';if(typeof source!=='undefined'&&source&&source.getVariable)return source.getVariable()||'';}}catch(e){}return '';};function setVariable(a,b){try{if(b!==undefined){if(typeof java!=='undefined'&&java&&java.putVariable)return java.putVariable(a,b);if(typeof source!=='undefined'&&source&&source.putVariable)return source.putVariable(a,b);}else{if(typeof java!=='undefined'&&java&&java.setVariable)return java.setVariable(a);if(typeof source!=='undefined'&&source&&source.setVariable)return source.setVariable(a);}}catch(e){}};function putVariable(k,v){try{if(typeof java!=='undefined'&&java&&java.putVariable)return java.putVariable(k,v);if(typeof source!=='undefined'&&source&&source.putVariable)return source.putVariable(k,v);}catch(e){}return v;};"""
}
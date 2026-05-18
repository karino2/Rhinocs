package io.github.karino2.rhinocs

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.ContinuationPending
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

class Interpreter {
    val factory: ContextFactory = object : ContextFactory() {
        override fun makeContext(): Context {
            val cx = super.makeContext()
            cx.isGeneratingDebug = false
            cx.isInterpretedMode = true
            return cx
        }
    }

    fun <T> withContext(block: (Context) -> T) : T{
        val cx = factory.enterContext()
        try {
            return block(cx)
        } finally {
            Context.exit()
        }
    }

    val global by lazy {
        withContext { GlobalObject(it) }
    }

    fun run(script: String, fileName:String ="*script*") {
        withContext {
            val script = it.compileString(script, fileName, 1, null)
            it.executeScriptWithContinuations(script, global)
            global.rview.invalidate()
        }
        runPendingRequest()
    }

    fun resume(cc: ContinuationPending, result: Any) {
        withContext {
            it.resumeContinuation(cc.continuation, global, result)
            global.rview.invalidate()
        }
        runPendingRequest()
    }

    fun newJSArray(arr: Array<Any>) = withContext { it.newArray(global, arr) }

    private fun runPendingRequest() {
        if (global.hasPendingRequest()) {
            val req = global.popLoadRequest()
            global.activity.loadPackageJS(req)
        }
    }

    fun setGlobalKey(strKey: String) {
        withContext {
            ScriptableObject.putProperty(global, $$"$key", strKey)
        }
    }



}
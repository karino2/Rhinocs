package io.github.karino2.rhinocs

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.ContinuationPending
import org.mozilla.javascript.Function
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
            global.rhinocs.statusText = ""
            val script = it.compileString(script, fileName, 1, null)
            it.executeScriptWithContinuations(script, global)
            it.processMicrotasks()
            global.rview.invalidate()
        }
        runPendingRequest()
    }

    fun callFunction(jsFunction: Function)  {
        withContext {
            it.callFunctionWithContinuations(jsFunction, global, emptyArray<Any>())
        }
    }

    fun resume(cc: ContinuationPending, result: Any) {
        withContext {
            it.resumeContinuation(cc.continuation, global, result)
            it.processMicrotasks()
            global.rview.invalidate()
        }
        runPendingRequest()
    }

    fun newJSArray(arr: Array<Any>) = withContext { it.newArray(global, arr) }

    private fun runPendingRequest() {
        if (global.hasPendingRequest()) {
            val req = global.popDelayedRequest()
            when(req.type) {
                DelayedRequestType.LOAD_JS -> {
                    val larg = req.arg as DelayedRequest.JSLoadArg
                    global.activity.loadPackageJS(larg.fname)
                    larg.callAfter?.let {
                        global.activity.callJsFunction(it)
                    }
                }
                DelayedRequestType.CALL_FUNCTION -> {
                    val farg = req.arg as Function
                    callFunction(farg)
                }
            }
        }
    }


    fun setGlobalKey(strKey: String) {
        withContext {
            ScriptableObject.putProperty(global, $$"$key", strKey)
        }
    }



}
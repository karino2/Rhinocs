package io.github.karino2.rhinocs

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.ContinuationPending
import org.mozilla.javascript.Function
import org.mozilla.javascript.JSFunction
import org.mozilla.javascript.ScriptableObject
import java.io.FileNotFoundException

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

    fun callFunction(jsFunction: Function, args: Array<Any>)  {
        withContext {
            if (jsFunction is JSFunction)
                it.callFunctionWithContinuations(jsFunction, global, args)
            else
                jsFunction.call(it, global, global, args)

            it.processMicrotasks()
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
                    try
                    {
                        if(!global.activity.loadPackageJS(larg.fname, mayNotExist = true))
                        {
                            global.activity.callJsFunction(larg.onFailure, arrayOf<Any>(FileNotFoundException(larg.fname)))
                            return
                        }
                        global.activity.callJsFunction(larg.onSuccess, emptyArray<Any>())
                    }catch(e: Exception) {
                        global.activity.callJsFunction(larg.onFailure, arrayOf<Any>(e))
                    }
                }
                DelayedRequestType.CALL_FUNCTION -> {
                    val farg = req.arg as Function
                    callFunction(farg, emptyArray<Any>())
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
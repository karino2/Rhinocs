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
            global.rhinocs.echoText = ""
            val script = it.compileString(script, fileName, 1, null)
            it.executeScriptWithContinuations(script, global)
            it.processMicrotasks()
            global.rview.invalidate()
        }
        runPendingRequest()
    }

    // promiseのresolveやrejectは引数一つ前提なので、
    // 複数引数は配列としてまとめる。
    fun callOneArgCallback(jsFunction: Function, vararg args: Any) {
        if (args.size == 1)
        {
            callFunction(jsFunction, arrayOf<Any>(args[0]))
        }
        else
        {
            val jsArr = withContext { it.newArray(global, args) }
            callFunction(jsFunction, arrayOf<Any>(jsArr))
        }

    }

    fun callSuccess(ca: DelayedRequest.Arg, vararg args: Any) {
        callOneArgCallback(ca.onSuccess, *args)
    }

    fun callFail(ca: DelayedRequest.Arg, vararg args: Any) {
        callOneArgCallback(ca.onFailure, *args)
    }

    fun callFunction(jsFunction: Function, args: Array<Any>)  {
        withContext {
            if (jsFunction is JSFunction)
                it.callFunctionWithContinuations(jsFunction, global, args)
            else
                jsFunction.call(it, global, global, args)

            it.processMicrotasks()
            runPendingRequest()
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

    private fun runPendingRequest() {
        if (global.hasPendingRequest()) {
            val req = global.popDelayedRequest()
            when(req.type) {
                DelayedRequestType.SELECT_OPEN_FILE-> {
                    val larg = req.arg
                    val mtypes = larg.userArg as Array<String>
                    global.activity.callbackArg = larg
                    global.activity.getOpenFileUriFromScript.launch(mtypes)
                    return
                }
                DelayedRequestType.SELECT_NEW_FILE-> {
                    val larg = req.arg
                    val defName = larg.userArg as String
                    global.activity.callbackArg = larg
                    global.activity.getNewFileUriFromScript.launch(defName)
                    return
                }
                DelayedRequestType.LOAD_JS -> {
                    val larg = req.arg
                    try
                    {
                        val fname = larg.userArg as String
                        if(!global.activity.loadPackageJS(fname, mayNotExist = true))
                        {
                            callFail(larg, FileNotFoundException(fname))
                            return
                        }
                        callSuccess(larg)
                    }catch(e: Exception) {
                        callFail(larg, e)
                    }
                }
                DelayedRequestType.QUERY_TEXT_DIALOG -> {
                    val larg = req.arg
                    val label = larg.userArg as String
                    global.activity.queryTextDialog(label, larg)
                }
                DelayedRequestType.READ_KEY -> {
                    val larg = req.arg
                    val label = larg.userArg as String
                    global.activity.waitReadKey(label, larg)
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
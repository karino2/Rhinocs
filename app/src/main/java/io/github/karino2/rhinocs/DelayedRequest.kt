package io.github.karino2.rhinocs

import org.mozilla.javascript.Function

enum class DelayedRequestType {
    LOAD_JS,
    CALL_FUNCTION
}

class DelayedRequest(val type: DelayedRequestType, val arg: Any) {
    class JSLoadArg(val fname: String, val callAfter: Function?)
    companion object {
        fun jsLoadRequest(fname: String, callAfter: Function?)
        = DelayedRequest(
            DelayedRequestType.LOAD_JS,
            JSLoadArg(fname, callAfter))
    }
}
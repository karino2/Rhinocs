package io.github.karino2.rhinocs

import org.mozilla.javascript.Function

enum class DelayedRequestType {
    LOAD_JS,
    CALL_FUNCTION,
    SELECT_FILE,
}

class DelayedRequest(val type: DelayedRequestType, val arg: Any) {
    class AsyncArg(val arg: Any, val onSuccess: Function, val onFailure: Function)
    companion object {
        fun jsLoadRequest(fname: String, onSuccess: Function, onFailure: Function)
        = DelayedRequest(
            DelayedRequestType.LOAD_JS,
            AsyncArg(fname, onSuccess, onFailure))
    }
}
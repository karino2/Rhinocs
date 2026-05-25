package io.github.karino2.rhinocs

import org.mozilla.javascript.Function

enum class DelayedRequestType {
    LOAD_JS,
    SELECT_FILE,
    QUERY_TEXT_DIALOG,
    READ_KEY,
}

class DelayedRequest(val type: DelayedRequestType, val arg: Arg) {
    class Arg(val userArg: Any, val onSuccess: Function, val onFailure: Function)
    companion object {
        fun jsLoadRequest(fname: String, onSuccess: Function, onFailure: Function)
        = DelayedRequest(
            DelayedRequestType.LOAD_JS,
            Arg(fname, onSuccess, onFailure))
    }
}
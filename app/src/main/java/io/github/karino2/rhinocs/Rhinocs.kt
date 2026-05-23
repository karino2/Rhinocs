package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.net.Uri

/**
 * Windowやミニバッファなどを持つモデル側の一番外側のクラス。
 * Editor全体を表す。
 */
class Rhinocs {
    val window = Window().apply { buffer = Buffer().apply { name = "*scratch*" } }

    var miniBufferWindow: MiniBufferWindow? = null

    val selectedWindow: Window
        get() {
            return miniBufferWindow?.window ?: window
        }

    val selectedBuffer : Buffer
        get() = selectedWindow.buffer

    var numRows: Int
        get() = window.numRows
        set(value) { window.numRows = value }

    var numCols: Int
        get() = window.numCols
        set(value) {
            window.numCols = value
            miniBufferWindow?.numCols = value
        }

    fun enterMiniBuffer(prompt: String) {
        if(miniBufferWindow != null)
            throw Error("Recursive minibuffer enter, NYI.")


        val mwin = MiniBufferWindow()
        mwin.numCols = numCols
        mwin.window.isSelected = true
        mwin.miniBuffer.prompt = prompt
        miniBufferWindow = mwin
        window.isSelected = false
    }

    fun leaveMiniBuffer() : String {
        window.isSelected = true
        val ret = miniBufferWindow?.miniBuffer?.buffer?.getLine(0) ?: ""
        miniBufferWindow = null
        return ret
    }

    fun loadFile(resolver: ContentResolver, uri: Uri) {
        window.loadFile(resolver, uri)
    }

    var statusText = ""

    val defaultModeFmt = "\${bufferName} [\${lineNum}:\${column}]"
    var modeLineFormat = defaultModeFmt

    val modeLineText: String
        get() {
            val res = StringBuilder()
            var i = 0
            while (i < modeLineFormat.length) {
                val c = modeLineFormat[i]
                if (c == '$' && i + 1 < modeLineFormat.length && modeLineFormat[i + 1] == '{') {
                    val end = modeLineFormat.indexOf('}', i + 2)
                    if (end != -1) {
                        val symbol = modeLineFormat.substring(i + 2, end)
                        val value = when (symbol) {
                            "bufferName" -> selectedWindow.buffer.name
                            "column" -> selectedWindow.point.offset + 1
                            "lineNum" -> selectedWindow.point.linenum + 1
                            else -> "\${$symbol}"
                        }
                        res.append(value)
                        i = end + 1
                        continue
                    }
                }
                res.append(c)
                i++
            }
            return res.toString()
        }

    fun resetModeLineFormat() {
        modeLineFormat = defaultModeFmt
    }
}

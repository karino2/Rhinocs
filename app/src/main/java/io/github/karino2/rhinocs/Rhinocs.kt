package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.net.Uri

/**
 * Windowやミニバッファなどを持つモデル側の一番外側のクラス。
 * Editor全体を表す。
 */
class Rhinocs {
    val window = Window().apply { buffer = Buffer().apply { name = "*scratch*" } }
    val selectedBuffer : Buffer
        get() = window.buffer

    var numRows: Int
        get() = window.numRows
        set(value) { window.numRows = value }

    var numCols: Int
        get() = window.numCols
        set(value) { window.numCols = value }

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
                            "bufferName" -> window.buffer.name
                            "column" -> window.point.offset + 1
                            "lineNum" -> window.point.linenum + 1
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

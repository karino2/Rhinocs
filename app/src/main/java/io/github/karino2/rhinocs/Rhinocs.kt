package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.net.Uri

/**
 * Windowやミニバッファなどを持つモデル側の一番外側のクラス。
 * Editor全体を表す。
 */
class Rhinocs {
    val windowList = ArrayList<Window>().apply{ add(Window()) }

    /*
      基本的にはactiveはウィンドウを返すがminibufferにenterしている時は
      minibufferに入る直前のWindowを返す（ミニバッファからでたらアクティブになるWindow
     */
    val mainActiveWindow : Window
        get() = windowList[0]

    var miniBufferWindow: MiniBufferWindow? = null

    val selectedWindow: Window
        get() {
            return miniBufferWindow?.window ?: mainActiveWindow
        }

    val selectedBuffer : Buffer
        get() = selectedWindow.buffer

    var numRows: Int
        get() = windowList.sumOf{ it.numRows }
        set(value) {
            val winNum = windowList.size
            val restRows = value/winNum
            // 端数は最初のwindowに
            val firstRows = value - restRows*(winNum-1)
            windowList[0].numRows = firstRows
            windowList.drop(1).forEach {
                it.numRows = restRows
            }
        }

    var numCols: Int
        get() = mainActiveWindow.numCols
        set(value) {
            windowList.forEach { win-> win.numCols = value}
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
        mainActiveWindow.isSelected = false
    }

    fun leaveMiniBuffer() : String {
        mainActiveWindow.isSelected = true
        val ret = miniBufferWindow?.miniBuffer?.buffer?.getLine(0) ?: ""
        miniBufferWindow = null
        return ret
    }

    fun loadFile(resolver: ContentResolver, uri: Uri) {
        mainActiveWindow.loadFile(resolver, uri)
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

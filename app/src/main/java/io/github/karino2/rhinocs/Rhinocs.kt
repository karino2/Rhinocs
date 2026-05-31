package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.net.Uri

/**
 * Windowやミニバッファなどを持つモデル側の一番外側のクラス。
 * Editor全体を表す。
 */
class Rhinocs {
    val bufferCollection = BufferCollection()
    val windowList = ArrayList<Window>().apply{ add(Window(bufferCollection.getBufferCreate("*scratch*"))) }

    var lastMainActiveIndex = 0

    /*
      基本的にはactiveはウィンドウを返すがminibufferにenterしている時は
      minibufferに入る直前のWindowを返す（ミニバッファからでたらアクティブになるWindow
     */
    val mainActiveWindow : Window
        get() = windowList[lastMainActiveIndex]

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

    fun enterMiniBuffer(prompt: String): MiniBufferWindow {
        if(miniBufferWindow != null)
            throw Error("Recursive minibuffer enter, NYI.")

        val mwin = MiniBufferWindow()
        mwin.numCols = numCols
        mwin.window.isSelected = true
        mwin.miniBuffer.prompt = prompt
        miniBufferWindow = mwin
        mainActiveWindow.isSelected = false
        return mwin
    }

    fun leaveMiniBuffer() : String {
        mainActiveWindow.isSelected = true
        val ret = miniBufferWindow?.miniBuffer?.buffer?.getLine(0) ?: ""
        miniBufferWindow = null
        return ret
    }

    fun loadFile(resolver: ContentResolver, uri: Uri) {
        FastFile.fromDocUri(resolver, uri)?.let {
            val buf = bufferCollection.newBuffer(it.name)
            buf.load(it.readText())
            buf.url = uri
            mainActiveWindow.buffer = buf
        }
    }

    fun getBufferCreate(bname: String) = bufferCollection.getBufferCreate(bname)

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

    fun splitWindow(baseWin: Window) : Boolean{
        if(baseWin == miniBufferWindow?.window)
            return false

        // 今の所分割は２つまで
        if(windowList.size == 2)
            return false


        val newWin = Window(baseWin.buffer)
        newWin.numCols = baseWin.numCols
        newWin.numRows = baseWin.numRows/2
        baseWin.numRows = (baseWin.numRows+1)/2
        newWin.point = baseWin.point
        newWin.lastOffset = baseWin.lastOffset
        newWin.goalColumn = baseWin.goalColumn
        newWin.isSelected = false

        windowList.add(newWin)
        return true
    }

    fun deleteWindow(targetWin: Window) : Boolean{
        if(targetWin == miniBufferWindow?.window)
            return false

        if(windowList.size == 1)
            return false

        val targetIndex = windowList.indexOf(targetWin)
        // 起こらないはずだが、変なのを渡しているケース
        if (targetIndex == -1)
            return false

        // 一つなので手抜き
        val otherWin = getNextWindow(targetIndex)
        otherWin.numRows += targetWin.numRows
        lastMainActiveIndex = 0
        if(targetWin.isSelected)
            otherWin.isSelected = true

        windowList.remove(targetWin)
        return true
    }

    private fun getNextWindow(targetIndex: Int): Window {
        assert(windowList.size >= 2)
        return windowList[(targetIndex+1)%windowList.size]
    }


    fun switchToOtherWindow() : Boolean {
        if (windowList.size == 1)
            return false

        val lastSelected = windowList[lastMainActiveIndex].isSelected
        windowList[lastMainActiveIndex].isSelected = false
        lastMainActiveIndex = (lastMainActiveIndex+1)%windowList.size
        windowList[lastMainActiveIndex].isSelected = lastSelected
        return true
    }

    fun deleteOtherWindows(): Boolean {
        if (windowList.size == 1)
            return false

        val current = mainActiveWindow
        val delcand = windowList.filter { it != current }
        delcand.forEach {
            deleteWindow(it)
        }
        return true
    }

    val floatingList = FloatingList()
}

package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View

class RView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }

    private val caretPaint = Paint().apply {
        color = Color.parseColor("#4285F4") // Android標準に近い青色
        style = Paint.Style.FILL
    }

    private val margin = 10f

    val cellWidth: Float
        get() = textPaint.measureText("A")

    val cellHeight: Float
        get() = textPaint.fontMetrics.let { it.descent - it.ascent }

    val numRows: Int
        get() = window.numRows
    val numCols: Int
        get() = window.numCols


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        window.numCols = ((w - margin) / cellWidth).toInt()
        window.numRows = ((h - margin) / cellHeight).toInt()
    }

    val window = Window().apply { buffer = Buffer.fromText("Hello, Rhinocs!日本語\n二行目") }


    fun loadFile(resolver: ContentResolver, uri: Uri) {
        window.loadFile(resolver, uri)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val fm = textPaint.fontMetrics

        val startX = margin
        val startY = -fm.ascent + margin

        val currentPos = window.pointRowCol
        window.updateOffset(currentPos.col)

        for (row in 0..<numRows) {
            val linfo = window.lineInfo(row)
            for (col in 0..<numCols) {
                val cell = linfo[col]
                if (!cell.isEmpty) {
                    val numCells = cell.widthCount
                    // textAlign = Paint.Align.CENTER なので、中心座標を指定
                    val x = startX + (col * cellWidth) + (numCells * cellWidth / 2f)
                    val y = startY + (row * cellHeight)
                    canvas.drawText(cell.ch.toString(), x, y, textPaint)
                }
            }
        }

        val rpos = currentPos.toRelative(window.lastOffset)

        // Draw caret
        if (rpos.row in 0..<numRows && rpos.col in 0..<numCols) {
            val caretX = startX + rpos.col * cellWidth
            val caretY = margin + rpos.row * cellHeight
            // 2dp相当の幅の縦棒を描画
            canvas.drawRect(caretX, caretY, caretX + 3f, caretY + cellHeight, caretPaint)
        }

    }

    val keyMap = mapOf(
        KeyEvent.KEYCODE_A to 'a',
        KeyEvent.KEYCODE_B to 'b',
        KeyEvent.KEYCODE_C to 'c',
        KeyEvent.KEYCODE_D to 'd',
        KeyEvent.KEYCODE_E to 'e',
        KeyEvent.KEYCODE_F to 'f',
        KeyEvent.KEYCODE_G to 'g',
        KeyEvent.KEYCODE_H to 'h',
        KeyEvent.KEYCODE_I to 'i',
        KeyEvent.KEYCODE_J to 'j',
        KeyEvent.KEYCODE_K to 'k',
        KeyEvent.KEYCODE_L to 'l',
        KeyEvent.KEYCODE_M to 'm',
        KeyEvent.KEYCODE_N to 'n',
        KeyEvent.KEYCODE_O to 'o',
        KeyEvent.KEYCODE_P to 'p',
        KeyEvent.KEYCODE_Q to 'q',
        KeyEvent.KEYCODE_R to 'r',
        KeyEvent.KEYCODE_S to 's',
        KeyEvent.KEYCODE_T to 't',
        KeyEvent.KEYCODE_U to 'u',
        KeyEvent.KEYCODE_V to 'v',
        KeyEvent.KEYCODE_W to 'w',
        KeyEvent.KEYCODE_X to 'x',
        KeyEvent.KEYCODE_Y to 'y',
        KeyEvent.KEYCODE_Z to 'z',
        KeyEvent.KEYCODE_0 to '0',
        KeyEvent.KEYCODE_1 to '1',
        KeyEvent.KEYCODE_2 to '2',
        KeyEvent.KEYCODE_3 to '3',
        KeyEvent.KEYCODE_4 to '4',
        KeyEvent.KEYCODE_5 to '5',
        KeyEvent.KEYCODE_6 to '6',
        KeyEvent.KEYCODE_7 to '7',
        KeyEvent.KEYCODE_8 to '8',
        KeyEvent.KEYCODE_9 to '9',
        KeyEvent.KEYCODE_LEFT_BRACKET to '[',
        KeyEvent.KEYCODE_RIGHT_BRACKET to ']',
        KeyEvent.KEYCODE_BACKSLASH to '\\',
        KeyEvent.KEYCODE_SEMICOLON to ';',
        KeyEvent.KEYCODE_APOSTROPHE to '\'',
        KeyEvent.KEYCODE_COMMA to ',',
        KeyEvent.KEYCODE_PERIOD to '.',
        KeyEvent.KEYCODE_SLASH to '/',
        KeyEvent.KEYCODE_GRAVE to '`',
        KeyEvent.KEYCODE_MINUS to '-',
        KeyEvent.KEYCODE_EQUALS to '=',
        KeyEvent.KEYCODE_AT to '@',
    )

    val shiftKeyMap = mapOf(
        KeyEvent.KEYCODE_A to 'A',
        KeyEvent.KEYCODE_B to 'B',
        KeyEvent.KEYCODE_C to 'C',
        KeyEvent.KEYCODE_D to 'D',
        KeyEvent.KEYCODE_E to 'E',
        KeyEvent.KEYCODE_F to 'F',
        KeyEvent.KEYCODE_G to 'G',
        KeyEvent.KEYCODE_H to 'H',
        KeyEvent.KEYCODE_I to 'I',
        KeyEvent.KEYCODE_J to 'J',
        KeyEvent.KEYCODE_K to 'K',
        KeyEvent.KEYCODE_L to 'L',
        KeyEvent.KEYCODE_M to 'M',
        KeyEvent.KEYCODE_N to 'N',
        KeyEvent.KEYCODE_O to 'O',
        KeyEvent.KEYCODE_P to 'P',
        KeyEvent.KEYCODE_Q to 'Q',
        KeyEvent.KEYCODE_R to 'R',
        KeyEvent.KEYCODE_S to 'S',
        KeyEvent.KEYCODE_T to 'T',
        KeyEvent.KEYCODE_U to 'U',
        KeyEvent.KEYCODE_V to 'V',
        KeyEvent.KEYCODE_W to 'W',
        KeyEvent.KEYCODE_X to 'X',
        KeyEvent.KEYCODE_Y to 'Y',
        KeyEvent.KEYCODE_Z to 'Z',
        KeyEvent.KEYCODE_0 to ')',
        KeyEvent.KEYCODE_1 to '!',
        KeyEvent.KEYCODE_2 to '@',
        KeyEvent.KEYCODE_3 to '#',
        KeyEvent.KEYCODE_4 to '$',
        KeyEvent.KEYCODE_5 to '%',
        KeyEvent.KEYCODE_6 to '^',
        KeyEvent.KEYCODE_7 to '&',
        KeyEvent.KEYCODE_8 to '*',
        KeyEvent.KEYCODE_9 to '(',
        KeyEvent.KEYCODE_LEFT_BRACKET to '{',
        KeyEvent.KEYCODE_RIGHT_BRACKET to '}',
        KeyEvent.KEYCODE_BACKSLASH to '|',
        KeyEvent.KEYCODE_SEMICOLON to ':',
        KeyEvent.KEYCODE_APOSTROPHE to '"',
        KeyEvent.KEYCODE_COMMA to '<',
        KeyEvent.KEYCODE_PERIOD to '>',
        KeyEvent.KEYCODE_SLASH to '?',
        KeyEvent.KEYCODE_GRAVE to '~',
        KeyEvent.KEYCODE_MINUS to '_',
        KeyEvent.KEYCODE_EQUALS to '+'
    )
    val specialMap = mapOf(
        KeyEvent.KEYCODE_DPAD_LEFT to "Left",
        KeyEvent.KEYCODE_DPAD_RIGHT to "Right",
        KeyEvent.KEYCODE_DPAD_UP to "Up",
        KeyEvent.KEYCODE_DPAD_DOWN to "Down",
        KeyEvent.KEYCODE_PAGE_UP to "PageUp",
        KeyEvent.KEYCODE_PAGE_DOWN to "PageDown",
        KeyEvent.KEYCODE_ENTER to "Return",
        KeyEvent.KEYCODE_SPACE to "Space",
        KeyEvent.KEYCODE_DEL to "Delete",
        KeyEvent.KEYCODE_FORWARD_DEL to "Backspace",
        KeyEvent.KEYCODE_TAB to "Tab",
        KeyEvent.KEYCODE_ESCAPE to "Escape"
    )

    fun selfInsertKeys() : List<String> {
        return keyMap.values.map { it.toString() } + shiftKeyMap.values.map { it.toString() }
    }

    fun prefixStr(event: KeyEvent) : String {
        val isCtrl = event.isCtrlPressed
        val isAlt = event.isAltPressed
        return if (isCtrl && isAlt) { "M-C-" } else if(isCtrl) { "C-" } else if(isAlt) { "M-" } else { "" }
    }

    fun keyEventToString(keyCode: Int, event: KeyEvent): String {
        val unicodeChar = event.unicodeChar
        val isShift = event.isShiftPressed
        val prefix = prefixStr(event)
        // 文字じゃない奴は特別扱い。
        specialMap[keyCode]?.let { srep ->
            return prefix+srep
        }

        // unicodeCharはCtrlとの組み合わせがあると0になってしまう
        // だがそれ以外ならキーボードの特殊事情も正しく処理してくれるので、これがあるならこれを使う。
        if (unicodeChar != 0) {
            return prefix + unicodeChar.toChar().toString()
        }


        if (isShift) {
            val ch = shiftKeyMap[keyCode] ?: return ""
            return prefix+ch.toString()
        } else {
            val ch = keyMap[keyCode] ?: return ""
            return prefix+ch.toString()
        }
    }

    var keyDownHandler : Function1<String, Unit>? = null

    fun debPrint(keyCode: Int, event: KeyEvent) {
        /*
        val unicodeChar = event.unicodeChar
        val charStr = if (unicodeChar != 0) unicodeChar.toChar().toString() else "None"
        val isCtrl = event.isCtrlPressed
        val isShift = event.isShiftPressed
        val isAlt = event.isAltPressed
        println("onKeyDown: keyCode=$keyCode, isShift=$isShift, isCtrl=$isCtrl, isAlt=$isAlt, metaState=${event.metaState}, unichar=$charStr, ")
         */
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        debPrint(keyCode, event)

        val strRep = keyEventToString(keyCode, event)
        if (strRep.isEmpty())
            return super.onKeyDown(keyCode, event)

        keyDownHandler?.let {
            it(strRep)
        }
        return true
    }
}

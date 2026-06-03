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
import androidx.core.graphics.toColorInt

class RView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val rhinocs = Rhinocs()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 35f
        typeface = Typeface.createFromAsset(context.assets, "fonts/JetBrainsMono-Regular.ttf")
        textAlign = Paint.Align.CENTER
    }

    var fontSize: Float
        get() = textPaint.textSize
        set(value) {
            textPaint.textSize = value
            recalcRowColNum(width, height)
        }

    private val caretPaint = Paint().apply {
        color = "#4285F4".toColorInt() // Android標準に近い青色
        style = Paint.Style.FILL
    }

    private val margin = 10f

    val cellWidth: Float
        get() = textPaint.measureText("W")

    val cellHeight: Float
        get() = textPaint.fontMetrics.let { it.descent - it.ascent }

    // ascentはbaseより上、負の値で表現
    val ascent: Float
        get() = textPaint.fontMetrics.ascent

    val borderThick = 2f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalcRowColNum(w, h)
    }

    private fun recalcRowColNum(w: Int, h: Int) {
        rhinocs.numCols = ((w - margin) / cellWidth).toInt()
        // モード行とステータス行の分と、ウィンドウsplit時の境界線の分
        rhinocs.numRows = ((h - margin - borderThick) / cellHeight).toInt() - 2
    }

    fun loadFile(resolver: ContentResolver, uri: Uri) {
        rhinocs.loadFile(resolver, uri)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val topX = margin
        var topY = margin

        for (win in rhinocs.windowList) {
            topY = drawOneWin(win, topX, topY, canvas)
            drawBorder(canvas, topY)
            topY += borderThick
        }


        val modeY = (height-margin)-2*cellHeight
        topY = drawModeLine(modeY, canvas)
        drawFloatingList(canvas, modeY,rhinocs.floatingList)

        drawMiniBufferLine(topY, canvas)
    }

    private fun drawFloatingList(canvas: Canvas, fromY: Float, floatingList: FloatingList) {
        if (floatingList.isEmpty()) return

        val popupHeight = floatingList.numItems * cellHeight

        val topY = fromY - popupHeight
        
        val hMargin = margin * 3f
        val rectLeft = hMargin
        val rectRight = width.toFloat() - hMargin

        // 背景と枠線を描画
        // 影用のPaint (少しぼかしを入れる)
        val shadowPaint = Paint().apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.FILL
        }
        // 影を描画 (少しずらす)
        canvas.drawRect(rectLeft + 4f, topY + 4f, rectRight + 4f, fromY + 4f, shadowPaint)

        val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        // 枠線を少し太く、色を濃く
        val borderPaint = Paint().apply { color = Color.GRAY; style = Paint.Style.STROKE; strokeWidth = 2f }
        
        canvas.drawRect(rectLeft, topY, rectRight, fromY, bgPaint)
        canvas.drawRect(rectLeft, topY, rectRight, fromY, borderPaint)

        val startX = rectLeft + margin

        floatingList.items.forEachIndexed { index, item ->
            val itemY = topY + index * cellHeight
            val baseY = topToBase(itemY)
            val isSelected = index == floatingList.selectedIndex

            // 選択中のアイテムを反転表示
            if (isSelected) {
                val highlightPaint =
                    Paint().apply { color = "#4285F4".toColorInt(); style = Paint.Style.FILL }
                canvas.drawRect(
                    rectLeft + 1f,
                    itemY,
                    rectRight - 1f,
                    itemY + cellHeight,
                    highlightPaint
                )
            }

            canvas.withAlign(Paint.Align.LEFT) {
                if (isSelected) {
                    withColor(Color.WHITE) {
                        drawText(item, startX, baseY, textPaint)
                    }
                } else {
                    drawText(item, startX, baseY, textPaint)
                }
            }
        }
    }


    /*
        topX, topYの位置を起点にWindowを描画する。
        一番下のY座標を返す。
     */
    private fun drawOneWin(
        win: Window,
        topX: Float,
        topY: Float,
        canvas: Canvas
    ) : Float {
        val topBaseY = topToBase(topY)
        val currentPos = win.pointRowCol
        win.updateOffset(currentPos.col)

        for (row in 0..<win.numRows) {
            val linfo = win.lineInfo(row)
            for (col in 0..<win.numCols) {
                val cell = linfo[col]
                if (!cell.isEmpty) {
                    val numCells = cell.widthCount
                    // textAlign = Paint.Align.CENTER なので、中心座標を指定
                    val x = topX + (col * cellWidth) + (numCells * cellWidth / 2f)
                    val y = topBaseY + (row * cellHeight)
                    canvas.drawText(cell.ch.toString(), x, y, textPaint)
                }
            }
        }

        if(win.isSelected || win.isDrawCaret)
            drawCaret(win, topX, topY, canvas)

        return topY+win.numRows*cellHeight
    }

    private fun drawCaret(
        win: Window,
        topX: Float,
        topY: Float,
        canvas: Canvas
    ) {
        val rpos = win.pointRowCol.toRelative(win.lastOffset)

        if (rpos.row in 0..<win.numRows && rpos.col in 0..<win.numCols) {
            val caretX = topX + rpos.col * cellWidth
            val caretY = topY + rpos.row * cellHeight
            // 2dp相当の幅の縦棒を描画
            canvas.drawRect(caretX, caretY, caretX + 3f, caretY + cellHeight, caretPaint)
        }
    }

    private fun Canvas.withColor(fg: Int, block: Canvas.() -> Unit) {
        val oldFg = textPaint.color
        textPaint.color = fg
        try {
            this.block()
        } finally {
            textPaint.color = oldFg
        }
    }

    private fun Canvas.withAlign(align: Paint.Align, block: Canvas.() -> Unit) {
        val oldAlign = textPaint.textAlign
        textPaint.textAlign = align
        try {
            this.block()
        } finally {
            textPaint.textAlign = oldAlign
        }
    }

    private fun Canvas.withColorAlign(fg: Int, align: Paint.Align, block: Canvas.() -> Unit) {
        withColor(fg) {
            withAlign(align) {
                this.block()
            }
        }
    }

    private fun drawModeLine(topY: Float, canvas: Canvas) :Float {
        val bgPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
        canvas.drawRect(0f, topY, width.toFloat(), topY + cellHeight, bgPaint)

        canvas.withColorAlign(Color.WHITE, Paint.Align.RIGHT) {
            val baseY = topToBase(topY)

            val modeText = rhinocs.modeLineText
            drawText(modeText, width.toFloat() - margin, baseY, textPaint)
        }
        return topY+cellHeight
    }

    private fun drawMiniBufferLine(topY: Float, canvas: Canvas) {
        val startX = margin

        val baseY = topToBase(topY)
        val statusText = rhinocs.statusText

        // エコー領域優先
        if(statusText.isNotEmpty())
        {
            canvas.withAlign(Paint.Align.LEFT) {
                drawText(statusText, startX, baseY, textPaint)
            }
        } else {
            rhinocs.miniBufferWindow?.let {mwin->
                val prompt = mwin.miniBuffer.prompt
                canvas.withAlign(Paint.Align.LEFT) {
                    drawText(prompt, startX, baseY, textPaint)
                }

                val promptWidth = textPaint.measureText(prompt)
                val startXAfter = startX + promptWidth

                // ミニバッファのWindowのカラム数を残りの幅に合わせる
                mwin.window.numCols = calcRemainingColumn(startX, promptWidth)

                drawOneWin(mwin.window, startXAfter, topY, canvas)
            }
        }
    }

    // ascentはマイナス、topよりascentだけ下のbaselineを求めている
    private fun topToBase(topY: Float) = topY - ascent

    private fun calcRemainingColumn(startX: Float, promptWidth: Float): Int {
        val remainingWidth = width.toFloat() - startX - promptWidth
        return (remainingWidth / cellWidth).toInt().coerceAtLeast(0)
    }

    // topYからその下の2ピクセルまでの範囲で、 境界線とわずかな影を描画
    private fun drawBorder(canvas: Canvas, topY: Float) {
        val separatorPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val shadowPaint = Paint().apply { color = "#E0E0E0".toColorInt(); strokeWidth = 1f }

        // 上側の細い境界線
        canvas.drawLine(0f, topY, width.toFloat(), topY, separatorPaint)
        // そのすぐ下にさらに薄い色で影をつける
        canvas.drawLine(0f, topY + 1f, width.toFloat(), topY + 1f, shadowPaint)
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
        KeyEvent.KEYCODE_EQUALS to '+',
        KeyEvent.KEYCODE_ZENKAKU_HANKAKU to "_", // わたしのキーボードにはアンダースコアが無いのでどうせ使わない全角半角をそれにあてる。
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
        KeyEvent.KEYCODE_DEL to "Backspace",
        KeyEvent.KEYCODE_FORWARD_DEL to "Delete",
        KeyEvent.KEYCODE_TAB to "Tab",
        KeyEvent.KEYCODE_ESCAPE to "Escape",
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
        val rep = event.repeatCount
        val action = event.action
        println("keydeb: keyCode=$keyCode, isShift=$isShift, isCtrl=$isCtrl, isAlt=$isAlt, metaState=${event.metaState}, rep=${rep}, ac=${action}, unichar=$charStr, ")
         */
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let { ev->
            // some device take over C-Space. For those device, treat C-Space keyup as keydown.
            if(event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_SPACE && ev.action == KeyEvent.ACTION_UP)
            {
                val strRep = keyEventToString(keyCode, event)
                if(lastKeyRep != strRep)
                {
                    keyDownHandler?.let {
                        lastKeyRep = strRep
                        it(strRep)
                    }

                }

            }
        }
        debPrint(keyCode, event!!)
        return super.onKeyPreIme(keyCode, event)
    }

    var lastKeyRep = ""

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // debPrint(keyCode, event)

        val strRep = keyEventToString(keyCode, event)
        if (strRep.isEmpty())
            return super.onKeyDown(keyCode, event)

        keyDownHandler?.let {
            lastKeyRep = strRep
            it(strRep)
        }
        return true
    }
}

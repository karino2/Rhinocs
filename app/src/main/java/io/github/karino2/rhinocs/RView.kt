package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.util.AttributeSet
import android.view.View

class RView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

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

    var numRows: Int = 0
        private set
    var numCols: Int = 0
        private set


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        numCols = ((w - margin) / cellWidth).toInt()
        numRows = ((h - margin) / cellHeight).toInt()

        grid.setRowColNum(numRows, numCols)
    }

    val grid = Grid().apply { bufferRef = BufferRef(Buffer.fromText("Hello, Rhinocs!日本語\n二行目"), RowCol(0, 0)) }

    fun loadFile(resolver: ContentResolver, uri: Uri) {
        FastFile.fromDocUri(resolver, uri)?.let {
            grid.bufferRef = BufferRef(Buffer.fromText(it.readText()), RowCol(0, 0))
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val fm = textPaint.fontMetrics

        val startX = margin
        val startY = -fm.ascent + margin

        for (row in 0..<numRows) {
            for (col in 0..<numCols) {
                val cell = grid.getCell(row, col)
                if (!cell.isEmpty) {
                    val numCells = cell.widthCount
                    // textAlign = Paint.Align.CENTER なので、中心座標を指定
                    val x = startX + (col * cellWidth) + (numCells * cellWidth / 2f)
                    val y = startY + (row * cellHeight)
                    canvas.drawText(cell.ch.toString(), x, y, textPaint)
                }
            }
        }

        // Draw caret
        if (grid.cursorPos.row in 0..<numRows && grid.cursorPos.col in 0..<numCols) {
            val caretX = startX + grid.cursorPos.col * cellWidth
            val caretY = margin + grid.cursorPos.row * cellHeight
            // 2dp相当の幅の縦棒を描画
            canvas.drawRect(caretX, caretY, caretX + 3f, caretY + cellHeight, caretPaint)
        }
    }
}

package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.net.Uri

class Window {
    var point = Point(0, 0)
    var buffer: Buffer? = null
        set(newBuf) {
            field = newBuf

            lastOffset = RowCol(0, 0)
            point = Point(0, 0)
        }

    var lastOffset = RowCol(0, 0)

    var numRows = 0
    var numCols = 0

    fun loadFile(resolver: ContentResolver, uri: Uri) {
        FastFile.fromDocUri(resolver, uri)?.let {
            buffer = Buffer.fromText(it.readText())
        }
    }

    val lineBuilder = LineAnalyzer()

    // 現在のpointを絶対Columnに変換したもの
    val pointColumn : Int
        get(){
            buffer?.let { buf->
                return lineBuilder.pointToColumn(buf, point)
            }
            return 0
        }

    // 絶対座標でのpointをrowcolに変換したもの
    val pointRowCol : RowCol
        get() = RowCol(point.linenum, pointColumn )
    val hjump = 8

    fun updateOffset(rowCol: RowCol)  {
        val newRow = if (rowCol.row < lastOffset.row) {
            rowCol.row
        } else if (lastOffset.row+numRows <= rowCol.row) {
            rowCol.row - numRows + 1
        } else {
            lastOffset.row
        }

        val newCol = if(rowCol.col < lastOffset.col) {
            (rowCol.col / hjump)*hjump
        } else if (lastOffset.col+numCols <= rowCol.col ) {
            ((rowCol.col - numCols + hjump)/hjump)*hjump
        } else {
            lastOffset.col
        }

        lastOffset = RowCol(newRow, newCol)
    }

    fun updateOffset(newcol: Int)  {
        updateOffset(RowCol(point.linenum, newcol))
    }

    fun lineInfo(relativeRow: Int) : ArrayList<Cell> {
        val line = buffer?.let { buf->

            val linenum = relativeRow+lastOffset.row
            if(buf.numLines > linenum) {
                buf.getLine(linenum)
            } else {
                ""
            }
        } ?: ""
        return lineBuilder.buildInfo(line, lastOffset.col, numCols)
    }

    fun forwardChar(delta: Int) {
        moveCharDelta(delta)
    }

    fun backwardChar(delta: Int) {
        moveCharDelta(-delta)
    }

    fun moveCharDelta(delta: Int) {
        buffer?.let { buf->
            if(delta > 0)
                point = buf.forwardChar(point, delta)
            else
                point = buf.backwardChar(point, -delta)
        }
    }
}
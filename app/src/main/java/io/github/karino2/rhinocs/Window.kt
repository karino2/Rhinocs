package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.net.Uri
import kotlin.math.min

class Window {
    var point = Point(0, 0, 0)
    var buffer: Buffer = Buffer()
        set(newBuf) {
            field = newBuf

            lastOffset = RowCol(0, 0)
            point = Point(0, 0, 0)
        }

    var lastOffset = RowCol(0, 0)

    var numRows = 0
    var numCols = 0

    var goalColumn : Int? = null

    fun computeGoalGolumn() : Int {
        goalColumn?.let {
            return it
        }
        val col = pointColumn
        goalColumn = col
        return col
    }

    fun resetGoalGolumn() {
        goalColumn = null
    }

    val pointMax: Long
        get() = buffer.pointMax

    fun loadFile(resolver: ContentResolver, uri: Uri) {
        FastFile.fromDocUri(resolver, uri)?.let {
            buffer = Buffer.fromText(it.readText())
            buffer.url = uri
            resetGoalGolumn()
        }
    }

    fun saveBuffer(resolver: ContentResolver) : Boolean {
        return buffer.url?.let {
            FastFile.fromDocUri(resolver, it)?.let {ff->
                ff.writeText(buffer.toText())
                true
            }
        } ?: false
    }

    val lineBuilder = LineAnalyzer()

    // 現在のpointを絶対Columnに変換したもの
    val pointColumn : Int
        get(){
            return lineBuilder.pointToColumn(buffer, point)
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
        val linenum = relativeRow+lastOffset.row
        val line = if(buffer.numLines > linenum) {
            buffer.getLine(linenum)
        } else {
            ""
        }
        return lineBuilder.buildInfo(line, lastOffset.col, numCols)
    }

    fun forwardChar(delta: Int) {
        moveCharDelta(delta)
    }

    fun backwardChar(delta: Int) {
        moveCharDelta(-delta)
    }

    fun insert(content: String) {
        point = buffer.insert(point, content)
        resetGoalGolumn()
    }

    // 削除した文字数を返す
    fun deleteRegion(from: Long, to: Long) : Long {
        val count = buffer.deleteRegion(from, to)
        // pointの補正
        if (count > 0 && point.point > from) {
            // 削除の範囲内ならfromにする
            if(point.point < from+count)
                point = buffer.toPoint(from)
            else // 削除した範囲よりも外側ならcount分だけ前にずらす
                point = buffer.toPoint(point.point - count)
        }
        return count
    }

    fun moveCharDelta(delta: Int) {
        resetGoalGolumn()

        if(delta > 0)
            point = buffer.forwardChar(point, delta)
        else
            point = buffer.backwardChar(point, -delta)
    }

    fun moveLineDelta(delta: Int) {
        resetGoalGolumn()

        if(delta > 0)
            point = buffer.forwardLine(point, delta)
        else
            point = buffer.backwardLine(point, -delta)
    }

    fun pontToColumn(pos: Long): Int {
        val pt = buffer.toPoint(pos)
        return lineBuilder.pointToColumn(buffer, pt)
    }

    // 実際に移動出来たカラム を返す。行末かcolumnかcolumn下が全角の右側ならcolumn-1を返す。
    fun gotoColumn(column: Int): Int {
        if (column == 0)
            return column
        val targetLine = buffer.getLine(point.linenum)
        val maxCol = lineBuilder.maxColumn(targetLine)
        if (maxCol < column) {
            point = buffer.toPoint(point.linenum, targetLine.length)
            return maxCol
        }
        val info = lineBuilder.buildInfo(targetLine, 0, column+1)
        val actual = if (info[column].isEmpty) { column - 1 } else { column }
        val offset = lineBuilder.columnToOffset(targetLine, actual)
        point = buffer.toPoint(point.linenum, offset)
        return actual
    }

    fun gotoChar(pos: Long) {
        resetGoalGolumn()
        point = buffer.toPoint(pos)
    }
}
package io.github.karino2.rhinocs

import android.content.ContentResolver

class Window(initBuf: Buffer) {
    fun isEOL() = buffer.isEOL(point)

    fun isBOL() = point.offset == 0

    var isMiniBuffer = false

    var point = Point(0, 0, 0)
    var buffer: Buffer = initBuf
        set(newBuf) {
            field = newBuf

            lastOffset = RowCol(0, 0)
            point = Point(0, 0, 0)
            resetGoalGolumn()
        }

    var isSelected = true
    // isearchなどでミニバッファをアクティブにしつつキャレットを描きたい場合
    var isDrawCaret = false

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
        get() = buffer.positionMax

    fun saveBuffer(resolver: ContentResolver) = buffer.save(resolver)

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

    fun insert(content: String) {
        // val debPrev = point
        point = buffer.insert(point, content)
        resetGoalGolumn()
        // println("insert: $debPrev, ${point}, $content")
    }

    // 削除した文字数を返す
    fun deleteRegion(from: Long, to: Long) : Long {
        val count = buffer.deleteRegion(from, to)
        val debPrev = point
        // pointの補正
        if (count > 0 && point.position > from) {
            // 削除の範囲内ならfromにする
            if(point.position < from+count)
                point = buffer.toPoint(from)
            else // 削除した範囲よりも外側ならcount分だけ前にずらす
                point = buffer.toPoint(point.position - count)
        }
        // println("debDelRegion: ($from, $to): $debPrev, ${point}, $count")
        return count
    }

    fun moveCharDelta(delta: Int) {
        resetGoalGolumn()
        // val debPrev = point

        if(delta > 0)
            point = buffer.forwardChar(point, delta)
        else
            point = buffer.backwardChar(point, -delta)
        // println("moveChar: $debPrev, ${point}, $delta")
    }

    fun moveLineDelta(delta: Int) {
        // val debPrev = point
        resetGoalGolumn()

        if(delta > 0)
            point = buffer.forwardLine(point, delta)
        else
            point = buffer.backwardLine(point, -delta)
        // println("moveLineDelta: $debPrev, ${point}, $delta")
    }

    fun pontToColumn(pos: Long): Int {
        val pt = buffer.toPoint(pos)
        return lineBuilder.pointToColumn(buffer, pt)
    }

    // 実際に移動出来たカラム を返す。行末かcolumnかcolumn下が全角の右側ならcolumn-1を返す。
    fun gotoColumn(column: Int): Int {
        // val debPrev = point
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
        // println("gotoColumn: $debPrev, ${point}, $column, $actual")
        return actual
    }

    fun gotoChar(pos: Long) {
        resetGoalGolumn()
        point = buffer.toPoint(pos)
    }

    fun scrollWindow(delta: Int) : Boolean{
        val goalLine = (point.linenum+delta).coerceAtLeast(0)
        if (goalLine == point.linenum)
            return false

        if (goalLine > buffer.numLines)
            return false;

        lastOffset = lastOffset.copy(row=goalLine)
        moveLineDelta(delta)
        gotoColumn(computeGoalGolumn())
        return true
    }

    fun gotoBol() {
        resetGoalGolumn()
        point = buffer.gotoBol(point)
    }

    fun gotoEol() {
        resetGoalGolumn()
        point = buffer.gotoEol(point)
    }
}
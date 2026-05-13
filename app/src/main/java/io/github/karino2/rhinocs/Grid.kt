package io.github.karino2.rhinocs

enum class CellType {
    EMPTY, HALF, FULL
}
data class Cell(val ch: Char, val ctype: CellType) {
    val isEmpty : Boolean
        get() = ctype == CellType.EMPTY

    val widthCount : Int
        get() = when(ctype) {
                CellType.EMPTY->0
                CellType.HALF->1
                CellType.FULL->2
            }

    companion object {
        val empty = Cell(' ', CellType.EMPTY)
        fun full(c : Char) = Cell(c, CellType.FULL)
        fun half(c: Char) = Cell(c, CellType.HALF)
        fun char(c: Char) = if(isFullWidth(c)) { full(c) } else { half(c) }

        fun isHalfWidth(c: Char): Boolean {
            return (c in '\u0000'..'\u007e') || (c in '\uff61'..'\uff9f')
        }

        fun isFullWidth(c: Char) = !isHalfWidth(c)
    }
}

data class BufferRef(val buffer: Buffer, val offsetRow: Int, val offsetCol: Int) {
    fun getLine(row: Int) = buffer.lines[row+offsetRow]
    fun isInside(row: Int) = row+offsetRow < buffer.numRows
}

class Grid {
    var numRows = 0
    var numCols = 0

    val cells = ArrayList<Cell>()
    var bufferRef : BufferRef? = null
        set(value) {
            field = value
            updateGrid()
        }

    fun setCell(row: Int, col:Int, cell: Cell) {
        cells[row*numCols+col] = cell
    }

    fun getCell(row: Int, col:Int) = cells[row*numCols+col]

    fun isFullWidth(c: Char) = Cell.isFullWidth(c)
    fun isHalfWidth(c: Char) = Cell.isHalfWidth(c)

    fun updateGrid() {
        cells.clear()
        for(i in 0..<numRows*numCols) {
            cells.add(Cell.empty)
        }
        bufferRef?.let { bref->
            for(row in 0..<numRows) {
                if (!bref.isInside(row)) return

                val line = bref.getLine(row)
                if (line.length <= bref.offsetCol)
                    continue;


                var spos = 0
                var nextEmpty = false
                for(gcol in 0..<bref.offsetCol+numCols) {
                    if (spos >= line.length)
                        break

                    if (nextEmpty) {
                        nextEmpty = false
                        continue
                    }

                    val ch = line[spos]
                    if (isFullWidth(ch)){
                        nextEmpty = true
                    }

                    if (gcol >= bref.offsetCol)
                    {
                        setCell(row, gcol - bref.offsetCol, Cell.char(ch))
                    }
                    spos += 1
                }
            }
        }
    }

    fun setRowColNum(row: Int, col: Int) {
        numRows = row
        numCols = col
        updateGrid()
    }
}
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

        fun isFullWidth(c: Char): Boolean {
            return (c in '\u1100'..'\u11ff') || // Hangul Jamo
                    (c in '\u2e80'..'\u9fff') || // CJK Radicals, Symbols, Hiragana, Katakana, Ideographs
                    (c in '\uac00'..'\ud7af') || // Hangul Syllables
                    (c in '\uf900'..'\ufaff') || // CJK Compatibility Ideographs
                    (c in '\ufe30'..'\ufe4f') || // CJK Compatibility Forms
                    (c in '\uff01'..'\uff60') || // Fullwidth ASCII variants
                    (c in '\uffe0'..'\uffe6')    // Fullwidth Symbol variants
        }

        fun isHalfWidth(c: Char) = !isFullWidth(c)
    }
}

data class BufferRef(val buffer: Buffer, var offset: RowCol) {
    fun getLine(row: Int) = buffer.lines[row + offset.row]
    fun isInside(row: Int) = row + offset.row < buffer.numRows
}

class Grid {
    var numRows = 0
    var numCols = 0

    var cursorPos = RowCol(0, 0)
    
    val cells = ArrayList<Cell>()
    var bufferRef : BufferRef? = null
        set(value) {
            field = value
            updateGrid()
        }

    fun setCell(row: Int, col: Int, cell: Cell) {
        cells[row * numCols + col] = cell
    }

    fun getCell(row: Int, col: Int) = cells[row * numCols + col]

    fun isFullWidth(c: Char) = Cell.isFullWidth(c)
    fun isHalfWidth(c: Char) = Cell.isHalfWidth(c)

    fun updateGrid() {
        cells.clear()
        for(i in 0..<numRows*numCols) {
            cells.add(Cell.empty)
        }
        bufferRef?.let { bref ->
            for (row in 0..<numRows) {
                if (!bref.isInside(row)) return

                val line = bref.getLine(row)
                if (line.length <= bref.offset.col)
                    continue

                var spos = 0
                var nextEmpty = false
                for (gcol in 0..<bref.offset.col + numCols) {
                    if (spos >= line.length)
                        break

                    if (nextEmpty) {
                        nextEmpty = false
                        continue
                    }

                    val ch = line[spos]
                    if (isFullWidth(ch)) {
                        nextEmpty = true
                    }

                    if (gcol >= bref.offset.col) {
                        setCell(row, gcol - bref.offset.col, Cell.char(ch))
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

    val offset: RowCol
        get() = bufferRef?.offset ?: RowCol(0, 0)

    fun setOffset(newOffset: RowCol) {
        bufferRef?.let {
            it.offset = newOffset
            updateGrid()
        }
    }
}

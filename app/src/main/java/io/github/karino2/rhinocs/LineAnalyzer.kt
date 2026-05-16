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

class LineAnalyzer {
    fun offsetToColumn(buf: Buffer, linenum: Int, offset: Int) : Int {
        val line = buf.getLine(linenum)
        return offsetToColumn(line, offset)

    }

    fun offsetToColumn(line: String, offset: Int): Int {
        return line.take(offset).map {
            charWidth(it)
        }.sum()
    }

    private fun charWidth(ch: Char): Int = if (isFullWidth(ch)) { 2 } else { 1 }

    // columnを最初に越えたoffsetを返す。
    fun columnToOffset(line: String, column: Int): Int {
        var sum = 0
        line.forEachIndexed { index, ch ->
            if (sum >= column)
                return index
            sum += charWidth(ch)
        }
        if (sum >= column)
            return line.length
        throw IndexOutOfBoundsException("column is out of bounds")
    }

    // その行の右端のカラムを返す。
    // maxColumnと同じカラムは有効。
    fun maxColumn(buf: Buffer, linenum: Int) : Int {
        val line = buf.getLine(linenum)
        return maxColumn(line)
    }

    fun maxColumn(line: String): Int {
        return offsetToColumn(line, line.length)
    }

    fun pointToColumn(buf: Buffer, point: Point) = offsetToColumn(buf, point.linenum, point.offset)

    fun buildInfo(line: String, colOffset: Int, numCols: Int) : ArrayList<Cell> {
        val cells = ArrayList<Cell>()
        for(i in 0..<numCols) {
            cells.add(Cell.empty)
        }

        var spos = 0
        var nextEmpty = false
        for (gcol in 0..<colOffset + numCols) {
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

            if (gcol >= colOffset) {
                cells[gcol - colOffset] = Cell.char(ch)
            }
            spos += 1
        }
        return cells
    }

    fun isFullWidth(c: Char) = Cell.isFullWidth(c)
}


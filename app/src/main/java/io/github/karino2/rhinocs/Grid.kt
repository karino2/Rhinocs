package io.github.karino2.rhinocs

import kotlin.collections.get

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

class GridLineBuilder {
    fun pointToColumn(buf: Buffer, point: Point) : Int {
        val line = buf.getLine(point.linenum)
        return line.take(point.offset).map { if(isFullWidth(it)) { 2 } else { 1 } }.sum()
    }

    fun build(line: String, colOffset: Int, numCols: Int) : ArrayList<Cell> {
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


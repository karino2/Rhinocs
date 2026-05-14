package io.github.karino2.rhinocs

data class RowCol(val row: Int, val col: Int) {
    fun toRelative(offset: RowCol) : RowCol {
        return RowCol(row-offset.row, col-offset.col)
    }
}

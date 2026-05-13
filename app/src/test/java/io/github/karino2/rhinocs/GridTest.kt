package io.github.karino2.rhinocs

import org.junit.Test

import org.junit.Assert.*

class GridTest {
    fun create(str: String) : Grid {
        return Grid().apply {
            setRowColNum(5, 5)
            bufferRef = BufferRef(Buffer.fromText(str), RowCol(0, 0))
        }
    }

    @Test
    fun isFullWidth_Hankaku() {
        assertFalse(Cell.isFullWidth(' '))
        assertFalse(Cell.isFullWidth('a'))
        assertFalse(Cell.isFullWidth('ｱ'))
    }

    @Test
    fun isFullWidth_Zenkaku() {
        assertTrue(Cell.isFullWidth('あ'))
        assertTrue(Cell.isFullWidth('亜'))
        assertTrue(Cell.isFullWidth('ア'))
        assertTrue(Cell.isFullWidth('　'))
    }


    @Test
    fun empty_isCorrect() {
        val target = create("")
        val actual = target.getCell(0, 0)
        assertTrue(actual.isEmpty)
    }

    @Test
    fun updateGrid_roman() {
        val target = create("abc")
        assertEquals(CellType.HALF, target.getCell(0, 0).ctype)
        assertEquals(CellType.HALF, target.getCell(0, 1).ctype)
        assertEquals(CellType.HALF, target.getCell(0, 2).ctype)
    }

    @Test
    fun updateGrid_zenkaku() {
        val target = create("aあb")
        assertEquals(CellType.HALF, target.getCell(0, 0).ctype)
        assertEquals(CellType.FULL, target.getCell(0, 1).ctype)
        assertEquals(CellType.EMPTY, target.getCell(0, 2).ctype)
        assertEquals(CellType.HALF, target.getCell(0, 3).ctype)
    }

    @Test
    fun updateGrid_offset() {
        val target = create("あab")
        target.setOffset(RowCol(0, 1))
        assertEquals(CellType.EMPTY, target.getCell(0, 0).ctype)
        assertEquals(CellType.HALF, target.getCell(0, 1).ctype)
        assertEquals(CellType.HALF, target.getCell(0, 2).ctype)
        assertEquals(CellType.EMPTY, target.getCell(0, 3).ctype)
    }
}
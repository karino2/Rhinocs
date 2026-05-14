package io.github.karino2.rhinocs

import org.junit.Test

import org.junit.Assert.*

class LinaAnalyzerTest {
    val builder = LineAnalyzer()
    fun create(str: String, offset: Int, numCol: Int) : ArrayList<Cell> {
        return builder.buildInfo(str, offset, numCol)
    }

    fun create(str: String) : ArrayList<Cell> {
        return create(str, 0, 5)
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
        val actual = target[0]
        assertTrue(actual.isEmpty)
    }

    @Test
    fun updateGrid_roman() {
        val target = create("abc")
        assertEquals(CellType.HALF, target[0].ctype)
        assertEquals(CellType.HALF, target[1].ctype)
        assertEquals(CellType.HALF, target[2].ctype)
    }

    @Test
    fun updateGrid_zenkaku() {
        val target = create("aあb")
        assertEquals(CellType.HALF, target[0].ctype)
        assertEquals(CellType.FULL, target[1].ctype)
        assertEquals(CellType.EMPTY, target[2].ctype)
        assertEquals(CellType.HALF, target[3].ctype)
    }

    @Test
    fun updateGrid_offset() {
        val target = create("あab", 1, 5)
        assertEquals(CellType.EMPTY, target[0].ctype)
        assertEquals(CellType.HALF, target[1].ctype)
        assertEquals(CellType.HALF, target[2].ctype)
        assertEquals(CellType.EMPTY, target[3].ctype)
    }

    @Test
    fun updateGrid_offsetZenkaku() {
        val target = create("ああa", 4, 5)
        assertEquals(CellType.HALF, target[0].ctype)
    }
}
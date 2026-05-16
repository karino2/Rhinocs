package io.github.karino2.rhinocs

import org.junit.Test

import org.junit.Assert.*

class BufferTest {
    @Test
    fun basic() {
        val buf = Buffer.fromText("First line\nSecond line")
        assertEquals(2, buf.lines.size)
        assertEquals("First line", buf.lines[0].toString())
        assertEquals("Second line", buf.lines[1].toString())
    }

    @Test
    fun load_emptyLastLine() {
        val buf = Buffer.fromText("First line\nSecond line\n")
        assertEquals(3, buf.lines.size)
        assertTrue(buf.lines[2].toString().isEmpty())
    }

    @Test
    fun insert_basic() {
        val buf = Buffer.fromText("abc\ndef")
        buf.insert(buf.toPoint(1), "ghi")
        assertEquals("aghibc", buf.getLine(0))
        assertEquals("def", buf.getLine(1))
    }

    @Test
    fun insert_newLineInside() {
        val buf = Buffer.fromText("abc\ndef")
        buf.insert(buf.toPoint(1), "gh\ni")
        assertEquals("agh", buf.getLine(0))
        assertEquals("ibc", buf.getLine(1))
        assertEquals("def", buf.getLine(2))
    }
}
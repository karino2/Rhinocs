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

    @Test
    fun deleteRegion_basic() {
        val buf = Buffer.fromText("abc\ndef")
        val count = buf.deleteRegion(1, 2)
        assertEquals(1, count)
        assertEquals("ac", buf.getLine(0))
        assertEquals("def", buf.getLine(1))
    }

    @Test
    fun deleteRegion_deleteEOL_concat() {
        val buf = Buffer.fromText("abc\ndef")
        val count = buf.deleteRegion(3, 4)
        assertEquals(1, count)
        assertEquals("abcdef", buf.getLine(0))
        assertEquals(1, buf.numLines)
    }

    @Test
    fun deleteRegion_multiLines() {
        // 012 3 456 7 890
        // abc \n def \n ghi
        val buf = Buffer.fromText("abc\ndef\nghi")
        // "c\ndef\ng" を削除 (2から9まで)
        val count = buf.deleteRegion(2, 9)
        assertEquals(7, count)
        assertEquals("abhi", buf.getLine(0))
        assertEquals(1, buf.numLines)
    }

    @Test
    fun toPoint_basic() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.toPoint(1)
        assertEquals(Point(0, 1, 1), actual)
    }

    @Test
    fun toPoint_eol() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.toPoint(3)
        assertEquals(Point(0, 3, 3), actual)
    }

    @Test
    fun toPoint_secondLine() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.toPoint(4)
        assertEquals(Point(1, 0, 4), actual)
    }

    @Test
    fun toPoint_linenumarg_secondLine() {
        val buf = Buffer.fromText("abc\ndef")
        val pt = buf.toPoint(1, 0)
        assertEquals(4, pt.point)
    }

    @Test
    fun pointMax_empty() {
        val buf = Buffer()
        assertEquals(0, buf.pointMax)
    }

    @Test
    fun pointMax_oneLine() {
        val buf = Buffer.fromText("abc")
        assertEquals(3, buf.pointMax)
    }

    @Test
    fun pointMax_twoLine() {
        val buf = Buffer.fromText("abc\ndef")
        assertEquals(7, buf.pointMax)
    }

    @Test
    fun forwardChar_basicL() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.forwardChar(buf.toPoint(1), 1)
        assertEquals(0, actual.linenum)
        assertEquals(2, actual.offset)
    }

    @Test
    fun forwardChar_canMoveToEOL() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.forwardChar(buf.toPoint(2), 1)
        assertEquals(0, actual.linenum)
        assertEquals(3, actual.offset)
    }

    @Test
    fun forwardChar_eolToNextLine() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.forwardChar(buf.toPoint(3), 1)
        assertEquals(1, actual.linenum)
        assertEquals(0, actual.offset)
        assertEquals(4, actual.point)
    }

    @Test
    fun forwardChar_eob() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.forwardChar(buf.toPoint(7), 1)
        assertEquals(1, actual.linenum)
        assertEquals(3, actual.offset)
        assertEquals(7, actual.point)
    }

    @Test
    fun backwardChar_basicL() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.backwardChar(buf.toPoint(2), 1)
        assertEquals(0, actual.linenum)
        assertEquals(1, actual.offset)
    }

    @Test
    fun backwardChar_canMoveToBOL() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.backwardChar(buf.toPoint(5), 1)
        assertEquals(1, actual.linenum)
        assertEquals(0, actual.offset)
    }

    @Test
    fun backwardChar_bollToPrevLine() {
        val buf = Buffer.fromText("abc\ndef")
        val actual = buf.backwardChar(buf.toPoint(4), 1)
        assertEquals(0, actual.linenum)
        assertEquals(3, actual.offset)
    }

    @Test
    fun marker_baisc() {
        val buf = Buffer.fromText("abc\ndef")
        buf.mark.position = 5
        assertEquals(5, buf.mark.position)

    }

    @Test
    fun marker_setOutsizeBound() {
        val buf = Buffer.fromText("abc\ndef")
        buf.mark.position = 8
        assertEquals(7, buf.mark.position)
    }

    @Test
    fun marker_insertAfter_noChange() {
        val buf = Buffer.fromText("abc\ndef")
        buf.mark.position = 2
        buf.insert(buf.toPoint(5), "zzz")
        assertEquals(2, buf.mark.position)
    }

    @Test
    fun marker_insertBefore_shift() {
        val buf = Buffer.fromText("abc\ndef")
        buf.mark.position = 2
        buf.insert(buf.toPoint(1), "zzz")
        assertEquals(5, buf.mark.position)
    }

    @Test
    fun marker_deleteAfter_noChange() {
        val buf = Buffer.fromText("abc\ndef")
        buf.mark.position = 4
        buf.deleteRegion(5, 6)
        assertEquals(4, buf.mark.position)
    }

    @Test
    fun marker_deleteBefore_shift() {
        val buf = Buffer.fromText("abc\ndef")
        buf.mark.position = 4
        buf.deleteRegion(1, 2)
        assertEquals(3, buf.mark.position)
    }

    @Test
    fun marker_deleteBetween_shiftToFrom() {
        val buf = Buffer.fromText("abc\ndef")
        buf.mark.position = 4
        buf.deleteRegion(2, 5)
        assertEquals(2, buf.mark.position)
    }

    @Test
    fun substring_basic() {
        val buf = Buffer.fromText("abcdefg")
        val actual = buf.substring(1, 3)
        assertEquals("bc", actual)
    }

    @Test
    fun substring_bol() {
        val buf = Buffer.fromText("abcdefg")
        val actual = buf.substring(0, 3)
        assertEquals("abc", actual)
    }

    @Test
    fun substring_diffLine() {
        val buf = Buffer.fromText("ab\ncd")
        val actual = buf.substring(1, 4)
        assertEquals("b\nc", actual)
    }
    @Test
    fun substring_diffLineBetween() {
        val buf = Buffer.fromText("ab\ncd\nef")
        val actual = buf.substring(1, 7)
        assertEquals("b\ncd\ne", actual)
    }
}
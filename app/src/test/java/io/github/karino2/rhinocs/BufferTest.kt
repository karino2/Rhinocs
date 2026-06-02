package io.github.karino2.rhinocs

import org.junit.Test
import org.junit.Assert.*

class BufferTest {

    fun BufferFromText(text: String) = Buffer().apply { load(text) }

    @Test
    fun basic() {
        val buf = BufferFromText("First line\nSecond line")
        assertEquals(2, buf.lines.size)
        assertEquals("First line", buf.lines[0].toString())
        assertEquals("Second line", buf.lines[1].toString())
    }

    @Test
    fun load_emptyLastLine() {
        val buf = BufferFromText("First line\nSecond line\n")
        assertEquals(3, buf.lines.size)
        assertTrue(buf.lines[2].toString().isEmpty())
    }

    @Test
    fun insert_basic() {
        val buf = BufferFromText("abc\ndef")
        buf.insert(buf.toPoint(1), "ghi")
        assertEquals("aghibc", buf.getLine(0))
        assertEquals("def", buf.getLine(1))
    }

    @Test
    fun insert_newLineInside() {
        val buf = BufferFromText("abc\ndef")
        buf.insert(buf.toPoint(1), "gh\ni")
        assertEquals("agh", buf.getLine(0))
        assertEquals("ibc", buf.getLine(1))
        assertEquals("def", buf.getLine(2))
    }

    @Test
    fun deleteRegion_basic() {
        val buf = BufferFromText("abc\ndef")
        val count = buf.deleteRegion(1, 2)
        assertEquals(1, count)
        assertEquals("ac", buf.getLine(0))
        assertEquals("def", buf.getLine(1))
    }

    @Test
    fun deleteRegion_deleteEOL_concat() {
        val buf = BufferFromText("abc\ndef")
        val count = buf.deleteRegion(3, 4)
        assertEquals(1, count)
        assertEquals("abcdef", buf.getLine(0))
        assertEquals(1, buf.numLines)
    }

    @Test
    fun deleteRegion_multiLines() {
        // 012 3 456 7 890
        // abc \n def \n ghi
        val buf = BufferFromText("abc\ndef\nghi")
        // "c\ndef\ng" を削除 (2から9まで)
        val count = buf.deleteRegion(2, 9)
        assertEquals(7, count)
        assertEquals("abhi", buf.getLine(0))
        assertEquals(1, buf.numLines)
    }

    @Test
    fun toPoint_basic() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.toPoint(1)
        assertEquals(Point(0, 1, 1), actual)
    }

    @Test
    fun toPoint_eol() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.toPoint(3)
        assertEquals(Point(0, 3, 3), actual)
    }

    @Test
    fun toPoint_secondLine() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.toPoint(4)
        assertEquals(Point(1, 0, 4), actual)
    }

    @Test
    fun toPoint_linenumarg_secondLine() {
        val buf = BufferFromText("abc\ndef")
        val pt = buf.toPoint(1, 0)
        assertEquals(4, pt.position)
    }

    @Test
    fun pointMax_empty() {
        val buf = Buffer()
        assertEquals(0, buf.positionMax)
    }

    @Test
    fun pointMax_oneLine() {
        val buf = BufferFromText("abc")
        assertEquals(3, buf.positionMax)
    }

    @Test
    fun pointMax_twoLine() {
        val buf = BufferFromText("abc\ndef")
        assertEquals(7, buf.positionMax)
    }

    @Test
    fun forwardChar_basicL() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.forwardChar(buf.toPoint(1), 1)
        assertEquals(0, actual.linenum)
        assertEquals(2, actual.offset)
    }

    @Test
    fun forwardChar_canMoveToEOL() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.forwardChar(buf.toPoint(2), 1)
        assertEquals(0, actual.linenum)
        assertEquals(3, actual.offset)
    }

    @Test
    fun forwardChar_eolToNextLine() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.forwardChar(buf.toPoint(3), 1)
        assertEquals(1, actual.linenum)
        assertEquals(0, actual.offset)
        assertEquals(4, actual.position)
    }

    @Test
    fun forwardChar_eob() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.forwardChar(buf.toPoint(7), 1)
        assertEquals(1, actual.linenum)
        assertEquals(3, actual.offset)
        assertEquals(7, actual.position)
    }

    @Test
    fun backwardChar_basicL() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.backwardChar(buf.toPoint(2), 1)
        assertEquals(0, actual.linenum)
        assertEquals(1, actual.offset)
    }

    @Test
    fun backwardChar_canMoveToBOL() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.backwardChar(buf.toPoint(5), 1)
        assertEquals(1, actual.linenum)
        assertEquals(0, actual.offset)
    }

    @Test
    fun backwardChar_bollToPrevLine() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.backwardChar(buf.toPoint(4), 1)
        assertEquals(0, actual.linenum)
        assertEquals(3, actual.offset)
    }

    @Test
    fun marker_baisc() {
        val buf = BufferFromText("abc\ndef")
        buf.mark.position = 5
        assertEquals(5, buf.mark.position)
    }

    @Test
    fun marker_setOutsizeBound() {
        val buf = BufferFromText("abc\ndef")
        buf.mark.position = 8
        assertEquals(7, buf.mark.position)
    }

    @Test
    fun marker_insertAfter_noChange() {
        val buf = BufferFromText("abc\ndef")
        buf.mark.position = 2
        buf.insert(buf.toPoint(5), "zzz")
        assertEquals(2, buf.mark.position)
    }

    @Test
    fun marker_insertBefore_shift() {
        val buf = BufferFromText("abc\ndef")
        buf.mark.position = 2
        buf.insert(buf.toPoint(1), "zzz")
        assertEquals(5, buf.mark.position)
    }

    @Test
    fun marker_deleteAfter_noChange() {
        val buf = BufferFromText("abc\ndef")
        buf.mark.position = 4
        buf.deleteRegion(5, 6)
        assertEquals(4, buf.mark.position)
    }

    @Test
    fun marker_deleteBefore_shift() {
        val buf = BufferFromText("abc\ndef")
        buf.mark.position = 4
        buf.deleteRegion(1, 2)
        assertEquals(3, buf.mark.position)
    }

    @Test
    fun marker_deleteBetween_shiftToFrom() {
        val buf = BufferFromText("abc\ndef")
        buf.mark.position = 4
        buf.deleteRegion(2, 5)
        assertEquals(2, buf.mark.position)
    }

    @Test
    fun substring_basic() {
        val buf = BufferFromText("abcdefg")
        val actual = buf.substring(1, 3)
        assertEquals("bc", actual)
    }

    @Test
    fun substring_bol() {
        val buf = BufferFromText("abcdefg")
        val actual = buf.substring(0, 3)
        assertEquals("abc", actual)
    }

    @Test
    fun substring_diffLine() {
        val buf = BufferFromText("ab\ncd")
        val actual = buf.substring(1, 4)
        assertEquals("b\nc", actual)
    }

    @Test
    fun substring_diffLineBetween() {
        val buf = BufferFromText("ab\ncd\nef")
        val actual = buf.substring(1, 7)
        assertEquals("b\ncd\ne", actual)
    }

    @Test
    fun searchForward_basic() {
        val buf = BufferFromText("abcde")
        val actual = buf.searchForward(buf.toPoint(0), "cd")
        assertEquals(buf.toPoint(2), actual)
    }

    @Test
    fun searchForward_secondLine() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.searchForward(buf.toPoint(0), "de")
        assertEquals(buf.toPoint(4), actual)
    }

    @Test
    fun searchForward_fromMiddle() {
        val buf = BufferFromText("abcabc")
        // Start search after first 'abc'
        val actual = buf.searchForward(buf.toPoint(1), "abc")
        assertEquals(buf.toPoint(3), actual)
    }

    @Test
    fun searchForward_notFound() {
        val buf = BufferFromText("abc")
        val actual = buf.searchForward(buf.toPoint(0), "z")
        assertNull(actual)
    }

    @Test
    fun searchForward_empty() {
        val buf = BufferFromText("abc")
        val start = buf.toPoint(1)
        val actual = buf.searchForward(start, "")
        assertEquals(start, actual)
    }

    @Test
    fun searchBackward_basic() {
        val buf = BufferFromText("abcde")
        val actual = buf.searchBackward(buf.toPoint(5), "cd")
        assertEquals(buf.toPoint(2), actual)
    }

    @Test
    fun searchBackward_prevLine() {
        val buf = BufferFromText("abc\ndef")
        val actual = buf.searchBackward(buf.toPoint(7), "bc")
        assertEquals(buf.toPoint(1), actual)
    }

    @Test
    fun searchBackward_sameLineMultiple() {
        val buf = BufferFromText("abcabc")
        val actual = buf.searchBackward(buf.toPoint(6), "abc")
        assertEquals(buf.toPoint(3), actual)
    }

    @Test
    fun searchBackward_notFound() {
        val buf = BufferFromText("abc")
        val actual = buf.searchBackward(buf.toPoint(3), "z")
        assertNull(actual)
    }

    @Test
    fun searchForward_limit() {
        val buf = BufferFromText("abcabc")
        val actual1 = buf.searchForward(buf.toPoint(0), "abc", 2)
        assertEquals(buf.toPoint(0), actual1)

        val actual2 = buf.searchForward(buf.toPoint(1), "abc", 2)
        assertNull(actual2)
    }

    @Test
    fun searchBackward_limit() {
        val buf = BufferFromText("abcabc")
        val actual1 = buf.searchBackward(buf.toPoint(6), "abc", 3)
        assertEquals(buf.toPoint(3), actual1)

        val actual2 = buf.searchBackward(buf.toPoint(6), "abc", 4)
        assertNull(actual2)
    }
}

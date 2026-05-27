package io.github.karino2.rhinocs

import org.junit.Test

import org.junit.Assert.*

class BufferCollectionTest {

    @Test
    fun newBuffer_basic() {
        val target = BufferCollection()
        val buf = target.newBuffer("test")
        assertEquals("test", buf.name)
    }

    @Test
    fun newBuffer_conflict() {
        val target = BufferCollection()
        val buf = target.newBuffer("test")
        val buf2 = target.newBuffer("test")
        val buf3 = target.newBuffer("test")
        assertEquals("test", buf.name)
        assertEquals("test-1", buf2.name)
        assertEquals("test-2", buf3.name)
    }

    @Test
    fun newBuffer_clear_conflict() {
        val target = BufferCollection()
        val buf = target.newBuffer("test")
        target.clear()
        val buf2 = target.newBuffer("test")
        assertEquals("test", buf.name)
        assertEquals("test", buf2.name)
    }

    @Test
    fun getBufferCreate_differentName_returnDifferent() {
        val target = BufferCollection()
        val buf = target.getBufferCreate("test")
        assertEquals("test", buf.name)
        val buf2 = target.getBufferCreate("test2")
        assertEquals("test2", buf2.name)
        assertNotSame(buf, buf2)
    }

    @Test
    fun getBufferCreate_sameName_returnSameBuf() {
        val target = BufferCollection()
        val buf = target.getBufferCreate("test")
        val buf2 = target.getBufferCreate("test")
        assertEquals(buf, buf2)

    }
}
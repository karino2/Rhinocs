package io.github.karino2.rhinocs

import org.junit.Test
import org.junit.Assert.*

class DiffTest {
    @Test
    fun calculate_noChange() {
        val old = listOf("a", "b", "c")
        val new = listOf("a", "b", "c")
        val diff = Diff(old, new)
        val ops = diff.calculate()
        assertTrue(ops.isEmpty())
    }

    @Test
    fun calculate_insert() {
        val old = listOf("a", "c")
        val new = listOf("a", "b", "c")
        val diff = Diff(old, new)
        val ops = diff.calculate()
        
        assertEquals(1, ops.size)
        assertEquals(DiffOp.Insert(1, 1,1), ops[0])
    }

    @Test
    fun calculate_remove() {
        val old = listOf("a", "b", "c")
        val new = listOf("a", "c")
        val diff = Diff(old, new)
        val ops = diff.calculate()
        
        assertEquals(1, ops.size)
        assertEquals(DiffOp.Remove(1, 1), ops[0])
    }

    @Test
    fun calculate_replace() {
        val old = listOf("a", "b", "c")
        val new = listOf("a", "d", "c")
        val diff = Diff(old, new)
        val ops = diff.calculate()
        
        // DiffUtil handles replace as Remove + Insert
        assertEquals(2, ops.size)
        // Order depends on DiffUtil implementation, but usually Remove then Insert for the same position
        assertTrue(ops.contains(DiffOp.Remove(1, 1)))
        assertTrue(ops.contains(DiffOp.Insert(1, 1,1)))
    }

    @Test
    fun calculate_multipleOps() {
        val old = listOf("L1", "L2", "L3")
        val new = listOf("L0", "L1", "L3", "L4")
        val diff = Diff(old, new)
        val ops = diff.calculate()
        
        // Expected:
        // Remove L2 (index 1 in old)
        // Insert L0 at index 0
        // Insert L4 at index 3
        
        // Let's see what DiffUtil produces. dispatchUpdatesTo calls callbacks in a specific order
        // that allows applying them one by one.

        val current = applyDiffOp(old, ops, new)
        assertEquals(new, current)
    }

    private fun applyDiffOp(
        old: List<String>,
        ops: List<DiffOp>,
        new: List<String>
    ): MutableList<String> {
        val current = old.toMutableList()
        ops.forEach { op ->
            when (op) {
                is DiffOp.Insert -> {
                    repeat(op.count) { i ->
                        current.add(op.toOldIndex + i, new[op.fromNewIndex + i])
                    }
                }

                is DiffOp.Remove -> {
                    repeat(op.count) {
                        current.removeAt(op.index)
                    }
                }
            }
        }
        return current
    }

    @Test
    fun calculate_complex() {
        val old = listOf("L0", "L1", "L2", "L3", "L4")
        val new = listOf("L0", "L2", "L3-new", "L4")
        val diff = Diff(old, new)
        val ops = diff.calculate()

        val current = applyDiffOp(old, ops, new)
        assertEquals(new, current)
    }
}

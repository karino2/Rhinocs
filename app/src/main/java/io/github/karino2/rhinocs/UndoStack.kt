package io.github.karino2.rhinocs

enum class UndoType {
    DELETE, INSERT, COMPOSITE
}

sealed class UndoLineOp {
    abstract val index: Int
    abstract val text: String
    data class LineInsert(override val index: Int, override val text: String) : UndoLineOp()
    data class LineDelete(override val index: Int, override val text: String) : UndoLineOp()
}

data class UndoData(
    val utype: UndoType,
    val at: Point,
    val text: String,
    val oldText: String? = null,
    val children: List<UndoLineOp>? = null
)

class UndoStack {
    val undoStack = mutableListOf<UndoData>()
    val redoStack = mutableListOf<UndoData>()

    var currentRevision = 0

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        currentRevision = 0
    }

    fun push(data: UndoData) {
        undoStack.add(data)
        redoStack.clear()
        currentRevision++
    }

    fun pushInsert(at: Point, text: String) {
        push(UndoData(UndoType.INSERT, at, text))
    }

    fun pushDelete(at: Point, text: String) {
        push(UndoData(UndoType.DELETE, at, text))
    }

    fun popUndo(): UndoData? {
        if (undoStack.isEmpty()) return null
        currentRevision--
        return undoStack.removeAt(undoStack.lastIndex)
    }

    fun popRedo(): UndoData? {
        if (redoStack.isEmpty()) return null
        currentRevision++
        return redoStack.removeAt(redoStack.lastIndex)
    }

    fun pushRedo(data: UndoData) {
        redoStack.add(data)
    }

    fun pushUndo(data: UndoData) {
        undoStack.add(data)
    }
}

class CompositeUndoBuilder(val at: Point) {
    private val children = mutableListOf<UndoLineOp>()

    fun pushInsert(index: Int, text: String) {
        children.add(UndoLineOp.LineInsert(index, text))
    }

    fun pushDelete(index: Int, text: String) {
        children.add(UndoLineOp.LineDelete(index, text))
    }

    fun build(): UndoData? {
        if (children.isEmpty()) return null
        return UndoData(UndoType.COMPOSITE, at, "", children = children)
    }
}

package io.github.karino2.rhinocs

enum class UndoType {
    DELETE, INSERT
}

data class UndoData(val utype: UndoType, val at: Point, val text: String)

class UndoStack {
    val undoStack = mutableListOf<UndoData>()
    val redoStack = mutableListOf<UndoData>()

    var currentRevision = 0

    fun pushInsert(at: Point, text: String) {
        undoStack.add(UndoData(UndoType.INSERT, at, text))
        redoStack.clear()
        currentRevision++
    }

    fun pushDelete(at: Point, text: String) {
        undoStack.add(UndoData(UndoType.DELETE, at, text))
        redoStack.clear()
        currentRevision++
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

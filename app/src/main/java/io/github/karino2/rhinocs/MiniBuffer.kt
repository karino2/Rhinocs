package io.github.karino2.rhinocs


class MiniBuffer {
    var prompt = ""
    val buffer = Buffer().apply{
        name = "*Mini Buffer*"
        isMiniBuffer = true
    }
}

class MiniBufferWindow {
    val miniBuffer = MiniBuffer()
    val window = Window().apply {
        buffer = miniBuffer.buffer
        numRows = 1
    }
    var numCols: Int
        get() = window.numCols
        set(value) { window.numCols = value }
}


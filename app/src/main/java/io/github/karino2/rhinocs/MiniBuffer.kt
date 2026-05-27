package io.github.karino2.rhinocs


class MiniBuffer {
    var prompt = ""
    val buffer = Buffer("*Mini Buffer*").apply{
        isMiniBuffer = true
    }
}

class MiniBufferWindow {
    val miniBuffer = MiniBuffer()
    val window = Window(miniBuffer.buffer).apply {
        numRows = 1
    }
    var numCols: Int
        get() = window.numCols
        set(value) { window.numCols = value }
}


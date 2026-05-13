package io.github.karino2.rhinocs

class Buffer {
    val lines = ArrayList<StringBuilder>()
    val numRows: Int
        get() = lines.size
    fun load(text: String) {
        lines.clear()
        text.split("\n").forEach {line->
            StringBuilder().let {
                it.append(line)
                lines.add(it)
            }
        }
    }
}
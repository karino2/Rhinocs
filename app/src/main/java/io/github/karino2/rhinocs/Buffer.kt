package io.github.karino2.rhinocs

class Buffer {
    val lines = ArrayList<StringBuilder>()
    val numLines: Int
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

    fun getLine(linenum: Int) = lines[linenum].toString()

    fun insert(at: Point, content: String) : Point {
        lines[at.linenum].insert(at.offset, content)
        return forwardChar(at, content.length)
    }

    fun forwardChar(from: Point, delta: Int) : Point {
        var rest = delta
        var offset = from.offset
        var linenum = from.linenum
        while(true) {
            val line = getLine(linenum)
            if(offset+rest < line.length)
                return Point(linenum, offset+rest)

            // 最後の行でも終わりまで行けなかった。最後のPointを返す
            if(linenum == lines.size-1) {
                return Point(linenum, line.length)
            }

            rest -= line.length-offset
            offset = 0
            linenum += 1
        }
    }

    fun backwardChar(from: Point, delta: Int) : Point {
        // 現在の行で十分な場合は計算が単純なので特別扱い
        if (from.offset >= delta)
            return from.copy(offset = from.offset - delta)


        var rest = delta-from.offset
        var linenum = from.linenum-1
        while(linenum>=0) {
            val line = getLine(linenum)
            if (line.length >= rest)
                return Point(linenum, line.length-rest)

            rest -= line.length
            linenum -= 1
        }

        // 最初の行でも終わりまで行けなかった。最初のPointを返す
        return Point(0, 0)
    }

    companion object {
        fun fromText(text: String) = Buffer().apply { load(text) }
    }
}
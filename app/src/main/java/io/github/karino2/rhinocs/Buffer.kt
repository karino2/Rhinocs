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
        val point = from.point+delta
        var rest = delta
        var offset = from.offset
        var linenum = from.linenum

        // 文字数を合計していく。最後をセットしたい時だけ使う
        var total = 0L
        while(true) {
            val line = getLine(linenum)
            if(offset+rest < line.length)
                return Point(linenum, offset+rest, point)

            // 最後の行でも終わりまで行けなかった。最後のPointを返す
            if(linenum == lines.size-1) {
                return Point(linenum, line.length, total+line.length)
            }

            rest -= line.length-offset
            total += line.length
            offset = 0
            linenum += 1
        }
    }

    fun backwardChar(from: Point, delta: Int) : Point {
        // 正常ならいつもpointはこれ。
        val point = from.point - delta

        // 現在の行で十分な場合は計算が単純なので特別扱い
        if (from.offset >= delta)
            return from.copy(offset = from.offset - delta, point = from.point - delta)


        var rest = delta-from.offset
        var linenum = from.linenum-1
        while(linenum>=0) {
            val line = getLine(linenum)
            if (line.length >= rest)
                return Point(linenum, line.length-rest, point -rest)

            rest -= line.length
            linenum -= 1
        }

        // 最初の行でも終わりまで行けなかった。最初のPointを返す
        return Point(0, 0, 0)
    }

    // Bufferからpoint番目の文字の位置からPointを返す。
    // 範囲内ならpointの指す位置を、範囲外なら一番最後のPointを返す。
    fun toPoint(point: Long) : Point {
        var total = 0L

        lines.forEachIndexed { index, line ->
            if (point < total + line.length) {
                return Point(index, (point - total).toInt(), point)
            }
            total += line.length
        }
        val lastIdx = (lines.size - 1).coerceAtLeast(0)
        return Point(lastIdx, lines[lastIdx].length, total)
    }

    val lastPoint : Long
        get() = lines.sumOf { it.length.toLong() }

    fun findLine(pos: Long): Int {
        var total = 0L
        lines.forEachIndexed { index, line ->
            total += line.length
            if (pos < total) {
                return index
            }
        }
        return (lines.size - 1).coerceAtLeast(0)
    }

    // 実際に削除された文字数を返す
    fun deleteRegion(from: Long, to: Long) : Long{
        if (from >= to) return 0
        val pFrom = toPoint(from)
        val pTo = toPoint(to)

        if (pFrom.point == pTo.point)
            return 0

        if (pFrom.linenum == pTo.linenum) {
            lines[pFrom.linenum].delete(pFrom.offset, pTo.offset)
        } else {
            lines[pFrom.linenum].delete(pFrom.offset, lines[pFrom.linenum].length)
            lines[pTo.linenum].delete(0, pTo.offset)
            for (i in pTo.linenum - 1 downTo pFrom.linenum + 1) {
                lines.removeAt(i)
            }
        }
        return pTo.point - pFrom.point
    }

    companion object {
        fun fromText(text: String) = Buffer().apply { load(text) }
    }
}
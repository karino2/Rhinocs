package io.github.karino2.rhinocs

import android.net.Uri
import kotlin.math.max
import kotlin.math.min

class Buffer {

    // 対応するfileがあれば入る。なければnull
    var url: Uri? = null

    // 今の所使ってない。
    var name = ""

    val lines = ArrayList<StringBuilder>().apply { add(StringBuilder()) }
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

    fun toText() :String {
        return lines.joinToString("\n")
    }

    fun getLine(linenum: Int) = lines[linenum].toString()

    fun insert(at: Point, content: String) : Point {
        val clines = content.split('\n')

        lines[at.linenum].insert(at.offset, clines[0])
        val restLines = clines.drop(1)
        if (restLines.isNotEmpty()) {
            val firstLineEndPos = at.offset+clines[0].length
            val restOfFirstLine = lines[at.linenum].substring(firstLineEndPos)
            lines[at.linenum].delete(firstLineEndPos, lines[at.linenum].length)
            restLines.forEachIndexed { index, line ->
                lines.add(at.linenum+1+index, StringBuilder().apply{ append(line) } )
            }
            lines[at.linenum+restLines.size].append(restOfFirstLine)
        }
        return forwardChar(at, content.length)
    }

    fun forwardLine(from: Point, delta: Int) : Point {
        val destLine = min(lines.size-1, from.linenum+delta)
        return toPoint(destLine, 0)
    }

    fun backwardLine(from: Point, delta: Int) : Point {
        val destLine = max(0, from.linenum-delta)
        return toPoint(destLine, 0)
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
            if(offset+rest <= line.length)
                return Point(linenum, offset+rest, point)

            // 最後の行でも終わりまで行けなかった。最後のPointを返す
            if(linenum == lines.size-1) {
                return Point(linenum, line.length, pointMax)
            }

            rest -= line.length+1-offset
            total += line.length+1
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


        var rest = delta-from.offset-1
        var linenum = from.linenum-1
        while(linenum>=0) {
            val line = getLine(linenum)
            if (line.length >= rest)
                return Point(linenum, line.length-rest, point -rest)

            rest -= line.length +1
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
            if (point <= total + line.length) {
                return Point(index, (point - total).toInt(), point)
            }
            total += line.length+1
        }
        val lastIdx = (lines.size - 1).coerceAtLeast(0)
        return Point(lastIdx, lines[lastIdx].length, total)
    }

    // 何行目の何文字めか、からPointを作る。先頭から何文字めか数える必要があるのでBufferのメソッドで。
    fun toPoint(linenum: Int, offset: Int) : Point {
        assert(linenum < lines.size)

        if (linenum == 0)
            return Point(0, offset, offset.toLong())

        val prev = lines.take(linenum).sumOf { it.length.toLong()+1 }
        return Point(linenum, offset, prev+offset)
    }

    val pointMax : Long
        get() = lines.sumOf { it.length.toLong() }+(lines.size-1)

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
            val suffix = lines[pTo.linenum].substring(pTo.offset)
            lines[pFrom.linenum].delete(pFrom.offset, lines[pFrom.linenum].length)
            lines[pFrom.linenum].append(suffix)
            for (i in pTo.linenum downTo pFrom.linenum + 1) {
                lines.removeAt(i)
            }
        }
        return pTo.point - pFrom.point
    }

    fun gotoBol(point: Point) = toPoint(point.linenum, 0)

    fun gotoEol(point: Point) = toPoint(point.linenum, getLine(point.linenum).length)

    companion object {
        fun fromText(text: String) = Buffer().apply { load(text) }
    }
}
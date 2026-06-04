package io.github.karino2.rhinocs

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.Keep
import kotlin.math.max
import kotlin.math.min

class Marker(val buffer: Buffer, pos: Long) {
    var position: Long = pos
        set(newPos) {
            when {
                newPos > buffer.positionMax -> field = buffer.positionMax
                // 負の値も設定出来るようにしておく。-1で設定されてないとする。
                else -> field = newPos
            }
        }
}

class Buffer() {

    constructor(bname: String) : this() {
        this.name = bname
    }

    // 対応するfileがあれば入る。なければnull
    var url: Uri? = null

    var name = ""
    var isMiniBuffer = false

    var savedRevision = 0
    val isModified: Boolean
        get() = undoStack.currentRevision != savedRevision

    val lines = ArrayList<StringBuilder>().apply { add(StringBuilder()) }
    val numLines: Int
        get() = lines.size

    val mark = Marker(this, -1)

    val undoStack = UndoStack()

    fun load(text: String) {
        lines.clear()
        text.split("\n").forEach {line->
            StringBuilder().let {
                it.append(line)
                lines.add(it)
            }
        }
        savedRevision = undoStack.currentRevision
    }

    fun toText() :String {
        return lines.joinToString("\n")
    }

    fun getLine(linenum: Int) = lines[linenum].toString()

    fun adjustAfterInsert(at: Point, contentSize: Int) {
        if(mark.position > at.position) {
            mark.position += contentSize
        }
    }

    fun notifyModified() {
        onModified()
    }

    var onModified: () -> Unit = {}

    fun insert(at: Point, content: String, recordUndo: Boolean = true) : Point {
        if(content.isEmpty()) return at

        if (recordUndo) {
            undoStack.pushInsert(at, content)
        }

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
        adjustAfterInsert(at, content.length)
        notifyModified()
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
        val point = from.position+delta
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
                return Point(linenum, line.length, positionMax)
            }

            rest -= line.length+1-offset
            total += line.length+1
            offset = 0
            linenum += 1
        }
    }

    fun backwardChar(from: Point, delta: Int) : Point {
        // 正常ならいつもpointはこれ。
        val point = from.position - delta

        // 現在の行で十分な場合は計算が単純なので特別扱い
        if (from.offset >= delta)
            return from.copy(offset = from.offset - delta, position = from.position - delta)


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
    // 範囲内ならpointの指す位置を、範囲外なら負なら0を、大きすぎるなら一番最後のPointを返す。
    fun toPoint(point: Long) : Point {
        if (point < 0)
            return Point(0, 0, 0)
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

    val positionMax : Long
        get() = lines.sumOf { it.length.toLong() }+(lines.size-1)

    fun adjustAfterDelete(from: Long, delNum: Long) {
        if(mark.position > from) {
            mark.position = (mark.position-delNum).coerceAtLeast(from)
        }
    }

    // 実際に削除された文字数を返す
    fun deleteRegion(from: Long, to: Long, recordUndo: Boolean = true) : Long{
        if (from >= to) return 0
        val pFrom = toPoint(from)
        val pTo = toPoint(to)

        if (pFrom.position == pTo.position)
            return 0

        if (recordUndo) {
            val deletedText = substring(from, to)
            undoStack.pushDelete(pFrom, deletedText)
        }

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
        val delNum =  pTo.position - pFrom.position
        adjustAfterDelete(from, delNum)
        notifyModified()
        return delNum
    }

    fun gotoBol(point: Point) = toPoint(point.linenum, 0)

    fun gotoEol(point: Point) = toPoint(point.linenum, getLine(point.linenum).length)

    fun substring(beg: Long, end: Long) : String {
        val bp = toPoint(beg)
        val ep = toPoint(end)
        if (bp.linenum == ep.linenum) {
            return lines[bp.linenum].substring(bp.offset, ep.offset)
        }
        val ret = StringBuilder()
        ret.append(lines[bp.linenum].substring(bp.offset))
        for(i in (bp.linenum+1)..(ep.linenum-1)) {
            ret.append("\n")
            ret.append(lines[i])
        }
        ret.append("\n")
        ret.append(lines[ep.linenum].substring(0, ep.offset))
        return ret.toString()
    }

    fun isEol(point: Point) : Boolean {
        if (lines.isEmpty())
            return true
        return lines[point.linenum].length == point.offset
    }

    fun save(resolver: ContentResolver) : Boolean {
        return url?.let {
            FastFile.fromDocUri(resolver, it)?.let {ff->
                ff.writeText(toText())
                savedRevision = undoStack.currentRevision
                true
            }
        } ?: false
    }


    fun searchForward(from: Point, word: String, limit: Long? = null) : Point? {
        if (word.isEmpty()) return from
        val lp = limit?.let { toPoint(it) }

        for (i in from.linenum until lines.size) {
            lp?.let { if (i > it.linenum) return null }

            val startOffset = if (i == from.linenum) from.offset else 0
            val line = lines[i]
            val index = line.indexOf(word, startOffset)
            if (index != -1) {
                lp?.let { if (i == it.linenum && index > it.offset) return null }
                return toPoint(i, index)
            }
        }
        return null
    }

    fun searchBackward(from: Point, word: String, limit: Long? = null): Point? {
        if (word.isEmpty()) return from
        val lp = limit?.let { toPoint(it) }

        for (i in from.linenum downTo 0) {
            lp?.let { if (i < it.linenum) return null }

            val line = lines[i]
            val maxStartOffset = if (i == from.linenum) {
                (from.offset - word.length).coerceAtMost(line.length)
            } else {
                line.length
            }

            if (maxStartOffset < 0) continue

            val index = line.lastIndexOf(word, maxStartOffset)
            if (index != -1) {
                lp?.let { if (i == it.linenum && index < it.offset) return null }
                return toPoint(i, index)
            }
        }
        return null
    }

    @Keep
    @JvmOverloads
    fun searchForward(from: Long, word: String, limit: Long? = null) : Long? {
        return searchForward(toPoint(from), word, limit)?.position
    }

    @Keep
    @JvmOverloads
    fun searchBackward(from: Long, word: String, limit: Long? = null) : Long? {
        return searchBackward(toPoint(from), word, limit)?.position
    }

    fun undo() : Point? {
        val data = undoStack.popUndo() ?: return null
        return when(data.utype) {
            UndoType.INSERT -> {
                deleteRegion(data.at.position, data.at.position + data.text.length, recordUndo = false)
                undoStack.pushRedo(data)
                data.at
            }
            UndoType.DELETE -> {
                insert(data.at, data.text, recordUndo = false)
                undoStack.pushRedo(data)
                data.at
            }
        }
    }

    fun redo() : Point? {
        val data = undoStack.popRedo() ?: return null
        return when(data.utype) {
            UndoType.INSERT -> {
                insert(data.at, data.text, recordUndo = false)
                undoStack.pushUndo(data)
                data.at
            }
            UndoType.DELETE -> {
                deleteRegion(data.at.position, data.at.position + data.text.length, recordUndo = false)
                undoStack.pushUndo(data)
                data.at
            }
        }
    }
}

package io.github.karino2.rhinocs

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/*
  このファイルはjmuk版のdictionary_loader.jsのevalDataを元にgeminiに書き直してもらったものです。
 */

data class SkkEntry(val word: String, val annotation: String?) {
    fun toJSObject(ctx: Context, scope: Scriptable) : Scriptable{
        val result = ctx.newObject(scope)
        ScriptableObject.putProperty(result, "word", word)
        annotation?.let {
            ScriptableObject.putProperty(result, "annotation", it)
        }
        return result
    }
}

class SkkDictionary() : ScriptableObject() {
    override fun getClassName(): String {
        return "SkkNativeDictionary"
    }

    val entryDict = HashMap<String, Array<SkkEntry>>()

    override fun get(name: String, start: Scriptable): Any? {
        val entries = entryDict[name] ?: return super.get(name, start)

        // on demandにSkkEntryからJavaScriptのオブジェクトの配列に変換する
        val ctx = Context.getCurrentContext()
        val jsArray = ctx.newArray(parentScope, entries.size)
        entries.forEachIndexed { index, entry ->
            ScriptableObject.putProperty(jsArray, index, entry.toJSObject(ctx, parentScope))
        }
        return jsArray
    }

}


class SkkDictionaryLoader {
    fun parseData(ctx: Context, scope: Scriptable, content: String): SkkDictionary {
        val startTime = System.currentTimeMillis()
        val lines = content.split("\n")

        val result = SkkDictionary()
        result.parentScope = scope

        lines.forEachIndexed { i, line ->
            if (line.isEmpty() || line.startsWith(";")) {
                return@forEachIndexed
            }
            val spacePos = line.indexOf(' ')
            if (spacePos < 0) return@forEachIndexed

            val reading = line.substring(0, spacePos)
            val entriesPart = line.substring(spacePos + 1)
            val entriesStrings = entriesPart.split("/")

            val entries = entriesStrings.filter { it.isNotEmpty() }.map { entryStr ->
                parseEntry(entryStr)
            }
            result.entryDict[reading] = entries.toTypedArray()

            /*
            if (i % 1000 == 0) {
                println("parsing: progress=$i, total=${lines.size}")
            }
             */
        }
        val endTime = System.currentTimeMillis()
        println("parsing: finished in ${endTime - startTime} ms")
        return result
    }

    private fun evalSexp(word: String): String {
        if (!word.startsWith("(concat ") || !word.endsWith(")")) {
            return word
        }

        val result = StringBuilder()
        var inStr = false
        var i = 8
        while (i < word.length - 1) {
            val c = word[i]
            if (c == '"') {
                inStr = !inStr
                i++
                continue
            }
            if (!inStr) {
                i++
                continue
            }
            if (c == '\\') {
                if (i + 3 < word.length - 1) {
                    val octalStr = word.substring(i + 1, i + 4)
                    try {
                        val code = octalStr.toInt(8)
                        result.append(code.toChar())
                        i += 4
                    } catch (e: NumberFormatException) {
                        result.append(c)
                        i++
                    }
                } else {
                    result.append(c)
                    i++
                }
            } else {
                result.append(c)
                i++
            }
        }
        return result.toString()
    }

    private fun parseEntry(entry: String): SkkEntry {
        val semicolon = entry.indexOf(';')
        return if (semicolon < 0) {
            SkkEntry(evalSexp(entry), null)
        } else {
            SkkEntry(evalSexp(entry.substring(0, semicolon)), evalSexp(entry.substring(semicolon + 1)))
        }
    }
}

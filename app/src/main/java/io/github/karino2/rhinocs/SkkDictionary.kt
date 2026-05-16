package io.github.karino2.rhinocs

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/*
  このファイルはjmuk版のdictionary_loader.jsのevalDataを元にgeminiに書き直してもらったものです。
 */

class SkkDictionary {
    fun parseData(ctx: Context, scope: Scriptable, content: String): Scriptable {
        val startTime = System.currentTimeMillis()
        val lines = content.split("\n")
        val result = ctx.newObject(scope)

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
                parseEntry(ctx, scope, entryStr)
            }
            
            val entriesArray = ctx.newArray(scope, entries.size)
            entries.forEachIndexed { index, entry ->
                ScriptableObject.putProperty(entriesArray, index, entry)
            }
            ScriptableObject.putProperty(result, reading, entriesArray)

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

    private fun parseEntry(ctx: Context, scope: Scriptable, entry: String): Scriptable {
        val semicolon = entry.indexOf(';')
        val result = ctx.newObject(scope)
        if (semicolon < 0) {
            ScriptableObject.putProperty(result, "word", evalSexp(entry))
        } else {
            ScriptableObject.putProperty(result, "word", evalSexp(entry.substring(0, semicolon)))
            ScriptableObject.putProperty(result, "annotation", evalSexp(entry.substring(semicolon + 1)))
        }
        return result
    }
}

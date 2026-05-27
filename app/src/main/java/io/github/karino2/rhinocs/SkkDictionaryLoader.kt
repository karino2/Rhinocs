package io.github.karino2.rhinocs

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject


class SkkDictionary : ScriptableObject() {
    override fun getClassName(): String {
        return "SkkNativeDictionary"
    }

    // エントリをパース前の文字列 ("単語1/単語2;注釈/") のまま保持してメモリを節約する
    val entryDict = HashMap<String, String>()

    override fun get(name: String, start: Scriptable): Any? {
        val entriesPart = entryDict[name] ?: return super.get(name, start)

        // オンデマンドに行の後半部分をパースして JS オブジェクトの配列に変換する
        val ctx = Context.getCurrentContext()
        val entriesStrings = entriesPart.split("/")
        val filtered = entriesStrings.filter { it.isNotEmpty() }
        
        val jsArray = ctx.newArray(parentScope, filtered.size)
        filtered.forEachIndexed { index, entryStr ->
            val entryJS = parseEntry(ctx, parentScope, entryStr)
            putProperty(jsArray, index, entryJS)
        }
        return jsArray
    }

    private fun parseEntry(ctx: Context, scope: Scriptable, entry: String): Scriptable {
        val semicolon = entry.indexOf(';')
        val result = ctx.newObject(scope)

        if (semicolon < 0) {
            putProperty(result, "word", evalSexp(entry))
        } else {
            val word = evalSexp(entry.substring(0, semicolon))
            val annotation = evalSexp(entry.substring(semicolon + 1))
            putProperty(result, "word", word)
            putProperty(result, "annotation", annotation)
        }
        return result
    }

    /*
      このメソッドはjmuk版のdictionary_loader.jsのevalSexpを元にgeminiに書き直してもらったものです。
     */
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
}


class SkkDictionaryLoader {
    fun parseData(ctx: Context, scope: Scriptable, content: String): SkkDictionary {
        val startTime = System.currentTimeMillis()

        val result = SkkDictionary()
        result.parentScope = scope

        // lineSequence() を使って一行ずつ lazy に処理する
        content.lineSequence().forEach { line ->
            if (line.isEmpty() || line.startsWith(";")) {
                return@forEach
            }
            val spacePos = line.indexOf(' ')
            if (spacePos < 0) return@forEach

            val reading = line.substring(0, spacePos)
            val entriesPart = line.substring(spacePos + 1)
            
            // オブジェクトを作らず文字列のまま格納
            result.entryDict[reading] = entriesPart
        }
        val endTime = System.currentTimeMillis()
        println("parsing: finished in ${endTime - startTime} ms")
        return result
    }
}

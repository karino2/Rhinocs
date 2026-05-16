package io.github.karino2.rhinocs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.mozilla.javascript.ContinuationPending
import androidx.core.content.edit
import androidx.core.net.toUri
import org.mozilla.javascript.EcmaError

data class RequestArg(val requestId: Int, val arg: Any)

class MainActivity : AppCompatActivity() {
    companion object {
        const val  PACKAGE_DIR_URI_KEY = "last_uri_path"
        fun packageDirUriStr(ctx: Context) = sharedPreferences(ctx).getString(PACKAGE_DIR_URI_KEY, null)
        fun writePackageDirUriStr(ctx: Context, path : String) = sharedPreferences(ctx).edit(commit = true) {
            putString(PACKAGE_DIR_URI_KEY, path)
        }

        fun resetPackageDirUriStr(ctx: Context) = sharedPreferences(ctx).edit(commit = true) {
            putString(PACKAGE_DIR_URI_KEY, null)
        }

        private fun sharedPreferences(ctx: Context) = ctx.getSharedPreferences("Rhinocs", Context.MODE_PRIVATE)

        fun showMessage(ctx: Context, msg : String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    fun showMessage(msg : String) = showMessage(this, msg)

    private val getFileUriFromScript = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            pendingCC?.let { pcc->
                interpreter.resume(pcc, it)
            }
        }
    }

    private val getPackageDirUri = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            writePackageDirUriStr(this, it.toString())
            loadInitScript()
        }
    }

    private val interpreter by lazy {
        Interpreter().apply{
            global.activity = this@MainActivity
            global.rview = rview
            loadBuiltin()
        }
    }

    private fun Interpreter.loadBuiltin() {
        // buildins_override.jsがあればそちらを優先
        val overwrite = packageDirUri?.let { FastFile.fromTreeUri(this@MainActivity, it).findFile("builtins_override.js")?.readText() }
        overwrite?.let {
            run(it, "builtins_override.js")
            return
        }
        run(readAsset("builtins.js"), "builtins.js")
    }


    var pendingCC: ContinuationPending? = null

    private val rview: RView
        get() = findViewById<RView>(R.id.rView)!!

    private val window: Window
        get() = rview.window

    val packageDirUri: Uri?
        get() = packageDirUriStr(this)?.toUri()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        rview.keyDownHandler = {keyStr->
            interpreter.setGlobalKey(keyStr)
            runScript($$"onKeyDown($key);")
        }
        findViewById<Button>(R.id.buttonDeb1).setOnClickListener {
            runScript("""find_file();""")
        }
        findViewById<Button>(R.id.buttonDeb2).setOnClickListener {
            getPackageDirUri.launch(null)
        }
        findViewById<Button>(R.id.buttonDeb3).setOnClickListener {
            loadInitScript()
        }
        findViewById<Button>(R.id.buttonDeb4).setOnClickListener {
        }

        // loadInitScript()
    }

    private fun loadInitScript() {
        loadPackageJS("skk/skk_all.js", "*script*", "Init js load fail: ")
    }

    fun sourceDirPath(sourceName: String) : List<String> {
        if (sourceName.startsWith("*"))
            return emptyList()
        return sourceName.split("/").dropLast(1)
    }

    private fun mergePath(relativePath: String, fromSource: String) : String {
        val sourceDirs = sourceDirPath(fromSource)
        if(sourceDirs.isEmpty())
            return relativePath
        if (relativePath.startsWith("*"))
            return relativePath
        return listOf(sourceDirs.joinToString("/"), relativePath).joinToString("/")
    }

    fun loadPackageJS(relativePath: String, fromSource: String, errorLabel: String) {
        findSourceFile(fromSource, relativePath)
        val content = readFileContent(relativePath, fromSource)
        if(content.isEmpty()) return

        val path = mergePath(relativePath, fromSource)
        runScript(content, path, errorLabel)
    }

    fun sourceDir(sourceName: String) : FastFile? {
        packageDirUri?.let { ini ->
            val root = FastFile.fromTreeUri(this, ini)
            return sourceDirPath(sourceName).fold(root as FastFile?) { acc, dirName ->
                    acc?.findFile(dirName)
                }
        }
        return null
    }

    private fun findSourceFile(
        fromSource: String,
        fileName: String
    ): FastFile? = sourceDir(fromSource)?.findFileRec(fileName)

    fun readFileContent(fileName: String, fromSource: String) : String {
        return findSourceFile(fromSource, fileName)?.readText() ?: ""
    }

    fun readGZIPFileContent(fileName: String, fromSource: String) : String {
        val startTime = System.currentTimeMillis()
        val content = findSourceFile(fromSource, fileName)?.readGZIPText() ?: return ""
        val endTime = System.currentTimeMillis()
        println("readGZIPFileContent: finished in ${endTime - startTime} ms")
        return content
    }

    private fun readAsset(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun runScript(script: String, fileName: String="*script*", errorLabel: String = "") {
        try {
            interpreter.run(script, fileName)
        } catch (e: ContinuationPending) {
            pendingCC = e
            val rarg = e.applicationState as RequestArg
            when (rarg.requestId) {
                GlobalObject.REQUEST_SELECT_FILE -> getFileUriFromScript.launch(rarg.arg as Array<String>)
            }
        } catch(e : EcmaError) {
            val msg = "$errorLabel: $e"
            showMessage(this, msg)
            println(msg)
        }
    }
}


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
                val dispName = FastFile.fromDocUri(contentResolver, it)?.name ?: ""
                withContinuationHandling {
                    interpreter.resume(pcc, interpreter.newJSArray(arrayOf(it.toString(), dispName)))
                }
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

    // reset可能なlazy getterみたいな事をしたい。
    private var _interpreter: Interpreter? = null
    private val interpreter : Interpreter
        get() {
            if (_interpreter == null) {
                _interpreter = Interpreter().apply {
                    global.activity = this@MainActivity
                    global.rview = rview
                    loadBuiltin()
                }
            }
            return _interpreter!!
        }

    private fun Interpreter.loadBuiltin() {
        // buildins_override.jsがあればそちらを優先
        val overwrite = packageDirUri?.let { FastFile.fromTreeUri(this@MainActivity, it).findFile("builtins_override.js")?.readText() }
        overwrite?.let {
            run(it, "/builtins_override.js")
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
            if (pendingReadKey)
            {
                pendingReadKey = false
                pendingCC?.let { pcc->
                    withContinuationHandling {
                        interpreter.resume(pcc, keyStr)
                    }
                }
            } else {
                interpreter.setGlobalKey(keyStr)
                runScript($$"onKeyDown($key);")
            }
        }
        findViewById<Button>(R.id.buttonDeb1).setOnClickListener {
            getPackageDirUri.launch(null)
        }
        findViewById<Button>(R.id.buttonDeb2).setOnClickListener {
            loadInitScript()
        }
        findViewById<Button>(R.id.buttonDeb3).setOnClickListener {
            _interpreter = null
            interpreter
            showMessage("Interpreter reset.")
        }

        // loadBuiltinしておく
        interpreter
    }

    private fun loadInitScript() {
        loadPackageJS("/init.js")
        showMessage("init.js loaded.")
    }

    fun loadPackageJS(absPath: String) {
        val content = readFileContent(absPath)
        if(content.isEmpty()) {
            showMessage("Fail to load: $absPath")
            return
        }

        runScript(content, absPath)
    }

    val packageRootDir: FastFile?
        get() {
            return packageDirUri?.let { ini ->
                FastFile.fromTreeUri(this, ini)
            }
        }

    fun trimHeadSlash(absPath: String) : String {
        if(!absPath.startsWith("/"))
            throw IllegalArgumentException("Path not start with /.")
        return absPath.drop(1)
    }

    private fun findSourceFile(
        absPath: String
    ): FastFile? = packageRootDir?.findFileRec(trimHeadSlash(absPath))

    fun readFileContent(fileName: String) : String {
        return findSourceFile(fileName)?.readText() ?: ""
    }

    private fun findSourceFileOrCreate(
        relativePath: String
    ): FastFile? {
        packageRootDir?.let { dir->
            dir.findFileRec(relativePath)?.let { return it }
            return dir.createFileRec(relativePath)
        }
        return null
    }



    fun writeFileContent(absPath: String, content: String) : Boolean  {
        val fileName = trimHeadSlash(absPath)
        findSourceFileOrCreate(fileName)?.let {
            it.writeText(content)
            return true
        }
        return false
    }

    fun readGZIPFileContent(fileName: String) : String {
        val startTime = System.currentTimeMillis()
        val content = findSourceFile(fileName)?.readGZIPText() ?: return ""
        val endTime = System.currentTimeMillis()
        println("readGZIPFileContent: finished in ${endTime - startTime} ms")
        return content
    }

    private fun readAsset(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun runScript(script: String, fileName: String="*script*") {
        withContinuationHandling { interpreter.run(script, fileName) }
    }

    private var pendingReadKey = false

    private fun withContinuationHandling(run: () -> Unit) {
        try {
            run()
        } catch (e: ContinuationPending) {
            pendingCC = e
            rview.invalidate()
            val rarg = e.applicationState as RequestArg
            when (rarg.requestId) {
                GlobalObject.REQUEST_SELECT_FILE -> getFileUriFromScript.launch(rarg.arg as Array<String>)
                GlobalObject.REQUEST_READ_KEY-> {
                    pendingReadKey = true
                    showMessage(rarg.arg as String)
                }
            }
        } catch (e: EcmaError) {
            val msg = "$e"
            showMessage(this, "$e")
            println(msg)
        }
    }
}


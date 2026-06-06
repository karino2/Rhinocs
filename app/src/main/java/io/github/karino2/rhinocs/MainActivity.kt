package io.github.karino2.rhinocs

import android.app.Dialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import org.mozilla.javascript.EcmaError
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.WrappedException
import java.io.FileOutputStream

interface JSActivity {
    fun getContentResolver() : ContentResolver
    fun readFileContent(fileName: String) : String
    fun writeFileContent(absPath: String, content: String) : Boolean
    fun readGZIPFileContent(fileName: String) : String
    fun putPrefString(key: String, value: String)
    fun getPrefString(key: String, defaultValue: String) : String
}

class JSActivityWrapper(b: MainActivity) : JSActivity by b

class MainActivity : JSActivity, AppCompatActivity() {
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

        const val DIALOG_ID_TEXT = 1
    }

    fun showMessage(msg : String) = showMessage(this, msg)

    val rhinocs: Rhinocs
        get() = rview.rhinocs

    @Keep
    override fun putPrefString(key: String, value: String) = sharedPreferences(this).edit(commit = true) {
        putString(key, value)
    }

    @Keep
    override fun getPrefString(key: String, defaultValue: String) : String = sharedPreferences(this).getString(key, defaultValue) ?: defaultValue

    val getOpenFileUriFromScript = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri->
        callbackArg?.let {ca->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val dispName = FastFile.fromDocUri(contentResolver, it)?.name ?: ""
                interpreter.callSuccess(ca, it.toString(), dispName)
                rview.invalidate()
                true
            } ?: callCancelCallback()
        }
    }

    val getNewFileUriFromScript = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri->
        callbackArg?.let {ca->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val dispName = FastFile.fromDocUri(contentResolver, it)?.name ?: ""
                interpreter.callSuccess(ca, it.toString(), dispName)
                rview.invalidate()
                true
            } ?: callCancelCallback()
        }
    }

    val getOpenDirUriFromScript = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri->
        callbackArg?.let {ca->
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val dir = FastFile.fromTreeUri(this, it)
                interpreter.callSuccess(ca, dir)
                rview.invalidate()
                true
            } ?: callCancelCallback()
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
                    global.setup(this@MainActivity, rview)
                    loadBuiltin()
                }
            }
            return _interpreter!!
        }

    private fun Interpreter.loadBuiltin() {
        // buildins_override.jsがあればそちらを優先
        val overwrite = packageDirUri?.let { FastFile.fromTreeUri(this@MainActivity, it).findFile("builtins_override.js")?.readText() }
        withExceptionHandling {
            overwrite?.let {
                run(it, "/builtins_override.js")
                true
            } ?: run(readAsset("builtins.js"), "builtins.js")
        }

    }


    var callbackArg: DelayedRequest.Arg? = null

    private val rview: RView
        get() = findViewById<RView>(R.id.rView)!!

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
                callbackArg?.let { ca->
                    interpreter.callSuccess(ca, keyStr)
                }
                rview.invalidate()
            } else {
                interpreter.setGlobalKey(keyStr)
                runScript($$"onKeyDown($key);")
            }
        }
        findViewById<Button>(R.id.buttonDeb1).setOnClickListener {
            getPackageDirUri.launch(null)
        }
        findViewById<Button>(R.id.buttonDeb2).setOnClickListener {
            _interpreter = null
            initInterpreter()
            rhinocs.resetModeLineFormat()
            showMessage("Interpreter reset.")
        }
        rview.requestFocus()

        // loadBuiltinしておく
        initInterpreter()
    }

    private fun initInterpreter() {
        interpreter
        loadInitScript()
    }

    private fun loadInitScript() {
        loadPackageJS("/init.js", mayNotExist = true)
    }

    fun loadPackageJS(absPath: String, mayNotExist: Boolean = false) : Boolean {
        val content = readFileContent(absPath)
        if(content.isEmpty()) {
            if (!mayNotExist)
                showMessage("Fail to load: $absPath")
            return false
        }

        runScript(content, absPath)
        return true
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

    override fun readFileContent(fileName: String) : String {
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

    @Keep
    override fun writeFileContent(absPath: String, content: String) : Boolean  {
        val fileName = trimHeadSlash(absPath)
        findSourceFileOrCreate(fileName)?.let {
            it.writeText(content)
            return true
        }
        return false
    }

    override fun readGZIPFileContent(fileName: String) : String {
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
        withExceptionHandling { interpreter.run(script, fileName) }
    }

    fun queryTextDialog(label: String, callback: DelayedRequest.Arg) {
        callbackArg = callback
        val bundle = Bundle().apply { putString("DIALOG_TEXT_LABEL", label) }
        showDialog(DIALOG_ID_TEXT, bundle)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateDialog(id: Int, args: Bundle?): Dialog? {
        when(id) {
            DIALOG_ID_TEXT->return createQueryTextDialog(args!!.getString("DIALOG_TEXT_LABEL")!!)
        }
        return super.onCreateDialog(id, args)
    }

    @Deprecated("Deprecated in Java")
    override fun onPrepareDialog(id: Int, dialog: Dialog?, args: Bundle?) {
        when(id) {
            DIALOG_ID_TEXT->{
                dialog!!.setTitle(args!!.getString("DIALOG_TEXT_LABEL")!!)
                dialog.findViewById<EditText>(R.id.text_edit_id)?.setText("")
            }
        }
        super.onPrepareDialog(id, dialog, args)
    }

    fun callCancelCallback() {
        callbackArg?.let { ca ->
            interpreter.callFail(ca)
            rview.invalidate()
        }
    }

    private fun createQueryTextDialog(label: String) : Dialog {
        val editText = EditText(this).apply { id = R.id.text_edit_id }
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        val margin = (20 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, 0, margin, 0)
        layout.addView(editText, params)

        fun callSuccessCallback() {
            callbackArg?.let { ca ->
                interpreter.callSuccess(ca, editText.text.toString())
                rview.invalidate()
            }
        }


        val dialog = AlertDialog.Builder(this)
            .setTitle(label)
            .setTitle(label)
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                callSuccessCallback()
            }
            .setNegativeButton("Cancel") { _, _ ->
                callCancelCallback()
            }
            .setOnCancelListener {
                callCancelCallback()
            }
            .create()

        editText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                if(keyCode == KeyEvent.KEYCODE_G && event.isCtrlPressed) {
                    dialog.cancel()
                    true
                }
                else if(keyCode == KeyEvent.KEYCODE_ENTER) {
                    callSuccessCallback()
                    dialog.dismiss()
                    true
                }
                else {
                    false
                }
            } else {
                false
            }
        }
        return dialog
    }

    private var pendingReadKey = false
    fun waitReadKey(label: String, callback: DelayedRequest.Arg) {
        pendingReadKey = true
        callbackArg = callback
        showMessage(label)
    }

    private fun withExceptionHandling(run: () -> Unit) {
        try {
            run()
        } catch (e: EcmaError) {
            showError("$e")
        } catch (e: WrappedException) {
            showError("$e")
        } catch (e: EvaluatorException) {
            showError("$e")
        }
    }

    private fun showError(msg: String) {
        showMessage(this, msg)
        println(msg)
    }
}

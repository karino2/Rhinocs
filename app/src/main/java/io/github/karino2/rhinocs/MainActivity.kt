package io.github.karino2.rhinocs

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.mozilla.javascript.ContinuationPending
import kotlin.math.max

data class RequestArg(val requestId: Int, val arg: Any)

class MainActivity : AppCompatActivity() {

    private val getFileUri = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri->
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

    private val interpreter by lazy {
        Interpreter().apply{
            global.activity = this@MainActivity
            global.rview = rview
            run("""
                function onKeyDown(str) {
                    print("deb:", str);
                }
            """.trimIndent())
        }
    }


    var pendingCC: ContinuationPending? = null

    private val rview: RView
        get() = findViewById<RView>(R.id.rView)!!

    private val window: Window
        get() = rview.window

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
            runScript("""let uri = select_file("text/*"); print(uri); open_uri(uri);""")
        }
        findViewById<Button>(R.id.buttonDeb2).setOnClickListener {
            window.moveCharDelta(-1)
            rview.invalidate()
        }
        findViewById<Button>(R.id.buttonDeb3).setOnClickListener {
            window.moveCharDelta(1)
            rview.invalidate()
        }
        findViewById<Button>(R.id.buttonDeb4).setOnClickListener {
        }
    }

    private fun runScript(script: String) {
        try {
            interpreter.run(script)
        } catch (e: ContinuationPending) {
            pendingCC = e
            val rarg = e.applicationState as RequestArg
            when (rarg.requestId) {
                GlobalObject.REQUEST_SELECT_FILE -> getFileUri.launch(rarg.arg as Array<String>)
            }
        }
    }
}

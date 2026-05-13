package io.github.karino2.rhinocs

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private val getFileUri = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            rview.loadFile(contentResolver, it)
        }
    }

    private val rview: RView
        get() = findViewById<RView>(R.id.rView)!!

    private val grid: Grid
        get() = rview.grid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<Button>(R.id.buttonDeb1).setOnClickListener {
            getFileUri.launch(arrayOf("text/*" /* "text/plain" */))
        }
        findViewById<Button>(R.id.buttonDeb2).setOnClickListener {
            grid.setOffset(grid.offsetRow, max(0, grid.offsetCol-1))
            rview.invalidate()
        }
        findViewById<Button>(R.id.buttonDeb3).setOnClickListener {
            grid.setOffset(grid.offsetRow, grid.offsetCol+1)
            rview.invalidate()
        }
    }
}
package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import com.example.lumikids.R
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class GameTypeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_type)
        enableEdgeToEdge()
        setupImmersiveMode()


        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val theme = intent.getStringExtra("THEME") ?: "furniure"

        val cardObject = findViewById<ImageView>(R.id.cardObjeto)
        val cardMemory = findViewById<ImageView>(R.id.cardMemorama)

        cardObject.setOnClickListener {
            openGame(ObjectLevelPickerActivity::class.java, theme)
        }
        cardMemory.setOnClickListener {
            openGame(MemoryLevelActivity::class.java, theme)
        }
    }

    private fun setupImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun openGame(
        activity: Class<*>,
        theme: String
    ) {
        val intent = Intent(this, activity)
        intent.putExtra("THEME", theme)
        startActivity(intent)
    }
}
package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lumikids.R

class MemoryLevelActivity : BaseActivity() {

    private lateinit var theme: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_level)
        enableEdgeToEdge()
        setupImmersiveMode()


        theme = intent.getStringExtra("THEME") ?: "emotions"

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        setupCard(R.id.card6, 6)
        setupCard(R.id.card8, 8)
        setupCard(R.id.card10, 10)
    }

    private fun setupImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }


    private fun setupCard(viewId: Int, numCards: Int) {
        findViewById<ImageView>(viewId).setOnClickListener {
            startMemoryGame(numCards)
        }
    }

    private fun startMemoryGame(totalCards: Int) {
        val intent = Intent(this, MemoryGame::class.java).apply {
            putExtra("THEME", theme)
            putExtra("NUM_CARDS", totalCards)
        }
        startActivity(intent)
    }
}
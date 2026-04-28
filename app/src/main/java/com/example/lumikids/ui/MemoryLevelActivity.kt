package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import com.example.lumikids.R

class MemoryLevelActivity : BaseActivity() {

    private lateinit var theme: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_level)


        theme = intent.getStringExtra("THEME") ?: "furniure"

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        setupCard(R.id.card6, 6)
        setupCard(R.id.card8, 8)
        setupCard(R.id.card10, 10)
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
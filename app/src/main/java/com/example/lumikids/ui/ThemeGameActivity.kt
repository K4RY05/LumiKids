package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lumikids.R

class ThemeGameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme)

        setupImmersiveMode()
        initViews()
    }

    private fun setupImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
    }

    // ✨ OPTIMIZACIÓN: Agrupamos todos los botones y sus clics en un solo lugar
    private fun initViews() {
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val furniure = findViewById<ImageView>(R.id.cardFur)
        val emotions = findViewById<ImageView>(R.id.cardEmociones)
        val clothes = findViewById<ImageView>(R.id.cardClot)

        btnBack.setOnClickListener { finish() }

        furniure.setOnClickListener { openGame("furniure") }
        emotions.setOnClickListener { openGame("emotions") }
        clothes.setOnClickListener { openGame("clothing") }
    }

    private fun openGame(theme: String) {
        val intent = Intent(this, GameTypeActivity::class.java)
        intent.putExtra("THEME", theme)
        startActivity(intent)
    }
}
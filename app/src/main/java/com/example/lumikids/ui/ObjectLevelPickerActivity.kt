package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lumikids.R

class ObjectLevelPickerActivity : BaseActivity() {

    // Estado
    private var selectedRounds: Int = 3
    private val minRounds = 1
    private val maxRounds = 12

    private lateinit var tvRoundCount: TextView

    private lateinit var theme: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_level)
        enableEdgeToEdge()
        setupImmersiveMode()


        theme = intent.getStringExtra("THEME") ?: "emotions"

        tvRoundCount = findViewById(R.id.tvRoundCount)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnDecrease = findViewById<ImageView>(R.id.btnDecrease)
        val btnIncrease = findViewById<ImageView>(R.id.btnIncrease)
        val btnStart = findViewById<TextView>(R.id.btnStart)

        updateUi()

        btnDecrease.setOnClickListener {
            if (selectedRounds > minRounds) {
                selectedRounds--
                updateUi()
            }
        }

        btnIncrease.setOnClickListener {
            if (selectedRounds < maxRounds) {
                selectedRounds++
                updateUi()
            }
        }

        btnBack.setOnClickListener { finish() }

        btnStart.setOnClickListener {
            startGame()
        }
    }
    private fun setupImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun updateUi() {
        tvRoundCount.text = selectedRounds.toString()
    }

    private fun startGame() {
        val intent = Intent(this, ObjectRecognition::class.java).apply {
            // Pasamos el String del tema directamente al juego
            putExtra("THEME", theme)
            putExtra("NUM_ROUNDS", selectedRounds)
        }
        startActivity(intent)
    }
}
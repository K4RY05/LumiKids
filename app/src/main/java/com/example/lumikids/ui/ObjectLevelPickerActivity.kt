package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R

class ObjectLevelPickerActivity : AppCompatActivity() {

    // Estado
    private var selectedRounds: Int = 3
    private val minRounds = 1
    private val maxRounds = 10

    private lateinit var tvRoundCount: TextView

    private lateinit var theme: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_level)

        theme = intent.getStringExtra("THEME") ?: "furniure"

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
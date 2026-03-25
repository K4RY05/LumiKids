package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.model.GameTheme
import com.example.lumikids.minigame.objectrecognition.ui.GameActivityN1

class ObjectLevelPickerActivity : AppCompatActivity() {

    // Estado
    private var selectedRounds: Int = 3
    private val minRounds = 1
    private val maxRounds = 10

    // UI
    private lateinit var tvRoundCount: TextView

    // 🔥 Tema tipado (NO string)
    private lateinit var theme: GameTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_level)

        theme = intent.getStringExtra("THEME")?.let {
            GameTheme.valueOf(it)
        } ?: GameTheme.FURNITURE


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
        val intent = Intent(this, GameActivityN1::class.java).apply {
            putExtra("THEME", theme.name)
            putExtra("NUM_ROUNDS", selectedRounds)
        }
        startActivity(intent)
    }
}
package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.minigame.memorygame.MemoryGame

class MemoryLevelActivity : AppCompatActivity() {

    // Ahora usamos String para el tema
    private lateinit var theme: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_level)

        // Recibir el String directamente.
        // Si es nulo, usamos "emotions" como valor por defecto.
        theme = intent.getStringExtra("THEME") ?: "furniure"

        // Botón regresar
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Eventos de dificultad
        setupCard(R.id.card6, 6)
        setupCard(R.id.card8, 8)
        setupCard(R.id.card10, 10)
    }

    private fun setupCard(viewId: Int, numCards: Int) {
        findViewById<ImageView>(viewId).setOnClickListener {
            iniciarMemorama(numCards)
        }
    }

    private fun iniciarMemorama(numeroDeCartas: Int) {
        val intent = Intent(this, MemoryGame::class.java).apply {
            // Pasamos el String tal cual lo recibimos
            putExtra("THEME", theme)
            putExtra("NUM_CARDS", numeroDeCartas)
        }
        startActivity(intent)
    }
}
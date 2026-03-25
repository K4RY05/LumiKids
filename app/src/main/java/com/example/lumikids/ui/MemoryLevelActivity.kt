package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.model.GameTheme
import com.example.lumikids.minigame.memorygame.ui.GameActivityN2

class MemoryLevelActivity : AppCompatActivity() {

    private lateinit var theme: GameTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_level)

        // 🔥 Recibir tema correctamente
        theme = intent.getStringExtra("THEME")?.let {
            GameTheme.valueOf(it)
        } ?: GameTheme.EMOTIONS

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
        val intent = Intent(this, GameActivityN2::class.java).apply {
            putExtra("THEME", theme.name)
            putExtra("NUM_CARDS", numeroDeCartas)
        }
        startActivity(intent)
    }
}
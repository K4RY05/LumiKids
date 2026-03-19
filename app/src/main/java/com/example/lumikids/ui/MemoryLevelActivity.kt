package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R

import com.example.lumikids.minigame.memorygame.ui.GameActivityN2

class MemoryLevelActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Asegúrate de que el nombre del layout coincida con el tuyo
        setContentView(R.layout.activity_memory_level)

        // Recibimos el tema que eligió el usuario
        val theme = intent.getStringExtra("THEME") ?: "EMOTIONS"

        // 2. Configuramos el botón de regresar
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // 3. Configuramos los clics de las tarjetas de dificultad
        findViewById<ImageView>(R.id.card6).setOnClickListener {
            iniciarMemorama(theme, 6)
        }
        findViewById<ImageView>(R.id.card8).setOnClickListener {
            iniciarMemorama(theme, 8)
        }
        findViewById<ImageView>(R.id.card10).setOnClickListener {
            iniciarMemorama(theme, 10)
        }
    }

    private fun iniciarMemorama(theme: String, numeroDeCartas: Int) {
        val intent = Intent(this, GameActivityN2::class.java).apply {
            putExtra("THEME", theme)
            putExtra("NUM_CARDS", numeroDeCartas) // Enviamos el número de cartas
        }
        startActivity(intent)
    }
}
package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R

class GameTypeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_type)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Recibimos el String directamente.
        // Si no llega nada, usamos "furniure" como respaldo para evitar que la app falle.
        val theme = intent.getStringExtra("THEME") ?: "furniure"

        // Tarjetas
        val cardObject = findViewById<ImageView>(R.id.cardObjeto)
        val cardMemory = findViewById<ImageView>(R.id.cardMemorama)

        // Eventos
        cardObject.setOnClickListener {
            openGame(ObjectLevelPickerActivity::class.java, theme)
        }

        cardMemory.setOnClickListener {
            openGame(MemoryLevelActivity::class.java, theme)
        }
    }

    // La función ahora recibe directamente el String
    private fun openGame(
        activity: Class<*>,
        theme: String
    ) {
        val intent = Intent(this, activity)
        intent.putExtra("THEME", theme) // Pasamos el String intacto a la siguiente actividad
        startActivity(intent)
    }
}
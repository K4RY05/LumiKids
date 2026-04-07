package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R

class ThemeGameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme)

        // Flecha regresar
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Tarjetas (Temáticas)
        val furniture = findViewById<ImageView>(R.id.cardFur)
        val emotions = findViewById<ImageView>(R.id.cardEmociones)
        val clothes = findViewById<ImageView>(R.id.cardClot)

        // Eventos: Pasamos directamente los Strings
        // Nota: Mantenemos "furniure" sin la 't' para que coincida con tu servidor
        furniture.setOnClickListener {
            openGame("furniure")
        }

        emotions.setOnClickListener {
            openGame("emotions")
        }

        clothes.setOnClickListener {
            openGame("clothes")
        }
    }

    // La función ahora recibe un String en lugar del enum GameTheme
    private fun openGame(theme: String) {
        val intent = Intent(this, GameTypeActivity::class.java)
        intent.putExtra("THEME", theme)
        startActivity(intent)
    }
}
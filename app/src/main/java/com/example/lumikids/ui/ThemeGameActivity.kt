package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.model.GameTheme
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

        // Eventos
        furniture.setOnClickListener {
            openGame(GameTheme.FURNITURE)
        }

        emotions.setOnClickListener {
            openGame(GameTheme.EMOTIONS)
        }

        clothes.setOnClickListener {
            openGame(GameTheme.CLOTHES)
        }
    }

    private fun openGame(theme: GameTheme) {
        val intent = Intent(this, GameTypeActivity::class.java)
        intent.putExtra("THEME", theme.name)
        startActivity(intent)
    }
}
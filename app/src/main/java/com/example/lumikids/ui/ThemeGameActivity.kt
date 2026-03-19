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
        val food = findViewById<ImageView>(R.id.cardFur)
        val emotions = findViewById<ImageView>(R.id.cardEmociones)
        val clothes = findViewById<ImageView>(R.id.cardRopa)

        //  FURNITURE
        food.setOnClickListener {

            val intent = Intent(this, GameTypeActivity::class.java)
            intent.putExtra("THEME", "FURNITURE")
            startActivity(intent)
        }

        //  EMOTIONS
        emotions.setOnClickListener {

            val intent = Intent(this, GameTypeActivity::class.java)
            intent.putExtra("THEME", "EMOTIONS")
            startActivity(intent)
        }

        //  CLOTHES
        clothes.setOnClickListener {
            val intent = Intent(this, GameTypeActivity::class.java)
            intent.putExtra("THEME", "CLOTHES")
            startActivity(intent)
        }
    }
}
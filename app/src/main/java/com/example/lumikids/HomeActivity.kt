package com.example.lumikids

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnTablero = findViewById<ImageView>(R.id.btnTablero)
        val btnMinijuegos = findViewById<ImageView>(R.id.btnMinijuegos)

        btnTablero.setOnClickListener {
            Toast.makeText(this, "Presionaste Tablero", Toast.LENGTH_SHORT).show()
        }

        btnMinijuegos.setOnClickListener {
            Toast.makeText(this, "Presionaste Minijuegos", Toast.LENGTH_SHORT).show()
        }
    }
}

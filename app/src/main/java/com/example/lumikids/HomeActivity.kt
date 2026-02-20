package com.example.lumikids

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import android.content.Intent

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnTablero = findViewById<ImageView>(R.id.btnTablero)
        val btnMinijuegos = findViewById<ImageView>(R.id.btnMinijuegos)

        val ivProfile = findViewById<ImageView>(R.id.ivProfile)

        ivProfile.setOnClickListener {

            // Intent para abrir UserActivity
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }


        btnTablero.setOnClickListener {
            Toast.makeText(this, "Presionaste Tablero", Toast.LENGTH_SHORT).show()
        }

        btnMinijuegos.setOnClickListener {
            Toast.makeText(this, "Presionaste Minijuegos", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, ThemeGameActivity::class.java)
            startActivity(intent)
        }

    }
}

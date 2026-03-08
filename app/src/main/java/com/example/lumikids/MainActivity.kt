package com.example.lumikids

import com.example.lumikids.ui.ProfileActivity
import com.example.lumikids.ui.ThemeGameActivity
import android.content.Intent
import android.widget.ImageView
import android.widget.Toast

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
      setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        val btnTablero = findViewById<ImageView>(R.id.btnTablero)
        val btnMinijuegos = findViewById<ImageView>(R.id.btnMinijuegos)
        val ivProfile = findViewById<ImageView>(R.id.ivProfile)

        // 3. Eventos de clic y redireccionamiento

        // Redirección al Perfil
        ivProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Clic en Tablero
        btnTablero.setOnClickListener {
            Toast.makeText(this, "Presionaste Tablero", Toast.LENGTH_SHORT).show()
            // Aquí podrás poner tu Intent hacia la actividad del Tablero en el futuro
        }

        // Redirección a Minijuegos
        btnMinijuegos.setOnClickListener {
            Toast.makeText(this, "Presionaste Minijuegos", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ThemeGameActivity::class.java)
            startActivity(intent)
        }
    }
}
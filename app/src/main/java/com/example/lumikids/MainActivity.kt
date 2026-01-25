package com.example.lumikids

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Esto va primero
        enableEdgeToEdge()

        // 2. Cargas el diseño (SOLO UNA VEZ)
        setContentView(R.layout.activity_main)

        // 3. Configuras los márgenes para la barra de estado
        // IMPORTANTE: Asegúrate de que el ID exista en el XML (ver Paso 2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
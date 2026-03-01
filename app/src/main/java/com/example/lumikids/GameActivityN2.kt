package com.example.lumikids

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class GameActivityN2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Aquí conectamos tu nuevo diseño XML (asegúrate de que el archivo XML se llame activity_game_n2)
        setContentView(R.layout.activity_game_n2)

        // Botón para regresar
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Cierra esta pantalla y vuelve a la anterior
        }

        // (El botón de pausa y las 10 tarjetas ya están en pantalla,
        //  pero aún no harán nada al tocarlas).
    }
}
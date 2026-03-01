package com.example.lumikids

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class GameTypeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_type)

        // Botón regresar
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Recibir temática seleccionada
        val theme = intent.getStringExtra("THEME")

        //  Tarjetas
        val cardObject = findViewById<ImageView>(R.id.cardObjeto)
        val cardMemory = findViewById<ImageView>(R.id.cardMemorama)

        //  Identifica el objeto
        cardObject.setOnClickListener {

            val intent = Intent(this, GameActivityN1::class.java)
            intent.putExtra("THEME", theme)
            intent.putExtra("GAME_TYPE", "OBJECT")
            startActivity(intent)
            finish()
        }

        // Memorama
        cardMemory.setOnClickListener {

            val intent = Intent(this, GameActivityN2::class.java)
            intent.putExtra("THEME", theme)
            intent.putExtra("GAME_TYPE", "MEMORY")
            startActivity(intent)
            finish()
        }


    }
}

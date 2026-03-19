package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.ui.MemoryLevelActivity
import com.example.lumikids.minigame.objectrecognition.ui.GameActivityN1

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

            val intent = Intent(this, ObjectLevelPickerActivity::class.java)
            intent.putExtra("THEME", theme)
            intent.putExtra("GAME_TYPE", "OBJECT")
            startActivity(intent)
            finish()
        }

        // Memorama
        cardMemory.setOnClickListener {

            val intent = Intent(this, MemoryLevelActivity::class.java)
            intent.putExtra("THEME", theme)
            intent.putExtra("GAME_TYPE", "MEMORY")
            startActivity(intent)
            finish()
        }


    }
}

package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.model.GameTheme

class GameTypeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_type)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val theme = intent.getStringExtra("THEME")?.let {
            GameTheme.valueOf(it)
        } ?: return

        // Tarjetas
        val cardObject = findViewById<ImageView>(R.id.cardObjeto)
        val cardMemory = findViewById<ImageView>(R.id.cardMemorama)

        // Eventos SIN GameType
        cardObject.setOnClickListener {
            openGame(ObjectLevelPickerActivity::class.java, theme)
        }

        cardMemory.setOnClickListener {
            openGame(MemoryLevelActivity::class.java, theme)
        }
    }

    private fun openGame(
        activity: Class<*>,
        theme: GameTheme
    ) {
        val intent = Intent(this, activity)
        intent.putExtra("THEME", theme.name)
        startActivity(intent)
    }
}
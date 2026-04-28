package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import com.example.lumikids.R
import androidx.activity.enableEdgeToEdge

class GameTypeActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_type)
        enableEdgeToEdge()



        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        val theme = intent.getStringExtra("THEME") ?: "furniure"

        val cardObject = findViewById<ImageView>(R.id.cardObjeto)
        val cardMemory = findViewById<ImageView>(R.id.cardMemorama)

        cardObject.setOnClickListener {
            openGame(ObjectLevelPickerActivity::class.java, theme)
        }
        cardMemory.setOnClickListener {
            openGame(MemoryLevelActivity::class.java, theme)
        }
    }



    private fun openGame(
        activity: Class<*>,
        theme: String
    ) {
        val intent = Intent(this, activity)
        intent.putExtra("THEME", theme)
        startActivity(intent)
    }
}
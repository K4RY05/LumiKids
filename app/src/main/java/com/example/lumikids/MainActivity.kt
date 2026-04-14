package com.example.lumikids

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.lumikids.ui.PecsBoardActivity
import com.example.lumikids.ui.ProfileActivity
import com.example.lumikids.ui.ThemeGameActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)

        val btnTablero = findViewById<ImageView>(R.id.btnTablero)
        val btnMinijuegos = findViewById<ImageView>(R.id.btnMinijuegos)
        val ivProfile = findViewById<ImageView>(R.id.ivProfile)

        ivProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        btnTablero.setOnClickListener {
            Toast.makeText(this, "Abriendo tablero", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, PecsBoardActivity::class.java)
            startActivity(intent)
        }

        btnMinijuegos.setOnClickListener {
            Toast.makeText(this, "Abriendo minijuegos", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, ThemeGameActivity::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            //v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)

            insets
        }
    }
}
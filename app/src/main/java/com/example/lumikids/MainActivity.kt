package com.example.lumikids

import com.example.lumikids.ui.ProfileActivity
import com.example.lumikids.ui.ThemeGameActivity

import android.content.Intent
import android.widget.ImageView
import android.widget.Toast

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lumikids.ui.BaseActivity
import com.example.lumikids.ui.PecsBoardActivity

class MainActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupImmersiveMode()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

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
            Toast.makeText(this, "Presionaste Minijuegos", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ThemeGameActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

}
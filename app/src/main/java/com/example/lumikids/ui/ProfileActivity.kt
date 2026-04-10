package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.lumikids.R
import com.example.lumikids.utils.SessionManager
import com.example.lumikids.utils.UserManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var userManager: UserManager
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userManager = UserManager(this)
        sessionManager = SessionManager(this)

        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        val btnDatos = findViewById<AppCompatButton>(R.id.btnDatos)
        val btnNotificaciones = findViewById<AppCompatButton>(R.id.btnNotificaciones)
        val btnTemas = findViewById<AppCompatButton>(R.id.btnTemas)
        val btnCerrarSesion = findViewById<AppCompatButton>(R.id.btnCerrarSesion)

        val nombreUsuario = userManager.getUserName()
        tvTitle.text = if (nombreUsuario.isNotEmpty()) nombreUsuario else "Usuario"



        btnDatos.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        btnNotificaciones.setOnClickListener {
            // Lógica para notificaciones en el futuro
        }

        btnTemas.setOnClickListener {
            // Lógica para temas de colores en el futuro
        }

        btnCerrarSesion.setOnClickListener {
            sessionManager.logout()

            // Redirigir al Login y limpiar el historial
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
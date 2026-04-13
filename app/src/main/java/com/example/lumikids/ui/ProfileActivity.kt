package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.utils.SessionManager


class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val txtTitulo = findViewById<TextView>(R.id.tvTitle)

        val btnDatos = findViewById<Button>(R.id.btnDatos)
        val btnNotificaciones = findViewById<Button>(R.id.btnNotificaciones)
        val btnTemas = findViewById<Button>(R.id.btnTemas)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        txtTitulo.text = "Configuraciones"

        btnDatos.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        btnNotificaciones.setOnClickListener {

        }

        btnTemas.setOnClickListener {
            // Abrir temas de colores (pendiente de implementar)
            Toast.makeText(this, "Temas de colores", Toast.LENGTH_SHORT).show()
        }

        btnCerrarSesion.setOnClickListener {
            // 1. Borrar los datos de sesión
            val sessionManager = SessionManager(this)
            sessionManager.logout()

            // 2. Redirigir a LoginActivity y limpiar pila
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
package com.example.lumikids.ui

import android.content.Intent // Asegúrate de importar Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.utils.SessionManager // Importa tu SessionManager

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val imgUsuario = findViewById<ImageView>(R.id.imgUsuario)
        val txtTitulo = findViewById<TextView>(R.id.txtTitulo)

        val btnDatos = findViewById<Button>(R.id.btnDatos)
        val btnNotificaciones = findViewById<Button>(R.id.btnNotificaciones)
        val btnTemas = findViewById<Button>(R.id.btnTemas)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        txtTitulo.text = "Usuario"

        btnDatos.setOnClickListener {
            // Abrir datos personales
        }

        btnNotificaciones.setOnClickListener {
            Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }

        btnTemas.setOnClickListener {
            // Abrir temas de colores
        }

        // --- SOLUCIÓN AQUÍ ---
        btnCerrarSesion.setOnClickListener {
            // 1. Borrar los datos de SharedPreferences
            val sessionManager = SessionManager(this)
            sessionManager.logout()

            // 2. Redirigir a LoginActivity y limpiar la pila de actividades
            val intent = Intent(this, LoginActivity::class.java)
            // Estas flags evitan que el usuario regrese a la app si presiona el botón "Atrás"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
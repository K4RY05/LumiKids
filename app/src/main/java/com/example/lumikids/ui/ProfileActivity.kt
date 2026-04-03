package com.example.lumikids.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R

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
            // Abrir notificaciones
        }

        btnTemas.setOnClickListener {
            // Abrir temas de colores
        }

        btnCerrarSesion.setOnClickListener {
            finish() // Cierra la pantalla
        }
    }
}

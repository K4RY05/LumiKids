package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.example.lumikids.R
import com.example.lumikids.utils.SessionManager

class ProfileActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager

    private var currentTheme: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        sessionManager = SessionManager(this)

        currentTheme = sessionManager.getTheme()

        when (currentTheme) {
            "green" -> setTheme(R.style.Theme_LumiKids_Green)
            "red" -> setTheme(R.style.Theme_LumiKids_Red)
            "pink" -> setTheme(R.style.Theme_LumiKids_Pink)
            else -> setTheme(R.style.Theme_LumiKids_Blue)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val txtTitulo = findViewById<TextView>(R.id.tvTitle)
        val btnDatos = findViewById<Button>(R.id.btnDatos)
        val btnNotificaciones = findViewById<Button>(R.id.btnNotificaciones)
        val btnTemas = findViewById<Button>(R.id.btnTemas)
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

        txtTitulo.text = "Configuraciones"

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        btnDatos.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        btnNotificaciones.setOnClickListener {
        }

        btnTemas.setOnClickListener {
            val intent = Intent(this, ThemeColorActivity::class.java)
            startActivity(intent)
        }

        btnCerrarSesion.setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

}
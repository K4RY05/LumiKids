package com.example.lumikids.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.utils.SessionManager

class ThemeColorActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Inicializar SessionManager primero para poder leer la preferencia
        sessionManager = SessionManager(this)

        // 2. Aplicar el tema ANTES de super.onCreate y setContentView (OBLIGATORIO)
        applySavedTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color)

        // 3. Vincular los botones usando los IDs exactos de activity_theme.xml
        findViewById<View>(R.id.btnTheme1).setOnClickListener { changeTheme("blue") }
        findViewById<View>(R.id.btnTheme2).setOnClickListener { changeTheme("green") }
        findViewById<View>(R.id.btnTheme3).setOnClickListener { changeTheme("red") }
        findViewById<View>(R.id.btnTheme4).setOnClickListener { changeTheme("pink") }

        // 4. Configurar el botón de regreso
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish() // Cierra esta pantalla y regresa al perfil
        }
    }

    private fun applySavedTheme() {
        val themeKey = sessionManager.getTheme()

        when (themeKey) {
            "green" -> setTheme(R.style.Theme_LumiKids_Green)
            "red" -> setTheme(R.style.Theme_LumiKids_Red)
            "pink" -> setTheme(R.style.Theme_LumiKids_Pink)
            else -> setTheme(R.style.Theme_LumiKids_Blue)
        }
    }

    private fun changeTheme(themeName: String) {
        // 1. Guardar la nueva elección de color
        sessionManager.setTheme(themeName)
        Toast.makeText(this, "Paleta actualizada", Toast.LENGTH_SHORT).show()

        // 2. Usar recreate() en lugar de finish() + startActivity()
        // Esto evita el error de Binder (Operation not permitted) en dispositivos con capas de personalización estrictas.
        recreate()
    }
}
package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.lumikids.R
import com.nulabinc.zxcvbn.Zxcvbn

class RegisterActivity : AppCompatActivity() {

    private var isPassVisible = false
    private var isPass2Visible = false
    private val TAG = "REGISTER_DEBUG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsuario = findViewById<EditText>(R.id.etUsername)
        val etCorreo = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPass)
        val etPassConfirm = findViewById<EditText>(R.id.etPass2)
        val btnContinuar = findViewById<AppCompatButton>(R.id.btnContinue)

        val ivTogglePass = findViewById<ImageButton>(R.id.ivTogglePass)
        val ivTogglePass2 = findViewById<ImageButton>(R.id.ivTogglePass2)

        ivTogglePass.setOnClickListener {
            isPassVisible = !isPassVisible
            togglePasswordVisibility(etPass, ivTogglePass, isPassVisible)
        }

        ivTogglePass2.setOnClickListener {
            isPass2Visible = !isPass2Visible
            togglePasswordVisibility(etPassConfirm, ivTogglePass2, isPass2Visible)
        }

        btnContinuar.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val pass = etPass.text.toString()
            val passConfirm = etPassConfirm.text.toString()

            if (usuario.isEmpty() || correo.isEmpty() || pass.isEmpty() || passConfirm.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 8) {
                Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val zxcvbn = Zxcvbn()
            val strength = zxcvbn.measure(pass)

            if (strength.score < 2) {
                val feedbackUsuario = getForceMessage(strength.feedback.warning, pass)
                Toast.makeText(this, feedbackUsuario, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (pass != passConfirm) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ID_user = createUserId(usuario)

            // --- CAMBIO AQUÍ: No registramos aún, enviamos los datos a TermsActivity ---
            Log.d(TAG, "Redirigiendo a TermsActivity con los datos del usuario en espera.")
            val intent = Intent(this, TermsActivity::class.java).apply {
                putExtra("EXTRA_ID_USER", ID_user)
                putExtra("EXTRA_USERNAME", usuario)
                putExtra("EXTRA_EMAIL", correo)
                putExtra("EXTRA_PASSWORD", pass)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun getForceMessage(warning: String, pass: String): String {
        if (warning.isNotEmpty()) {
            return when {
                warning.contains("repeated", ignoreCase = true) || warning.contains("repeat", ignoreCase = true) -> "La contraseña es muy repetitiva (ej. aaaa o 111)."
                warning.contains("sequence", ignoreCase = true) -> "Evita usar secuencias como abc o 123."
                warning.contains("common", ignoreCase = true) || warning.contains("dictionary", ignoreCase = true) -> "Esta contraseña es demasiado común o fácil de adivinar."
                warning.contains("name", ignoreCase = true) || warning.contains("surname", ignoreCase = true) -> "Evita usar nombres propios o apellidos."
                else -> "Contraseña débil. Agrega más complejidad."
            }
        }
        val tieneNumero = pass.any { it.isDigit() }
        val tieneCaracterEspecial = pass.any { !it.isLetterOrDigit() }

        return when {
            pass.length < 8 -> "Tu contraseña es muy corta. Usa al menos 8 caracteres."
            !tieneNumero -> "Contraseña débil: Te falta agregar al menos un número."
            !tieneCaracterEspecial -> "Contraseña débil: Agrega un carácter especial (ej. @, #, !)."
            else -> "La contraseña es muy sencilla. Agrega más letras, números o símbolos."
        }
    }

    private fun togglePasswordVisibility(editText: EditText, button: ImageButton, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            button.setImageResource(R.drawable.ic_eye_open)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setImageResource(R.drawable.ic_eye_closed)
        }
        editText.setSelection(editText.text.length)
    }

    private fun createUserId(nombre: String): String {
        val letras = if (nombre.length >= 3) nombre.substring(0, 3).lowercase() else nombre.lowercase().padEnd(3, 'x')
        val numeros = (100..999).random()
        return "$letras$numeros"
    }
}
package com.example.lumikids

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var tilEmail: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PRIMERO el layout
        setContentView(R.layout.activity_login)

        // DESPUÉS los findViewById
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvSignUp = findViewById(R.id.tvSignUp)
        tilEmail = findViewById(R.id.tilEmail)

        setupListeners()
        setupPasswordToggle()
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            validarLogin()
        }

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupPasswordToggle() {
        ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.transformationMethod =
                PasswordTransformationMethod.getInstance()
            ivTogglePassword.setImageResource(R.drawable.ic_eye_closed)
        } else {
            etPassword.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            ivTogglePassword.setImageResource(R.drawable.ic_eye_open)
        }

        etPassword.setSelection(
            etPassword.text?.length ?: 0
        )

        isPasswordVisible = !isPasswordVisible
    }

    private fun validarLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // limpiar error previo
        tilEmail.error = null

        var isValid = true

        if (email.isEmpty()) {
            tilEmail.error = "El correo es obligatorio"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Correo no válido"
            isValid = false
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "La contraseña es obligatoria", Toast.LENGTH_SHORT).show()
            isValid = false
        } else if (password.length < 6) {
            Toast.makeText(this, "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (isValid) {
            Toast.makeText(
                this,
                "Datos correctos (aún sin backend)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

package com.example.lumikids

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupPasswordToggle()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            validarLogin()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // 👁️ MOSTRAR / OCULTAR CONTRASEÑA (IGUAL QUE REGISTER)
    private fun setupPasswordToggle() {
        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Ocultar contraseña
            binding.etPassword.transformationMethod =
                PasswordTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_closed)
        } else {
            // Mostrar contraseña
            binding.etPassword.transformationMethod =
                HideReturnsTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_open)
        }

        // Mantener cursor al final
        binding.etPassword.setSelection(binding.etPassword.text.length)

        isPasswordVisible = !isPasswordVisible
    }

    private fun validarLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Limpiar errores previos
        binding.tilEmail.error = null

        var isValid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = "El correo es obligatorio"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Correo no válido"
            isValid = false
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "La contraseña es obligatoria", Toast.LENGTH_SHORT).show()
            isValid = false
        } else if (password.length < 6) {
            Toast.makeText(this, "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        // Feedback SOLO FRONTEND
        if (isValid) {
            Toast.makeText(
                this,
                "Datos correctos (aún sin backend)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

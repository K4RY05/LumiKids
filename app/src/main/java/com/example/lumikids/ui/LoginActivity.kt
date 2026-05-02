package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.MainActivity
import com.example.lumikids.R
import com.example.lumikids.databinding.ActivityLoginBinding
import com.example.lumikids.model.ApiResponse
import com.example.lumikids.model.LoginRequest
import com.example.lumikids.network.AuthApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.SessionManager
import com.google.android.material.snackbar.Snackbar
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Si ya está logueado, ir directo a Main
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // Mostrar texto
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_open)
            } else {
                // Ocultar texto
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_closed)
            }
            // Mantener el cursor al final
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }

        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            binding.etEmail.error = null
            binding.etPassword.error = null

            // 2. Validaciones con feedback visual (UX)
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.error = "Ingresa un correo electrónico válido"
                binding.etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Ingresa tu contraseña"
                binding.etPassword.requestFocus()
                return@setOnClickListener
            }

            loginUsuario(email, password)
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUsuario(email: String, password: String) {

        val api = RetrofitClient.instance.create(AuthApi::class.java)
        val request = LoginRequest(email, password)

        api.login(request).enqueue(object : Callback<ApiResponse> {

            override fun onResponse(
                call: Call<ApiResponse>,
                response: Response<ApiResponse>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {

                    // 1. Obtenemos el userId de la respuesta
                    val userId = response.body()?.userId.toString()

                    // 2. Guardamos la sesión con ambos datos
                    sessionManager.saveLogin(email, userId)

                    // 3. Mostramos mensaje de éxito y cambiamos de pantalla
                    Toast.makeText(
                        this@LoginActivity,
                        response.body()?.message ?: "Bienvenido",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                } else {
                    Snackbar.make(
                        binding.root,
                        response.body()?.message ?: "Credenciales incorrectas",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(
                call: Call<ApiResponse>,
                t: Throwable
            ) {
                Snackbar.make(
                    binding.root,
                    "Error de conexión: Verifica tu internet o el servidor local",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        })
    }
}
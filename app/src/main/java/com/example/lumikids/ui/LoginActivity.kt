package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.MainActivity
import com.example.lumikids.databinding.ActivityLoginBinding
import com.example.lumikids.model.ApiResponse
import com.example.lumikids.model.LoginRequest
import com.example.lumikids.network.AuthApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

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

        binding.btnLogin.setOnClickListener {

            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // Validar campos vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar formato email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Correo electrónico inválido", Toast.LENGTH_SHORT).show()
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

                    // Guardar sesión
                    sessionManager.saveLogin(email)

                    Toast.makeText(
                        this@LoginActivity,
                        response.body()?.message ?: "Bienvenido",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                } else {

                    Toast.makeText(
                        this@LoginActivity,
                        response.body()?.message ?: "Credenciales incorrectas",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(
                call: Call<ApiResponse>,
                t: Throwable
            ) {
                Toast.makeText(
                    this@LoginActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}
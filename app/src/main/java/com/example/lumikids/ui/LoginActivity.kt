package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.text.InputType
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
import com.example.lumikids.utils.UserManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var userManager: UserManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        userManager = UserManager(this)

        // 1. Verificar sesión primero
        if (sessionManager.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // 2. Inflar el binding UNA SOLA VEZ
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. Configurar lógica del botón de Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Correo electrónico inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginUsuario(email, password)
        }

        // 4. Configurar lógica para ir al Registro
        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // 5. Lógica para mostrar/ocultar contraseña (Ojo)
        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // Mostrar contraseña
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_open)
            } else {
                // Ocultar contraseña
                binding.etPassword.inputType = InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_closed)
            }

            // Mantener el cursor al final del texto
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    private fun loginUsuario(email: String, password: String) {
        val api = RetrofitClient.instance.create(AuthApi::class.java)
        val request = LoginRequest(email, password)

        api.login(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val body = response.body() // Guardamos el cuerpo de la respuesta

                if (response.isSuccessful && body?.success == true) {
                    // --- LO QUE SE AÑADIÓ ---
                    sessionManager.saveLogin(email)

                    // Guardamos el ID del usuario para futuras ediciones
                    body.ID_user?.let { sessionManager.saveUserId(it) }

                    // Guardamos Nombre y Correo en UserManager para que se vean en el Perfil
                    userManager.saveUserData(
                        body.name ?: "Usuario",
                        body.email ?: email
                    )
                    // -------------------------

                    Toast.makeText(this@LoginActivity, body.message ?: "Bienvenido", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, body?.message ?: "Credenciales incorrectas", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}
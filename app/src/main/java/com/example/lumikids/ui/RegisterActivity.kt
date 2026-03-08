package com.example.lumikids.ui

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.lumikids.R
import com.example.lumikids.network.AuthApi
import com.example.lumikids.model.RegisterRequest
import com.example.lumikids.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsuario = findViewById<EditText>(R.id.etUsername)
        val etCorreo = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPass)
        val etPassConfirm = findViewById<EditText>(R.id.etPass2)
        val btnContinuar = findViewById<AppCompatButton>(R.id.btnContinue)

        btnContinuar.setOnClickListener {

            val usuario = etUsuario.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val pass = etPass.text.toString()
            val passConfirm = etPassConfirm.text.toString()

            // Validar campos vacíos
            if (usuario.isEmpty() || correo.isEmpty() || pass.isEmpty() || passConfirm.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar correo
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar contraseñas
            if (pass != passConfirm) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generar ID de usuario
            val ID_user = generarUserId(usuario)

            registrarUsuario(ID_user, usuario, correo, pass)
        }
    }

    // Función para generar ID (3 letras + 3 números)
    private fun generarUserId(nombre: String): String {

        val letras = if (nombre.length >= 3) {
            nombre.substring(0, 3).lowercase()
        } else {
            nombre.lowercase().padEnd(3, 'x')
        }

        val numeros = (100..999).random()

        return "$letras$numeros"
    }

    private fun registrarUsuario(ID_user: String, nombre: String, email: String, password: String) {

        val api = RetrofitClient.instance.create(AuthApi::class.java)

        val request = RegisterRequest(
            ID_user = ID_user,
            name = nombre,
            email = email,
            password = password
        )

        api.register(request).enqueue(object : Callback<com.example.lumikids.model.ApiResponse> {

            override fun onResponse(
                call: Call<com.example.lumikids.model.ApiResponse>,
                response: Response<com.example.lumikids.model.ApiResponse>
            ) {
                if (response.isSuccessful) {

                    Toast.makeText(
                        this@RegisterActivity,
                        response.body()?.message ?: "Registro exitoso",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                } else {

                    Toast.makeText(
                        this@RegisterActivity,
                        "Error en el registro",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(
                call: Call<com.example.lumikids.model.ApiResponse>,
                t: Throwable
            ) {
                Toast.makeText(
                    this@RegisterActivity,
                    "Error de conexión: ${t.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}
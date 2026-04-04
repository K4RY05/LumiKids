package com.example.lumikids.ui

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.lumikids.R
import com.example.lumikids.network.AuthApi
import com.example.lumikids.model.RegisterRequest
import com.example.lumikids.network.RetrofitClient
import com.google.android.material.snackbar.Snackbar // 🔥 Usaremos Snackbar para errores de servidor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private var isPasswordConfirmVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etUsuario = findViewById<EditText>(R.id.etUsername)
        val etCorreo = findViewById<EditText>(R.id.etEmail)
        val etPass = findViewById<EditText>(R.id.etPass)
        val etPassConfirm = findViewById<EditText>(R.id.etPass2)
        val btnContinuar = findViewById<AppCompatButton>(R.id.btnContinue)

        // Botones de mostrar/ocultar contraseña
        val ivTogglePass = findViewById<ImageButton>(R.id.ivTogglePass)
        val ivTogglePass2 = findViewById<ImageButton>(R.id.ivTogglePass2)

        // Lógica para mostrar/ocultar contraseña principal
        ivTogglePass.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(etPass, ivTogglePass, isPasswordVisible)
        }

        // Lógica para mostrar/ocultar confirmación de contraseña
        ivTogglePass2.setOnClickListener {
            isPasswordConfirmVisible = !isPasswordConfirmVisible
            togglePasswordVisibility(etPassConfirm, ivTogglePass2, isPasswordConfirmVisible)
        }

        btnContinuar.setOnClickListener {

            val usuario = etUsuario.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val pass = etPass.text.toString()
            val passConfirm = etPassConfirm.text.toString()

            // 1. Limpiar errores previos
            etUsuario.error = null
            etCorreo.error = null
            etPass.error = null
            etPassConfirm.error = null

            // 2. Validaciones con feedback visual (de arriba hacia abajo)
            if (usuario.isEmpty()) {
                etUsuario.error = "Ingresa tu nombre de usuario"
                etUsuario.requestFocus()
                return@setOnClickListener
            }

            if (correo.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                etCorreo.error = "Ingresa un correo electrónico válido"
                etCorreo.requestFocus()
                return@setOnClickListener
            }

            if (pass.isEmpty()) {
                etPass.error = "Ingresa una contraseña"
                etPass.requestFocus()
                return@setOnClickListener
            }

            // Buena práctica: Validar longitud mínima
            if (pass.length < 6) {
                etPass.error = "La contraseña debe tener al menos 6 caracteres"
                etPass.requestFocus()
                return@setOnClickListener
            }

            if (passConfirm.isEmpty() || pass != passConfirm) {
                etPassConfirm.error = "Las contraseñas no coinciden"
                etPassConfirm.requestFocus()
                return@setOnClickListener
            }

            // Generar ID de usuario y registrar
            val ID_user = generarUserId(usuario)
            registrarUsuario(ID_user, usuario, correo, pass)
        }
    }

    // Función auxiliar para cambiar el tipo de input y el ícono
    private fun togglePasswordVisibility(editText: EditText, icon: ImageButton, isVisible: Boolean) {
        if (isVisible) {
            // Mostrar texto
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            icon.setImageResource(R.drawable.ic_eye_open) // Asumiendo que tienes este ícono
        } else {
            // Ocultar texto
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            icon.setImageResource(R.drawable.ic_eye_closed)
        }
        // Mantener el cursor al final del texto
        editText.setSelection(editText.text.length)
    }

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
                    finish() // Regresa a la pantalla de Login
                } else {
                    // Usamos Snackbar para errores que vienen del servidor (ej. "El correo ya existe")
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Error en el registro: Verifica tus datos o intenta con otro correo",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(
                call: Call<com.example.lumikids.model.ApiResponse>,
                t: Throwable
            ) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Error de conexión: ${t.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        })
    }
}
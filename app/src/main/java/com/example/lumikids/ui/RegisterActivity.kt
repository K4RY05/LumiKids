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
import com.example.lumikids.network.AuthApi
import com.example.lumikids.model.RegisterRequest
import com.example.lumikids.network.RetrofitClient
import com.nulabinc.zxcvbn.Zxcvbn
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                Log.w(TAG, "Registro fallido: El usuario dejó campos vacíos.")
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar correo
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Log.w(TAG, "Registro fallido: Formato de correo inválido -> $correo")
                Toast.makeText(this, "Ingresa un correo electrónico válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 8) {
                Log.w(TAG, "Registro fallido: Contraseña muy corta (${pass.length} caracteres).")
                Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }


            val zxcvbn = Zxcvbn()
            val strength = zxcvbn.measure(pass)

            if (strength.score < 2) {
                val feedbackUsuario = getForceMessage(strength.feedback.warning, pass)
                Log.w(TAG, "Registro fallido: Contraseña débil (Score: ${strength.score}) -> $feedbackUsuario")

                Toast.makeText(this, feedbackUsuario, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Validar contraseñas
            if (pass != passConfirm) {
                Log.w(TAG, "Registro fallido: Las contraseñas no coinciden.")
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generar ID de usuario
            val ID_user = createUserId(usuario)

            Log.d(TAG, "Iniciando petición de registro para: $usuario con ID: $ID_user")

            registerUser(ID_user, usuario, correo, pass)
        }
    }

    private fun getForceMessage(warning: String, pass: String): String {
        if (warning.isNotEmpty()) {
            return when {
                warning.contains("repeated", ignoreCase = true) || warning.contains("repeat", ignoreCase = true) ->
                    "La contraseña es muy repetitiva (ej. aaaa o 111)."
                warning.contains("sequence", ignoreCase = true) ->
                    "Evita usar secuencias como abc o 123."
                warning.contains("common", ignoreCase = true) || warning.contains("dictionary", ignoreCase = true) ->
                    "Esta contraseña es demasiado común o fácil de adivinar."
                warning.contains("name", ignoreCase = true) || warning.contains("surname", ignoreCase = true) ->
                    "Evita usar nombres propios o apellidos."
                else -> "Contraseña débil. Agrega más complejidad."
            }
        }

        val tieneNumero = pass.any { it.isDigit() }
        val tieneCaracterEspecial = pass.any { !it.isLetterOrDigit() }

        return when {
            pass.length < 8 -> "Tu contraseña es muy corta. Usa al menos 8 caracteres." // Actualizado a 8 por consistencia
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
        val letras = if (nombre.length >= 3) {
            nombre.substring(0, 3).lowercase()
        } else {
            nombre.lowercase().padEnd(3, 'x')
        }
        val numeros = (100..999).random()
        return "$letras$numeros"
    }

    private fun registerUser(ID_user: String, nombre: String, email: String, password: String) {

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
                    Log.d(TAG, "Registro en servidor EXITOSO. Respuesta: ${response.body()?.message}")

                    Toast.makeText(
                        this@RegisterActivity,
                        response.body()?.message ?: "Registro exitoso",
                        Toast.LENGTH_LONG
                    ).show()

                    val intent = Intent(this@RegisterActivity, TermsActivity::class.java)
                    startActivity(intent)

                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "ERROR EN SERVIDOR HTTP ${response.code()}: $errorBody")

                    var mensajeParaUsuario = "Error en el registro"

                    try {
                        if (!errorBody.isNullOrEmpty()) {
                            val jsonObject = org.json.JSONObject(errorBody)
                            mensajeParaUsuario = jsonObject.getString("message")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al intentar leer el JSON del servidor", e)
                    }

                    Toast.makeText(
                        this@RegisterActivity,
                        mensajeParaUsuario,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onFailure(
                call: Call<com.example.lumikids.model.ApiResponse>,
                t: Throwable
            ) {
                Log.e(TAG, "FALLO CRÍTICO DE RED/CONEXIÓN: ${t.message}", t)

                Toast.makeText(
                    this@RegisterActivity,
                    "Error de conexión: Verifica tu internet",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }
}
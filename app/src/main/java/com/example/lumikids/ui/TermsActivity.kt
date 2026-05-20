package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.model.ApiResponse
import com.example.lumikids.model.RegisterRequest
import com.example.lumikids.network.AuthApi
import com.example.lumikids.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader

class TermsActivity : AppCompatActivity() {

    private val TAG = "TERMS_DEBUG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        // Recibir los datos enviados desde RegisterActivity
        val idUser = intent.getStringExtra("EXTRA_ID_USER") ?: ""
        val username = intent.getStringExtra("EXTRA_USERNAME") ?: ""
        val email = intent.getStringExtra("EXTRA_EMAIL") ?: ""
        val password = intent.getStringExtra("EXTRA_PASSWORD") ?: ""

        val tvTerms = findViewById<TextView>(R.id.tvTermsText)
        tvTerms.text = readRawTextFile(R.raw.terms)

        val btnFinish = findViewById<Button>(R.id.btnFinish)
        val cbAccept = findViewById<CheckBox>(R.id.cbAccept)

        // El botón inicia deshabilitado y opaco
        btnFinish.isEnabled = false
        btnFinish.alpha = 0.5f

        cbAccept.setOnCheckedChangeListener { _, isChecked ->
            btnFinish.isEnabled = isChecked
            btnFinish.alpha = if (isChecked) 1.0f else 0.5f
        }

        btnFinish.setOnClickListener {
            // Proceder con el registro real en el servidor
            registerUserInServer(idUser, username, email, password)
        }
    }

    private fun registerUserInServer(ID_user: String, nombre: String, email: String, password: String) {
        val api = RetrofitClient.instance.create(AuthApi::class.java)
        val request = RegisterRequest(
            ID_user = ID_user,
            name = nombre,
            email = email,
            password = password
        )

        Log.d(TAG, "Enviando registro al servidor desde Términos: $nombre")

        api.register(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Registro EXITOSO. Respuesta: ${response.body()?.message}")
                    Toast.makeText(this@TermsActivity, response.body()?.message ?: "Registro exitoso", Toast.LENGTH_LONG).show()

                    // Redirigir al LoginActivity limpiando el historial
                    val intent = Intent(this@TermsActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
                        Log.e(TAG, "Error al leer JSON de error", e)
                    }
                    Toast.makeText(this@TermsActivity, mensajeParaUsuario, Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e(TAG, "FALLO DE CONEXIÓN: ${t.message}", t)
                Toast.makeText(this@TermsActivity, "Error de conexión: Verifica tu internet", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun readRawTextFile(resId: Int): String {
        val inputStream = resources.openRawResource(resId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val text = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            text.append(line).append('\n')
        }
        reader.close()
        return text.toString()
    }
}
package com.example.lumikids.ui

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.lumikids.R
import com.example.lumikids.model.UpdateProfileRequest
import com.example.lumikids.network.EditProfileApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    private var verifiedPassword = ""
    private var currentName = ""
    private var currentEmail = ""

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmNewPassword: EditText
    private lateinit var btnConfirm: AppCompatButton

    // Variables de estado para saber si el texto está visible o no
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        sessionManager = SessionManager(this)

        // Verificamos la sesión de inmediato
        if (sessionManager.getUserId() == null) {
            Toast.makeText(this, "Error de sesión.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Recibimos los datos enviados desde EditProfileActivity
        verifiedPassword = intent.getStringExtra("CURRENT_PASSWORD") ?: ""
        currentName = intent.getStringExtra("CURRENT_NAME") ?: ""
        currentEmail = intent.getStringExtra("CURRENT_EMAIL") ?: ""

        if (verifiedPassword.isEmpty()) {
            Toast.makeText(this, "Error de seguridad. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword)
        btnConfirm = findViewById(R.id.btnConfirm)
        val btnBack = findViewById<AppCompatButton>(R.id.btnBack)

        // Enlazamos los botones del "ojito" (Asegúrate de que estos IDs existan en tu XML)
        val btnToggleNew = findViewById<ImageButton>(R.id.btnToggleNewPassword)
        val btnToggleConfirm = findViewById<ImageButton>(R.id.btnToggleConfirmPassword)

        // Configuramos los listeners para alternar la visibilidad
        btnToggleNew.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            togglePasswordVisibility(etNewPassword, btnToggleNew, isNewPasswordVisible)
        }

        btnToggleConfirm.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(etConfirmNewPassword, btnToggleConfirm, isConfirmPasswordVisible)
        }

        btnConfirm.setOnClickListener {
            hideKeyboard()
            changePassword()
        }

        btnBack.setOnClickListener {
            hideKeyboard()
            finish()
        }
    }

    private fun changePassword() {
        val newPass = etNewPassword.text.toString().trim()
        val confirmPass = etConfirmNewPassword.text.toString().trim()

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Por favor completa ambos campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass != confirmPass) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        if (verifiedPassword == newPass) {
            Toast.makeText(this, "La nueva contraseña debe ser diferente a la actual", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = sessionManager.getUserId() ?: return

        btnConfirm.text = "Actualizando..."
        btnConfirm.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = UpdateProfileRequest(userId, currentName, currentEmail, verifiedPassword, newPass)

                val response = RetrofitClient.instance.create(EditProfileApi::class.java).updateProfile(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ChangePasswordActivity, "¡Contraseña actualizada con éxito!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        btnConfirm.text = "Guardar nueva contraseña"
                        btnConfirm.isEnabled = true

                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (!errorBody.isNullOrEmpty()) {
                            try {
                                org.json.JSONObject(errorBody).optString("message", "Error al actualizar")
                            } catch (_: Exception) { // Usamos '_' para ignorar la advertencia si falla el parseo
                                "Error en el formato de respuesta"
                            }
                        } else {
                            response.body()?.message ?: "Error desconocido"
                        }

                        Toast.makeText(this@ChangePasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                // Usamos 'e' para dejar un registro oculto en Logcat, útil para depurar
                android.util.Log.e("ChangePassword", "Fallo en la red: ${e.message}", e)

                withContext(Dispatchers.Main) {
                    btnConfirm.text = "Guardar nueva contraseña"
                    btnConfirm.isEnabled = true
                    Toast.makeText(this@ChangePasswordActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun hideKeyboard() {
        val view: View? = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // Nueva función para alternar la visibilidad
    private fun togglePasswordVisibility(editText: EditText, button: ImageButton, isVisible: Boolean) {
        if (isVisible) {
            // Mostrar contraseña
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            button.setImageResource(R.drawable.ic_eye_open)
        } else {
            // Ocultar contraseña
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setImageResource(R.drawable.ic_eye_closed)
        }
        // Mover el cursor al final del texto para que no salte al inicio
        editText.setSelection(editText.text.length)
    }
}
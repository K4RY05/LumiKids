package com.example.lumikids.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.lumikids.R
import com.example.lumikids.model.UpdateProfileRequest
import com.example.lumikids.network.AuthApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.SessionManager
import com.example.lumikids.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var userManager: UserManager

    private var verifiedPassword = ""

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmNewPassword: EditText // Agregamos la variable
    private lateinit var btnConfirm: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        sessionManager = SessionManager(this)
        userManager = UserManager(this)

        verifiedPassword = intent.getStringExtra("CURRENT_PASSWORD") ?: ""

        if (verifiedPassword.isEmpty()) {
            Toast.makeText(this, "Error de seguridad. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Vinculamos el nuevo campo
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword)
        btnConfirm = findViewById(R.id.btnConfirm)
        val btnBack = findViewById<AppCompatButton>(R.id.btnBack)

        btnConfirm.setOnClickListener {
            changePassword()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun changePassword() {
        val newPass = etNewPassword.text.toString().trim()
        val confirmPass = etConfirmNewPassword.text.toString().trim() // Leemos la confirmación

        // 1. Validar que no estén vacíos
        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            Toast.makeText(this, "Por favor completa ambos campos", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. 👉 NUEVO: Validar que las contraseñas coincidan
        if (newPass != confirmPass) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Validar que no sea igual a la actual
        if (verifiedPassword == newPass) {
            Toast.makeText(this, "La nueva contraseña debe ser diferente a la actual", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = sessionManager.getUserId() ?: return
        val name = userManager.getUserName()
        val email = userManager.getUserEmail()

        btnConfirm.text = "Actualizando..."
        btnConfirm.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = UpdateProfileRequest(userId, name, email, verifiedPassword, newPass)
                val response = RetrofitClient.instance.create(AuthApi::class.java).updateProfile(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@ChangePasswordActivity, "¡Contraseña actualizada con éxito!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        btnConfirm.text = "Guardar nueva contraseña"
                        btnConfirm.isEnabled = true
                        Toast.makeText(this@ChangePasswordActivity, response.body()?.message ?: "Error al actualizar", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnConfirm.text = "Guardar nueva contraseña"
                    btnConfirm.isEnabled = true
                    Toast.makeText(this@ChangePasswordActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
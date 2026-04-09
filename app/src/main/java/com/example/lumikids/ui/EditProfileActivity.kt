package com.example.lumikids.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.lumikids.R
import com.example.lumikids.model.ApiResponse
import com.example.lumikids.model.LoginRequest
import com.example.lumikids.model.UpdateProfileRequest
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.network.AuthApi
import com.example.lumikids.utils.SessionManager
import com.example.lumikids.utils.UserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response as RetrofitResponse

class EditProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var userManager: UserManager

    private var verifiedPassword = ""
    private var fieldWaitingToUnlock = ""

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnEditName: ImageButton
    private lateinit var btnEditEmail: ImageButton
    private lateinit var btnChangePassword: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)
        userManager = UserManager(this)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        btnEditName = findViewById(R.id.btnEditName)
        btnEditEmail = findViewById(R.id.btnEditEmail)
        btnChangePassword = findViewById(R.id.btnChangePassword)

        val btnBack = findViewById<AppCompatButton>(R.id.btnBack)
        val tvDelete = findViewById<TextView>(R.id.tvDelete)

        etName.setText(userManager.getUserName())
        etEmail.setText(userManager.getUserEmail())

        btnEditName.setOnClickListener {
            if (!etName.isEnabled) {
                fieldWaitingToUnlock = "name"
                if (verifiedPassword.isEmpty()) showPasswordDialog() else unlockField()
            } else saveChanges()
        }

        btnEditEmail.setOnClickListener {
            if (!etEmail.isEnabled) {
                fieldWaitingToUnlock = "email"
                if (verifiedPassword.isEmpty()) showPasswordDialog() else unlockField()
            } else saveChanges()
        }

        // 👉 NUEVO: Ahora el botón de Contraseña usa el Pop-up también
        btnChangePassword.setOnClickListener {
            if (verifiedPassword.isEmpty()) {
                fieldWaitingToUnlock = "password"
                showPasswordDialog()
            } else {
                goToChangePasswordScreen()
            }
        }

        btnBack.setOnClickListener {
            hideKeyboard()
            finish()
        }

        tvDelete.setOnClickListener {
            Toast.makeText(this, "Borrar cuenta en construcción", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verificación de Identidad")
        builder.setMessage("Por seguridad, ingresa tu contraseña actual:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Contraseña"

        val layout = FrameLayout(this)
        layout.setPadding(60, 20, 60, 0)
        layout.addView(input)
        builder.setView(layout)

        builder.setPositiveButton("Verificar") { _, _ ->
            val password = input.text.toString().trim()
            if (password.isNotEmpty()) verifyPassword(password)
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun verifyPassword(password: String) {
        val email = userManager.getUserEmail()
        val request = LoginRequest(email, password)

        val api = RetrofitClient.instance.create(AuthApi::class.java)
        api.login(request).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: RetrofitResponse<ApiResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    verifiedPassword = password
                    unlockField()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Contraseña incorrecta", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun unlockField() {
        if (fieldWaitingToUnlock == "name") {
            etName.isEnabled = true
            etName.requestFocus()
            showKeyboard(etName)
            btnEditName.setImageResource(android.R.drawable.ic_menu_save)
        } else if (fieldWaitingToUnlock == "email") {
            etEmail.isEnabled = true
            etEmail.requestFocus()
            showKeyboard(etEmail)
            btnEditEmail.setImageResource(android.R.drawable.ic_menu_save)
        } else if (fieldWaitingToUnlock == "password") {
            // 👉 Si el usuario quería cambiar clave, lo mandamos a la otra pantalla
            goToChangePasswordScreen()
        }
        fieldWaitingToUnlock = ""
    }

    // 👉 Función que abre la pantalla y le pasa la contraseña verificada en secreto
    private fun goToChangePasswordScreen() {
        val intent = Intent(this, ChangePasswordActivity::class.java)
        intent.putExtra("CURRENT_PASSWORD", verifiedPassword)
        startActivity(intent)
    }

    private fun saveChanges() {
        val newName = etName.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()

        if (newName.isEmpty() || newEmail.isEmpty()) return
        val userId = sessionManager.getUserId() ?: return

        btnEditName.isEnabled = false
        btnEditEmail.isEnabled = false
        hideKeyboard()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = UpdateProfileRequest(userId, newName, newEmail, verifiedPassword, null)
                val response = RetrofitClient.instance.create(AuthApi::class.java).updateProfile(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@EditProfileActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        userManager.saveUserData(newName, newEmail)
                        lockAllFields()
                    } else {
                        Toast.makeText(this@EditProfileActivity, response.body()?.message ?: "Error", Toast.LENGTH_SHORT).show()
                        btnEditName.isEnabled = true
                        btnEditEmail.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Error de red", Toast.LENGTH_SHORT).show()
                    btnEditName.isEnabled = true
                    btnEditEmail.isEnabled = true
                }
            }
        }
    }

    private fun lockAllFields() {
        verifiedPassword = ""
        etName.isEnabled = false
        btnEditName.isEnabled = true
        btnEditName.setImageResource(android.R.drawable.ic_menu_edit)
        etEmail.isEnabled = false
        btnEditEmail.isEnabled = true
        btnEditEmail.setImageResource(android.R.drawable.ic_menu_edit)
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
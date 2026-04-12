package com.example.lumikids.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.lumikids.R
import com.example.lumikids.model.ApiResponse
import com.example.lumikids.model.LoginRequest
import com.example.lumikids.model.UpdateProfileRequest
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.network.AuthApi
import com.example.lumikids.network.EditProfileApi
import com.example.lumikids.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response as RetrofitResponse

class EditProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var userId: String = ""
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
        val id = sessionManager.getUserId()

        if (id == null) {
            Toast.makeText(this, "Error de sesión. Vuelve a ingresar.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        userId = id

        initViews()
        setupListeners()
        // 👉 Ahora esta función sí realizará la carga desde el servidor
        cargarDatosUsuario()
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        btnEditName = findViewById(R.id.btnEditName)
        btnEditEmail = findViewById(R.id.btnEditEmail)
        btnChangePassword = findViewById(R.id.btnChangePassword)
    }

    private fun setupListeners() {
        findViewById<AppCompatButton>(R.id.btnBack).setOnClickListener {
            hideKeyboard()
            finish()
        }

        findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            mostrarDialogoEliminarCuenta()
        }

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

        btnChangePassword.setOnClickListener {
            if (verifiedPassword.isEmpty()) {
                fieldWaitingToUnlock = "password"
                showPasswordDialog()
            } else {
                goToChangePasswordScreen()
            }
        }
    }

    private fun cargarDatosUsuario() {
        // Le decimos a la corrutina que haga el trabajo de red en segundo plano (IO)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.instance.create(EditProfileApi::class.java)
                val response = api.getUserProfile(userId)

                // Una vez que Retrofit termine, cambiamos al hilo principal para actualizar la pantalla
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!
                        // ✅ Asignamos los datos obtenidos a los EditText con seguridad
                        etName.setText(user.name)
                        etEmail.setText(user.email)
                    } else {
                        Toast.makeText(this@EditProfileActivity, "Error al cargar datos del servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                // Si hay un error, también debemos mostrar el Toast en el hilo principal
                withContext(Dispatchers.Main) {
                    Log.e("EDIT_PROFILE", "Error al cargar datos: ${e.message}")
                    Toast.makeText(this@EditProfileActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
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
        val email = etEmail.text.toString().trim()
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
        when (fieldWaitingToUnlock) {
            "name" -> {
                etName.isEnabled = true
                etName.requestFocus()
                showKeyboard(etName)
                btnEditName.setImageResource(android.R.drawable.ic_menu_save)
            }
            "email" -> {
                etEmail.isEnabled = true
                etEmail.requestFocus()
                showKeyboard(etEmail)
                btnEditEmail.setImageResource(android.R.drawable.ic_menu_save)
            }
            "password" -> goToChangePasswordScreen()
        }
        fieldWaitingToUnlock = ""
    }

    private fun goToChangePasswordScreen() {
        val intent = Intent(this, ChangePasswordActivity::class.java)
        intent.putExtra("CURRENT_PASSWORD", verifiedPassword)
        intent.putExtra("CURRENT_NAME", etName.text.toString().trim())
        intent.putExtra("CURRENT_EMAIL", etEmail.text.toString().trim())
        startActivity(intent)
    }

    private fun saveChanges() {
        val newName = etName.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()

        if (newName.isEmpty() || newEmail.isEmpty()) return

        btnEditName.isEnabled = false
        btnEditEmail.isEnabled = false
        hideKeyboard()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = UpdateProfileRequest(userId, newName, newEmail, verifiedPassword, null)
                val response = RetrofitClient.instance.create(EditProfileApi::class.java).updateProfile(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@EditProfileActivity, "Perfil actualizado", Toast.LENGTH_SHORT).show()
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

    private fun mostrarDialogoEliminarCuenta() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Cuenta")
            .setMessage("¿Estás seguro de que deseas eliminar tu cuenta permanentemente? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> eliminarCuenta() }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCuenta() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.create(EditProfileApi::class.java).deleteAccount(userId)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@EditProfileActivity, "Cuenta eliminada", Toast.LENGTH_LONG).show()
                        sessionManager.logout()
                        val intent = Intent(this@EditProfileActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@EditProfileActivity, response.body()?.message ?: "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
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
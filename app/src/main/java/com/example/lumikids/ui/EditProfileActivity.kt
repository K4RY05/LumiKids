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
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.example.lumikids.R
import com.example.lumikids.model.LoginRequest
import com.example.lumikids.model.UpdateProfileRequest
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.network.AuthApi
import com.example.lumikids.network.EditProfileApi
import com.example.lumikids.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager
    private var userId: String = ""
    private var verifiedPassword = ""
    private var fieldWaitingToUnlock = ""
    private var originalName = ""
    private var originalEmail = ""

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText

    private lateinit var btnStartEditName: ImageButton
    private lateinit var btnStartEditEmail: ImageButton

    private lateinit var containerNameActions: LinearLayout
    private lateinit var containerEmailActions: LinearLayout
    private lateinit var btnSaveName: AppCompatButton
    private lateinit var btnCancelName: AppCompatButton
    private lateinit var btnSaveEmail: AppCompatButton
    private lateinit var btnCancelEmail: AppCompatButton

    private lateinit var btnChangePassword: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)
        val id = sessionManager.getUserId()

        // Si el ID es nulo o "null", redirigir al login
        if (id == null) {
            Toast.makeText(this, "Sesión no válida. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        userId = id

        initViews()
        setupListeners()
        fetchUserData()
    }

    private fun initViews() {
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        btnStartEditName = findViewById(R.id.btnStartEditName)
        btnStartEditEmail = findViewById(R.id.btnStartEditEmail)
        containerNameActions = findViewById(R.id.containerNameActions)
        containerEmailActions = findViewById(R.id.containerEmailActions)
        btnSaveName = findViewById(R.id.btnSaveName)
        btnCancelName = findViewById(R.id.btnCancelName)
        btnSaveEmail = findViewById(R.id.btnSaveEmail)
        btnCancelEmail = findViewById(R.id.btnCancelEmail)
        btnChangePassword = findViewById(R.id.btnChangePassword)
    }

    private fun setupListeners() {
        findViewById<AppCompatButton>(R.id.btnBack).setOnClickListener {
            hideKeyboard()
            finish()
        }

        findViewById<TextView>(R.id.tvDelete).setOnClickListener {
            showDeleteAccountDialog()
        }

        btnStartEditName.setOnClickListener {
            if (etEmail.isEnabled) {
                Toast.makeText(this, "Guarda los cambios de tu correo primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            fieldWaitingToUnlock = "name"
            showPasswordDialog()
        }

        btnCancelName.setOnClickListener {
            etName.setText(originalName)
            lockAllFields()
            hideKeyboard()
        }

        btnSaveName.setOnClickListener { saveChanges("name") }

        btnStartEditEmail.setOnClickListener {
            if (etName.isEnabled) {
                Toast.makeText(this, "Guarda los cambios de tu nombre primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            fieldWaitingToUnlock = "email"
            showPasswordDialog()
        }

        btnCancelEmail.setOnClickListener {
            etEmail.setText(originalEmail)
            lockAllFields()
            hideKeyboard()
        }

        btnSaveEmail.setOnClickListener { saveChanges("email") }

        btnChangePassword.setOnClickListener {
            if (etName.isEnabled || etEmail.isEnabled) {
                Toast.makeText(this, "Guarda tus cambios antes de cambiar la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            fieldWaitingToUnlock = "password"
            showPasswordDialog()
        }
    }

    private fun fetchUserData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("EDIT_PROFILE_DEBUG", "Solicitando datos para: $userId")
                val api = RetrofitClient.instance.create(EditProfileApi::class.java)
                val response = api.getUserProfile(userId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!
                        // Asignación de datos desde UserProfileResponse (usa ID_user internamente)
                        etName.setText(user.name)
                        etEmail.setText(user.email)
                        originalName = user.name
                        originalEmail = user.email
                        Log.d("EDIT_PROFILE_DEBUG", "Datos cargados: ${user.name}")
                    } else {
                        val code = response.code()
                        Log.e("EDIT_PROFILE_ERROR", "Error HTTP: $code")
                        Toast.makeText(this@EditProfileActivity, "Error servidor ($code). Revisa tu conexión.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EDIT_PROFILE_EXCEPTION", "Fallo: ${e.message}")
                    Toast.makeText(this@EditProfileActivity, "Error de red: Datos no recibidos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verificación")
        builder.setMessage("Ingresa tu contraseña para editar:")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.hint = "Contraseña actual"

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
        // Se usa el email original para verificar contra el endpoint de login
        val request = LoginRequest(originalEmail, password)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.instance.create(AuthApi::class.java)
                val response = api.login(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        verifiedPassword = password
                        unlockField()
                    } else {
                        Toast.makeText(this@EditProfileActivity, "Contraseña incorrecta", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Error de verificación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun unlockField() {
        when (fieldWaitingToUnlock) {
            "name" -> {
                etName.isEnabled = true
                etName.requestFocus()
                showKeyboard(etName)
                btnStartEditName.visibility = View.GONE
                containerNameActions.visibility = View.VISIBLE
            }
            "email" -> {
                etEmail.isEnabled = true
                etEmail.requestFocus()
                showKeyboard(etEmail)
                btnStartEditEmail.visibility = View.GONE
                containerEmailActions.visibility = View.VISIBLE
            }
            "password" -> goToChangePasswordScreen()
            "delete_account" -> deleteAccount()
        }
        fieldWaitingToUnlock = ""
    }

    private fun lockAllFields() {
        verifiedPassword = ""
        etName.isEnabled = false
        btnStartEditName.visibility = View.VISIBLE
        containerNameActions.visibility = View.GONE
        etEmail.isEnabled = false
        btnStartEditEmail.visibility = View.VISIBLE
        containerEmailActions.visibility = View.GONE
    }

    private fun saveChanges(fieldType: String) {
        val newName = etName.text.toString().trim()
        val newEmail = etEmail.text.toString().trim()

        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(this, "Campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Se envía el ID_user correctamente en el request de actualización
                val request = UpdateProfileRequest(userId, newName, newEmail, verifiedPassword, null)
                val response = RetrofitClient.instance.create(EditProfileApi::class.java).updateProfile(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@EditProfileActivity, "Cambios guardados", Toast.LENGTH_SHORT).show()
                        originalName = newName
                        originalEmail = newEmail
                        lockAllFields()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val mensajeAMostrar = if (!errorBody.isNullOrEmpty()) {
                            try {
                                org.json.JSONObject(errorBody).getString("message")
                            } catch (e: Exception) {
                                "Error en el formato de respuesta"
                            }
                        } else {
                            "Error desconocido"
                        }
                        Toast.makeText(this@EditProfileActivity, mensajeAMostrar, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Error de conexión al guardar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Cuenta")
            .setMessage("¿Confirmas la eliminación permanente?")
            .setPositiveButton("Eliminar") { _, _ ->
                fieldWaitingToUnlock = "delete_account"
                showPasswordDialog()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteAccount() {
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
                        Toast.makeText(this@EditProfileActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Error de red", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun goToChangePasswordScreen() {
        val intent = Intent(this, ChangePasswordActivity::class.java)
        intent.putExtra("CURRENT_PASSWORD", verifiedPassword)
        intent.putExtra("CURRENT_NAME", originalName)
        intent.putExtra("CURRENT_EMAIL", originalEmail)
        startActivity(intent)
        lockAllFields()
        fetchUserData()
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
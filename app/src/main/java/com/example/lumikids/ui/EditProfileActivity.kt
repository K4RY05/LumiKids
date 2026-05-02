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

    // Referencias a los contenedores y botones de Guardar/Cancelar
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

        if (id == null) {
            Toast.makeText(this, "Error de sesión. Vuelve a ingresar.", Toast.LENGTH_SHORT).show()
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

        // ==================== LÓGICA NOMBRE ====================
        btnStartEditName.setOnClickListener {
            // Validación: No permitir editar ambos al mismo tiempo
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

        btnSaveName.setOnClickListener {
            saveChanges("name")
        }

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

        btnSaveEmail.setOnClickListener {
            saveChanges("email")
        }

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
                val api = RetrofitClient.instance.create(EditProfileApi::class.java)
                val response = api.getUserProfile(userId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val user = response.body()!!
                        etName.setText(user.name)
                        etEmail.setText(user.email)

                        originalName = user.name
                        originalEmail = user.email
                    } else {
                        Toast.makeText(this@EditProfileActivity, "Error al cargar datos del servidor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
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
                    Toast.makeText(this@EditProfileActivity, "Error de red al verificar", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Ningún campo puede quedar en blanco", Toast.LENGTH_SHORT).show()
            return
        }

        if (fieldType == "name") {
            btnSaveName.isEnabled = false
            btnCancelName.isEnabled = false
        } else {
            btnSaveEmail.isEnabled = false
            btnCancelEmail.isEnabled = false
        }

        hideKeyboard()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = UpdateProfileRequest(userId, newName, newEmail, verifiedPassword, null)
                val response = RetrofitClient.instance.create(EditProfileApi::class.java).updateProfile(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@EditProfileActivity, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                        originalName = newName
                        originalEmail = newEmail

                        lockAllFields()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = if (!errorBody.isNullOrEmpty()) {
                            try {
                                org.json.JSONObject(errorBody).optString("message", "Error al actualizar")
                            } catch (e: Exception) {
                                "Error en el formato de respuesta"
                            }
                        } else {
                            response.body()?.message ?: "Error desconocido"
                        }

                        Toast.makeText(this@EditProfileActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }

                    // Restaurar clics de los botones
                    btnSaveName.isEnabled = true
                    btnCancelName.isEnabled = true
                    btnSaveEmail.isEnabled = true
                    btnCancelEmail.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnSaveName.isEnabled = true
                    btnCancelName.isEnabled = true
                    btnSaveEmail.isEnabled = true
                    btnCancelEmail.isEnabled = true
                }
            }
        }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Cuenta")
            .setMessage("¿Estás seguro de que deseas eliminar tu cuenta permanentemente? Esta acción no se puede deshacer.")
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

    private fun goToChangePasswordScreen() {
        val intent = Intent(this, ChangePasswordActivity::class.java)
        intent.putExtra("CURRENT_PASSWORD", verifiedPassword)
        intent.putExtra("CURRENT_NAME", etName.text.toString().trim())
        intent.putExtra("CURRENT_EMAIL", etEmail.text.toString().trim())
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
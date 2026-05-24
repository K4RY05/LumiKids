package com.example.lumikids.ui

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_password)

        // IMPORTANTE: Dimensiones MATCH_PARENT/MATCH_PARENT para que el fondo translúcido funcione
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        // Referencias a las vistas dentro del diálogo con los IDs en inglés
        val etPassword = dialog.findViewById<EditText>(R.id.etDialogPassword)
        val btnVerificar = dialog.findViewById<Button>(R.id.btnVerifyDialog)
        val btnCancelar = dialog.findViewById<ImageButton>(R.id.btnCancelDialog)

        // Mostrar teclado automáticamente
        etPassword.requestFocus()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        // Acción del botón Cancelar
        btnCancelar.setOnClickListener {
            dialog.dismiss()
            fieldWaitingToUnlock = "" // Limpiamos el estado si el usuario cancela
        }

        // Acción del botón Verificar
        btnVerificar.setOnClickListener {
            val password = etPassword.text.toString().trim()
            if (password.isNotEmpty()) {
                verifyPassword(password)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Por favor, ingresa tu contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showDeleteAccountDialog() {
        // Usar el diseño personalizado y aplicar la transparencia de ventana
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirm_delete)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmDelete)
        val btnCancel = dialog.findViewById<ImageButton>(R.id.btnCancelDelete)

        // Acción del botón Cancelar (la flecha)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Acción del botón Eliminar (Confirmar)
        btnConfirm.setOnClickListener {
            dialog.dismiss() // Cerramos este diálogo de advertencia
            fieldWaitingToUnlock = "delete_account" // Marcamos que queremos borrar la cuenta
            showPasswordDialog() // Mostramos el diálogo de contraseña
        }

        dialog.show()
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
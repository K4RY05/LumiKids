package com.example.lumikids.utils
import android.content.Context

class SessionManager(context: Context) {

    // Usaremos este archivo para toda la app
    private val prefs = context.getSharedPreferences("lumikids_session", Context.MODE_PRIVATE)

    // Modificamos para recibir el userId
    fun saveLogin(email: String, userId: String) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_email", email)
            putString("user_id", userId)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    // Nueva función para recuperar el ID de forma segura y limpia
    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
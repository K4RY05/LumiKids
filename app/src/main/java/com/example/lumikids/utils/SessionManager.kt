package com.example.lumikids.utils

import android.content.Context

class SessionManager(context: Context) {

    // Archivo de preferencias para toda la app
    private val prefs = context.getSharedPreferences("lumikids_session", Context.MODE_PRIVATE)

    // Guarda los datos de inicio de sesión
    fun saveLogin(email: String, userId: String) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_email", email)
            putString("user_id", userId)
            apply()
        }
    }

    // Verifica si hay una sesión activa
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    // Recupera el ID de forma segura y limpia
    fun getUserId(): String? {
        val id = prefs.getString("user_id", null)

        if (id == "null" || id.isNullOrEmpty()) {
            return null
        }

        return id
    }

    // Limpia las preferencias al cerrar sesión
    fun logout() {
        prefs.edit().clear().apply()
    }


    fun saveUserId(userId: String) {
        prefs.edit().putString("user_id", userId).apply()
    }


    fun getUserId(): String? {
        return prefs.getString("user_id", null)
    }



}
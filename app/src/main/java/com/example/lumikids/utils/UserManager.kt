package com.example.lumikids.utils

import android.content.Context
import android.content.SharedPreferences

class UserManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveUserData(name: String, email: String) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            apply() // Lo guarda en segundo plano para no congelar la app
        }
    }

    // ==========================================
    //   OBTENER DATOS
    // ==========================================
    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, "") ?: ""
    }

    fun getUserEmail(): String {
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    // ==========================================
    //   BORRAR DATOS (Al cerrar sesión)
    // ==========================================
    fun clearUserData() {
        prefs.edit().clear().apply()
    }

    // ==========================================
    //   CONSTANTES (Evita errores de dedo)
    // ==========================================
    companion object {
        private const val PREF_NAME = "user_profile_prefs"
        private const val KEY_USER_NAME = "KEY_USER_NAME"
        private const val KEY_USER_EMAIL = "KEY_USER_EMAIL"
    }
}
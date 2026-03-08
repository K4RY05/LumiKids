package com.example.lumikids.utils

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("lumikids_session", Context.MODE_PRIVATE)

    fun saveLogin(email: String) {
        prefs.edit().apply {
            putBoolean("is_logged_in", true)
            putString("user_email", email)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
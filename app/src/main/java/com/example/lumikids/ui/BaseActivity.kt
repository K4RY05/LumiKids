package com.example.lumikids.ui


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.utils.SessionManager

open class BaseActivity : AppCompatActivity() {

    private lateinit var baseSessionManager: SessionManager
    private var appliedTheme: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        baseSessionManager = SessionManager(this)
        appliedTheme = baseSessionManager.getTheme()

        when (appliedTheme) {
            "green" -> setTheme(R.style.Theme_LumiKids_Green)
            "red" -> setTheme(R.style.Theme_LumiKids_Red)
            "pink" -> setTheme(R.style.Theme_LumiKids_Pink)
            else -> setTheme(R.style.Theme_LumiKids_Blue)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val currentTheme = baseSessionManager.getTheme()
        if (appliedTheme != currentTheme) {
            recreate()
        }
    }
}
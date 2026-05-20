package com.example.lumikids.ui

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.MainActivity
import com.example.lumikids.R
import com.example.lumikids.utils.SessionManager
import java.util.Calendar

open class BaseActivity : AppCompatActivity() {

    private lateinit var baseSessionManager: SessionManager
    private var appliedTheme: String = ""

     open fun requiredOrientation(): Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    override fun onCreate(savedInstanceState: Bundle?) {
        enforceOrientation()
        baseSessionManager = SessionManager(this)
        appliedTheme = baseSessionManager.getTheme()
        applyUserTheme(appliedTheme)
        super.onCreate(savedInstanceState)
        setFullScreen()
    }

    override fun onResume() {
        super.onResume()
        enforceOrientation()

        val currentTheme = baseSessionManager.getTheme()
        if (appliedTheme != currentTheme) {
            appliedTheme = currentTheme
            recreate()
            return
        }

        if (this is LockActivity || this is ParentalControlActivity ||
            this is MainActivity  || this is SplashActivity) return

        if (isBlocked()) {
            startActivity(Intent(this, LockActivity::class.java))
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) setFullScreen()
    }

    private fun enforceOrientation() {
        val required = requiredOrientation()
        if (required != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED &&
            requestedOrientation != required) {
            requestedOrientation = required
        }
    }

    private fun applyUserTheme(theme: String) {
        when (theme) {
            "green" -> setTheme(R.style.Theme_LumiKids_Green)
            "red"   -> setTheme(R.style.Theme_LumiKids_Red)
            "pink"  -> setTheme(R.style.Theme_LumiKids_Pink)
            else    -> setTheme(R.style.Theme_LumiKids_Blue)
        }
    }

    private fun setFullScreen() {
        supportActionBar?.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(
                    android.view.WindowInsets.Type.statusBars() or
                            android.view.WindowInsets.Type.navigationBars()
                )
                it.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }

    private fun isBlocked(): Boolean {
        val prefs = getSharedPreferences("control_parental", MODE_PRIVATE)
        if (!prefs.contains("start_hour") || !prefs.contains("end_hour")) return false

        val startH = prefs.getInt("start_hour", 8)
        val startM = prefs.getInt("start_minute", 0)
        val endH   = prefs.getInt("end_hour", 20)
        val endM   = prefs.getInt("end_minute", 0)

        val now     = Calendar.getInstance()
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start   = startH * 60 + startM
        val end     = endH   * 60 + endM

        if (start == end) return false

        val isWithinAllowedTime = if (start < end) {
            current in start until end
        } else {
            current >= start || current < end
        }

        return !isWithinAllowedTime
    }
}
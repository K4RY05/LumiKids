package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.MainActivity
import com.example.lumikids.R
import com.example.lumikids.utils.SessionManager
import java.util.Calendar

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var baseSessionManager: SessionManager
    private var appliedTheme: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        baseSessionManager = SessionManager(this)
        appliedTheme = baseSessionManager.getTheme()

        when (appliedTheme) {
            "green" -> setTheme(R.style.Theme_LumiKids_Green)
            "red"   -> setTheme(R.style.Theme_LumiKids_Red)
            "pink"  -> setTheme(R.style.Theme_LumiKids_Pink)
            else    -> setTheme(R.style.Theme_LumiKids_Blue)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        val currentTheme = baseSessionManager.getTheme()
        if (appliedTheme != currentTheme) {
            recreate()
            return
        }

        // Evitar bucles: LockActivity y ParentalControlActivity nunca se bloquean
        if (this is LockActivity || this is ParentalControlActivity || this is MainActivity || this is SplashActivity) return

        if (isBlocked()) {
            startActivity(Intent(this, LockActivity::class.java))
            finish()
        }
    }

    /**
     * Devuelve TRUE si la app debe estar bloqueada en este momento.
     *
     * Lógica:
     *   - Si no hay configuración guardada → NO bloquear.
     *   - Horario normal  (start < end, ej. 08:00–20:00):
     *       Permitido si current está DENTRO del rango → bloquear si está FUERA.
     *   - Horario nocturno (start > end, ej. 22:00–08:00):
     *       Permitido si current >= start O current < end → bloquear si está FUERA.
     */
    private fun isBlocked(): Boolean {
        val prefs = getSharedPreferences("control_parental", MODE_PRIVATE)

        // Sin configuración → no bloquear
        if (!prefs.contains("start_hour") || !prefs.contains("end_hour")) return false

        val startH = prefs.getInt("start_hour", 8)
        val startM = prefs.getInt("start_minute", 0)
        val endH   = prefs.getInt("end_hour", 20)
        val endM   = prefs.getInt("end_minute", 0)

        val now     = Calendar.getInstance()
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start   = startH * 60 + startM
        val end     = endH   * 60 + endM

        // Misma hora de inicio y fin → sin restricción
        if (start == end) return false

        val isWithinAllowedTime = if (start < end) {
            // Horario normal: ej. 08:00 → 20:00
            // Permitido si current está entre start y end
            current in start until end
        } else {
            // Horario nocturno: ej. 22:00 → 08:00 (cruza medianoche)
            // Permitido si current >= start (noche) O current < end (madrugada)
            current >= start || current < end
        }

        return !isWithinAllowedTime  // bloquear = estar FUERA del horario permitido
    }
}
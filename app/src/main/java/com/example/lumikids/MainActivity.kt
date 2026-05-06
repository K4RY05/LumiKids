package com.example.lumikids

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lumikids.ui.BaseActivity
import com.example.lumikids.ui.ParentalControlActivity
import com.example.lumikids.ui.PecsBoardActivity
import com.example.lumikids.ui.ProfileActivity
import com.example.lumikids.ui.ThemeGameActivity
import java.util.Calendar

class MainActivity : BaseActivity() {

    private var lockDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupImmersiveMode()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        val btnTablero    = findViewById<ImageView>(R.id.btnTablero)
        val btnMinijuegos = findViewById<ImageView>(R.id.btnMinijuegos)
        val ivProfile     = findViewById<ImageView>(R.id.ivProfile)

        ivProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        btnTablero.setOnClickListener {
            Toast.makeText(this, "Abriendo tablero", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, PecsBoardActivity::class.java))
        }
        btnMinijuegos.setOnClickListener {
            Toast.makeText(this, "Presionaste Minijuegos", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ThemeGameActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (isFinishing) return
        checkLockAndShowDialog()
    }

    override fun onPause() {
        super.onPause()
        lockDialog?.dismiss()
        lockDialog = null
    }

    private fun checkLockAndShowDialog() {
        val prefs = getSharedPreferences("control_parental", MODE_PRIVATE)
        if (!prefs.contains("start_hour") || !prefs.contains("end_hour")) return

        val startH = prefs.getInt("start_hour", 8)
        val startM = prefs.getInt("start_minute", 0)
        val endH   = prefs.getInt("end_hour", 20)
        val endM   = prefs.getInt("end_minute", 0)

        val now     = Calendar.getInstance()
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start   = startH * 60 + startM
        val end     = endH   * 60 + endM

        if (start == end) return

        val isWithinAllowedTime = if (start < end) {
            current in start until end
        } else {
            current >= start || current < end
        }

        if (!isWithinAllowedTime && lockDialog?.isShowing != true) {
            showLockDialog(startH, startM)
        }
    }

    private fun showLockDialog(unlockHour: Int, unlockMinute: Int) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.activity_lock)

        // Fondo transparente para que se vea el overlay oscuro del XML
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.setCancelable(false)

        // Mostrar la hora de desbloqueo
        dialog.findViewById<TextView>(R.id.txtTimeValue).text = formatTime(unlockHour, unlockMinute)

        // Botón ACEPTAR → cierra la app
        dialog.findViewById<AppCompatButton>(R.id.btnAccept).setOnClickListener {
            dialog.dismiss()
            finishAffinity()
        }

        // Botón CONTROL PARENTAL → abre la pantalla de configuración
        dialog.findViewById<AppCompatButton>(R.id.btnParentalControl).setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ParentalControlActivity::class.java))
        }

        dialog.show()
        lockDialog = dialog
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else      -> hour
        }
        return String.format("%02d:%02d %s", h12, minute, period)
    }

    private fun setupImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}
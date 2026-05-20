package com.example.lumikids.ui

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R

class LockActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(R.layout.activity_lock)

        // Bloquear botón atrás
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { }
        })

        // Mostrar hora de desbloqueo
        val prefs = getSharedPreferences("control_parental", MODE_PRIVATE)
        val endH = prefs.getInt("start_hour", 8)
        val endM = prefs.getInt("start_minute", 0)
        findViewById<android.widget.TextView>(R.id.txtTimeValue).text =
            String.format("%02d:%02d", endH, endM)

        // Botón Control Parental
        findViewById<android.widget.Button>(R.id.btnParentalControl).setOnClickListener {
            startActivity(
                android.content.Intent(this, ParentalControlActivity::class.java)
            )
        }

        // Botón Aceptar — solo cierra el diálogo visual (no desbloquea)
        findViewById<android.widget.Button>(R.id.btnAccept).setOnClickListener {
            finishAffinity()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
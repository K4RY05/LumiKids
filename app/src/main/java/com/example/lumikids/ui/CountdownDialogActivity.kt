package com.example.lumikids.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R

class CountdownDialogActivity : BaseActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvCountdown: TextView
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawableResource(android.R.color.transparent)
        // Cancelar notificación al abrir
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(995)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON   or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.dialog_lock_countdown)

        progressBar = findViewById(R.id.progressCountdown)
        tvCountdown  = findViewById(R.id.tvCountdownText)

        startCountdown()
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(10_000L, 1_000L) {

            override fun onTick(millisUntilFinished: Long) {
                val secsLeft = (millisUntilFinished / 1000).toInt() + 1
                tvCountdown.text = secsLeft.toString()
                progressBar.progress = secsLeft * 10
            }

            override fun onFinish() {
                tvCountdown.text = "0"
                progressBar.progress = 0
                goToLock()
            }

        }.start()
    }

    private fun goToLock() {
        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Bloqueado durante la cuenta regresiva
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
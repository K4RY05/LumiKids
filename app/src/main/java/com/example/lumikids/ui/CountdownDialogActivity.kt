package com.example.lumikids.ui

import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import kotlinx.coroutines.*

class CountdownDialogActivity : BaseActivity() {

    private lateinit var progressCountdown: ProgressBar
    private lateinit var tvCountdownText: TextView
    private var countdownJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setDimAmount(0.6f)
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_lock_countdown)
        dialog.setCancelable(false)

        progressCountdown = dialog.findViewById(R.id.progressCountdown)
        tvCountdownText   = dialog.findViewById(R.id.tvCountdownText)

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setGravity(android.view.Gravity.CENTER)
        }

        dialog.show()
        startCountdown(dialog)
    }

    private fun startCountdown(dialog: Dialog) {
        val totalSeconds = 10
        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            for (secondsLeft in totalSeconds downTo 0) {
                tvCountdownText.text = secondsLeft.toString()
                progressCountdown.progress = (secondsLeft * 100) / totalSeconds

                if (secondsLeft == 0) {
                    dialog.dismiss()
                    launchLock()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun launchLock() {
        startActivity(
            Intent(this, LockActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownJob?.cancel()
    }
}
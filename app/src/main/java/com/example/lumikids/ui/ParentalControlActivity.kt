package com.example.lumikids.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.lumikids.R
import com.example.lumikids.utils.NotificationReceiver
import androidx.appcompat.widget.AppCompatButton
import java.util.*

class ParentalControlActivity : BaseActivity() {

    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView

    private var startHour = 8
    private var startMinute = 0
    private var endHour = 20
    private var endMinute = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parental_control)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnSave = findViewById<AppCompatButton>(R.id.btnSave)
        val containerStart = findViewById<ConstraintLayout>(R.id.containerStartTime)
        val containerEnd = findViewById<ConstraintLayout>(R.id.containerEndTime)
        tvStartTime = findViewById(R.id.bntStartTime)
        tvEndTime = findViewById(R.id.bntEndTime)

        loadSavedTimes()

        btnBack.setOnClickListener { finish() }
        containerStart.setOnClickListener { showTimePicker(isStart = true) }
        containerEnd.setOnClickListener { showTimePicker(isStart = false) }

        btnSave.setOnClickListener {
            saveTimes()
            try {
                scheduleNotifications()
            } catch (e: SecurityException) {
                scheduleNotificationsFallback()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Toast.makeText(this, "Horario guardado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimePicker(isStart: Boolean) {
        val hour = if (isStart) startHour else endHour
        val minute = if (isStart) startMinute else endMinute

        TimePickerDialog(
            this,
            { _, h, m ->
                if (isStart) {
                    startHour = h; startMinute = m
                    tvStartTime.text = formatTime(h, m)
                } else {
                    endHour = h; endMinute = m
                    tvEndTime.text = formatTime(h, m)
                }
            },
            hour, minute, false
        ).show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val h12 = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        return String.format("%02d:%02d %s", h12, minute, period)
    }

    private fun saveTimes() {
        getSharedPreferences("control_parental", MODE_PRIVATE).edit()
            .putInt("start_hour", startHour).putInt("start_minute", startMinute)
            .putInt("end_hour", endHour).putInt("end_minute", endMinute)
            .apply()
    }

    private fun loadSavedTimes() {
        val prefs = getSharedPreferences("control_parental", MODE_PRIVATE)
        startHour = prefs.getInt("start_hour", 8); startMinute = prefs.getInt("start_minute", 0)
        endHour = prefs.getInt("end_hour", 20); endMinute = prefs.getInt("end_minute", 0)
        tvStartTime.text = formatTime(startHour, startMinute)
        tvEndTime.text = formatTime(endHour, endMinute)
    }

    /**
     * Calcula millis para la hora dada (hoy o mañana si ya pasó),
     * restando el offset de anticipación.
     */
    private fun getTargetTimeMillis(hour: Int, minute: Int, offsetMillis: Long = 0): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis - offsetMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis - offsetMillis
    }

    private fun scheduleNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            scheduleNotificationsFallback(); return
        }
        setAlarm(alarmManager, getTargetTimeMillis(endHour, endMinute, 10 * 60 * 1000L), "Faltan 10 minutos para el bloqueo")
        setAlarm(alarmManager, getTargetTimeMillis(endHour, endMinute, 5 * 60 * 1000L),  "Faltan 5 minutos para el bloqueo")
    }

    private fun scheduleNotificationsFallback() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(
            getTargetTimeMillis(endHour, endMinute, 10 * 60 * 1000L) to "Faltan 10 minutos para el bloqueo",
            getTargetTimeMillis(endHour, endMinute,  5 * 60 * 1000L) to "Faltan 5 minutos para el bloqueo"
        ).forEach { (time, msg) ->
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, buildPendingIntent(msg))
        }
    }

    private fun setAlarm(alarmManager: AlarmManager, time: Long, message: String) {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, buildPendingIntent(message))
    }

    private fun buildPendingIntent(message: String): PendingIntent {
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("mensaje", message)
            putExtra("control_parental", true)
        }
        return PendingIntent.getBroadcast(
            this, message.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
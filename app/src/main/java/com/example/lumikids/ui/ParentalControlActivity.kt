package com.example.lumikids.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.lumikids.R
import com.example.lumikids.utils.NotificationReceiver
import java.util.*

class ParentalControlActivity : BaseActivity() {

    private lateinit var tvStartTime: TextView
    private lateinit var tvEndTime: TextView

    private var startHour   = 15
    private var startMinute = 0
    private var endHour     = 16
    private var endMinute   = 0

    private var captchaCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parental_control)

        val btnBack        = findViewById<ImageButton>(R.id.btnBack)
        val btnSave        = findViewById<AppCompatButton>(R.id.btnSave)
        val containerStart = findViewById<ConstraintLayout>(R.id.containerStartTime)
        val containerEnd   = findViewById<ConstraintLayout>(R.id.containerEndTime)
        tvStartTime        = findViewById(R.id.bntStartTime)
        tvEndTime          = findViewById(R.id.bntEndTime)

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

        showCaptchaDialog()
    }

    // ─────────────────────────────────────────────
    // CAPTCHA
    // ─────────────────────────────────────────────

    private fun showCaptchaDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_control)
        dialog.setCancelable(false)

        val btnCancelar = dialog.findViewById<ImageButton>(R.id.btnCancelar)
        val btnAceptar  = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAceptar)
        val btnRefresh  = dialog.findViewById<AppCompatImageButton>(R.id.btnRefreshCaptcha)
        val edtCaptcha  = dialog.findViewById<EditText>(R.id.edtCaptchaInput)
        val imgCaptcha  = dialog.findViewById<ImageView>(R.id.imgCaptcha)

        captchaCode = generateCaptchaCode()
        imgCaptcha.setImageBitmap(generateCaptchaBitmap(captchaCode))

        btnRefresh.setOnClickListener {
            captchaCode = generateCaptchaCode()
            imgCaptcha.setImageBitmap(generateCaptchaBitmap(captchaCode))
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        btnAceptar.setOnClickListener {
            val userInput = edtCaptcha.text.toString().trim()
            if (userInput.equals(captchaCode, ignoreCase = true)) {
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Captcha incorrecto", Toast.LENGTH_SHORT).show()
                captchaCode = generateCaptchaCode()
                imgCaptcha.setImageBitmap(generateCaptchaBitmap(captchaCode))
                edtCaptcha.text.clear()
            }
        }

        dialog.show()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun generateCaptchaCode(length: Int = 6): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789@#&"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun generateCaptchaBitmap(text: String): Bitmap {
        val width  = 420
        val height = 130
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val random = java.util.Random()

        canvas.drawColor(Color.rgb(220, 220, 220))

        val noisePaint = Paint().apply { strokeWidth = 1.5f; isAntiAlias = true }
        repeat(12) {
            noisePaint.color = Color.rgb(random.nextInt(180), random.nextInt(180), random.nextInt(180))
            canvas.drawLine(
                random.nextFloat() * width, random.nextFloat() * height,
                random.nextFloat() * width, random.nextFloat() * height,
                noisePaint
            )
        }

        val dotPaint = Paint().apply { strokeWidth = 3f }
        repeat(80) {
            dotPaint.color = Color.rgb(random.nextInt(200), random.nextInt(200), random.nextInt(200))
            canvas.drawPoint(random.nextFloat() * width, random.nextFloat() * height, dotPaint)
        }

        val letterPaint = Paint().apply {
            textSize       = 58f
            isFakeBoldText = true
            isAntiAlias    = true
            typeface       = android.graphics.Typeface.MONOSPACE
        }

        val letterWidth = width.toFloat() / text.length
        text.forEachIndexed { i, char ->
            letterPaint.color = Color.rgb(random.nextInt(100), random.nextInt(100), random.nextInt(100))
            canvas.save()
            val x = letterWidth * i + letterWidth / 2
            val y = height / 2f + 20f + random.nextInt(20) - 10
            canvas.rotate((random.nextFloat() * 40f) - 20f, x, y)
            canvas.drawText(char.toString(), x - 18f, y, letterPaint)
            canvas.restore()
        }

        val overPaint = Paint().apply { strokeWidth = 2f; isAntiAlias = true }
        repeat(4) {
            overPaint.color = Color.rgb(random.nextInt(150), random.nextInt(150), random.nextInt(150))
            canvas.drawLine(
                random.nextFloat() * width, random.nextFloat() * height,
                random.nextFloat() * width, random.nextFloat() * height,
                overPaint
            )
        }

        return bitmap
    }

    // ─────────────────────────────────────────────
    // TIME PICKER
    // ─────────────────────────────────────────────

    private fun showTimePicker(isStart: Boolean) {
        val hour   = if (isStart) startHour else endHour
        val minute = if (isStart) startMinute else endMinute

        TimePickerDialog(this, { _, h, m ->
            if (isStart) {
                startHour = h; startMinute = m
                tvStartTime.text = formatTime(h, m)
            } else {
                endHour = h; endMinute = m
                tvEndTime.text = formatTime(h, m)
            }
        }, hour, minute, false).show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val h12    = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        return String.format("%02d:%02d %s", h12, minute, period)
    }

    // ─────────────────────────────────────────────
    // GUARDAR / CARGAR
    // ─────────────────────────────────────────────

    private fun saveTimes() {
        getSharedPreferences("control_parental", MODE_PRIVATE).edit()
            .putInt("start_hour",   startHour)
            .putInt("start_minute", startMinute)
            .putInt("end_hour",     endHour)
            .putInt("end_minute",   endMinute)
            .apply()
    }

    private fun loadSavedTimes() {
        val prefs   = getSharedPreferences("control_parental", MODE_PRIVATE)
        startHour   = prefs.getInt("start_hour",   8)
        startMinute = prefs.getInt("start_minute", 0)
        endHour     = prefs.getInt("end_hour",     20)
        endMinute   = prefs.getInt("end_minute",   0)
        tvStartTime.text = formatTime(startHour, startMinute)
        tvEndTime.text   = formatTime(endHour, endMinute)
    }

    // ─────────────────────────────────────────────
    // ALARMAS
    // ─────────────────────────────────────────────

    private fun getTargetTimeMillis(hour: Int, minute: Int, offsetMillis: Long = 0): Long {
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // ✅ Verifica si la hora BASE ya pasó (sin offset)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
        // ✅ Resta el offset DESPUÉS de tener el tiempo base correcto
        return baseTime - offsetMillis
    }

    private fun scheduleNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            scheduleNotificationsFallback()
            return
        }

        // Notificaciones 10 y 5 minutos antes
        setAlarm(alarmManager, getTargetTimeMillis(endHour, endMinute, 10 * 60 * 1000L), "Faltan 10 minutos para el bloqueo")
        setAlarm(alarmManager, getTargetTimeMillis(endHour, endMinute,  5 * 60 * 1000L), "Faltan 5 minutos para el bloqueo")


        setAlarmCountdown(alarmManager, getTargetTimeMillis(endHour, endMinute, 10 * 1000L))
    }

    private fun scheduleNotificationsFallback() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        listOf(
            getTargetTimeMillis(endHour, endMinute, 10 * 60 * 1000L) to "Faltan 10 minutos para el bloqueo",
            getTargetTimeMillis(endHour, endMinute,  5 * 60 * 1000L) to "Faltan 5 minutos para el bloqueo"
        ).forEach { (time, msg) ->
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, buildPendingIntent(msg))
        }


        setAlarmCountdown(alarmManager, getTargetTimeMillis(endHour, endMinute, 10 * 1000L))
    }

    private fun setAlarm(alarmManager: AlarmManager, time: Long, message: String) {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, buildPendingIntent(message))
    }

    private fun setAlarmCountdown(alarmManager: AlarmManager, time: Long) {
        val intent = Intent(this, CountdownDialogActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 998, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
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
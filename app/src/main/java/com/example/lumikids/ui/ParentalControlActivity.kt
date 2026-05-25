package com.example.lumikids.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
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

    private var captchaCode      = ""
    private var intentosFallidos = 0
    private val MAX_INTENTOS     = 5
    private val BLOQUEO_MS       = 5 * 60 * 1000L

    // ─────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────

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

        window.decorView.post {
            Log.d("CAPTCHA", "decorView.post ejecutado — activity lista")
            if (estaBloqueado()) {
                mostrarDialogoBloqueo(tiempoBloqueoRestante())
            } else {
                intentosFallidos = 0
                showCaptchaDialog()
            }
        }
    }

    // ─────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun formatCountdown(ms: Long): String {
        val minutos  = ms / 1000 / 60
        val segundos = (ms / 1000) % 60
        return if (minutos > 0) "%02d:%02d".format(minutos, segundos)
        else "%02d".format(segundos)
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val h12    = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        return String.format("%02d:%02d %s", h12, minute, period)
    }

    // ─────────────────────────────────────────────
    // BLOQUEO POR INTENTOS
    // ─────────────────────────────────────────────

    private fun estaBloqueado(): Boolean {
        val prefs = getSharedPreferences("captcha_bloqueo", MODE_PRIVATE)
        return System.currentTimeMillis() < prefs.getLong("bloqueo_hasta", 0L)
    }

    private fun tiempoBloqueoRestante(): Long {
        val prefs = getSharedPreferences("captcha_bloqueo", MODE_PRIVATE)
        return prefs.getLong("bloqueo_hasta", 0L) - System.currentTimeMillis()
    }

    private fun activarBloqueo() {
        getSharedPreferences("captcha_bloqueo", MODE_PRIVATE).edit()
            .putLong("bloqueo_hasta", System.currentTimeMillis() + BLOQUEO_MS)
            .apply()
    }

    private fun mostrarDialogoBloqueo(tiempoRestanteMs: Long) {
        if (isFinishing || isDestroyed) return

        // Resolver color_primario
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(R.attr.color_primario, typedValue, true)
        val colorPrimario = typedValue.data

        // Resolver color_fondo_pantalla
        val typedValueFondo = android.util.TypedValue()
        theme.resolveAttribute(R.attr.color_fondo_pantalla, typedValueFondo, true)
        val colorFondo = typedValueFondo.data

        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setCancelable(false)

        // ── ROOT ──────────────────────────────────────────────────────────────
        val root = ConstraintLayout(this).apply {
            id = View.generateViewId()
            setBackgroundColor(Color.parseColor("#80000000"))
            isClickable = true
            isFocusable = true
        }

        // ── dialogContainer ───────────────────────────────────────────────────
        val dialogContainer = ConstraintLayout(this).apply {
            id = View.generateViewId()
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
            background = ContextCompat.getDrawable(
                this@ParentalControlActivity,
                R.drawable.bg_pause_container
            )
        }

        // ── btnCancelar ───────────────────────────────────────────────────────
        val btnCancelar = ImageButton(this).apply {
            id = View.generateViewId()
            setImageDrawable(
                ContextCompat.getDrawable(this@ParentalControlActivity, R.drawable.ic_left)
            )
            setBackgroundColor(colorFondo)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = "Cancelar y salir"
            setOnClickListener { dialog.dismiss(); finish() }
        }

        // ── tvTitle ───────────────────────────────────────────────────────────
        val tvTitle = TextView(this).apply {
            id = View.generateViewId()
            text = "Acceso Bloqueado"
            setTextColor(colorPrimario)
            textSize = resources.getDimension(R.dimen.pause_title_size) /
                    resources.displayMetrics.scaledDensity
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        // ── clockContainer ────────────────────────────────────────────────────
        val clockContainer = FrameLayout(this).apply {
            id = View.generateViewId()
        }

        val progressBar = ProgressBar(
            this, null, android.R.attr.progressBarStyleHorizontal
        ).apply {
            id = View.generateViewId()
            isIndeterminate  = false
            max              = 100
            progress         = 100
            progressDrawable = ContextCompat.getDrawable(
                this@ParentalControlActivity,
                R.drawable.circular_countdown_bar
            )
            rotation = -90f
        }

        val tvCountdown = TextView(this).apply {
            id = View.generateViewId()
            text = formatCountdown(tiempoRestanteMs)
            setTextColor(colorPrimario)
            textSize = 32f
            setTypeface(typeface, Typeface.BOLD)
        }

        // ── tvMessage ─────────────────────────────────────────────────────────
        val tvMessage = TextView(this).apply {
            id = View.generateViewId()
            text = "Demasiados intentos fallidos."
            setTextColor(Color.DKGRAY)
            textSize = 14f
            gravity = Gravity.CENTER
        }

        // ── Ensamblar clockContainer ──────────────────────────────────────────
        val clockSizePx = dpToPx(140)
        clockContainer.addView(
            progressBar,
            FrameLayout.LayoutParams(clockSizePx, clockSizePx)
        )
        clockContainer.addView(
            tvCountdown,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        )

        // ── Agregar hijos a dialogContainer ──────────────────────────────────
        dialogContainer.addView(btnCancelar)
        dialogContainer.addView(tvTitle)
        dialogContainer.addView(clockContainer)
        dialogContainer.addView(tvMessage)

        // ── Constraints de dialogContainer ───────────────────────────────────
        val btnSizePx   = dpToPx(60)
        val marginStart = dpToPx(16)

        ConstraintSet().apply {
            clone(dialogContainer)

            // btnCancelar: alineado vertical con tvTitle, al inicio
            connect(btnCancelar.id, ConstraintSet.TOP,    tvTitle.id,              ConstraintSet.TOP)
            connect(btnCancelar.id, ConstraintSet.BOTTOM, tvTitle.id,              ConstraintSet.BOTTOM)
            connect(btnCancelar.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(btnCancelar.id, ConstraintSet.END,    tvTitle.id,              ConstraintSet.START)
            constrainWidth(btnCancelar.id,  btnSizePx)
            constrainHeight(btnCancelar.id, btnSizePx)

            // tvTitle: top del parent, a la derecha del botón
            connect(tvTitle.id, ConstraintSet.TOP,   ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(tvTitle.id, ConstraintSet.START, btnCancelar.id,          ConstraintSet.END, marginStart)
            connect(tvTitle.id, ConstraintSet.END,   ConstraintSet.PARENT_ID, ConstraintSet.END)

            // chain horizontal packed
            setHorizontalChainStyle(btnCancelar.id, ConstraintSet.CHAIN_PACKED)

            // clockContainer: debajo del título, centrado
            connect(clockContainer.id, ConstraintSet.TOP,   tvTitle.id,              ConstraintSet.BOTTOM, dpToPx(24))
            connect(clockContainer.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(clockContainer.id, ConstraintSet.END,   ConstraintSet.PARENT_ID, ConstraintSet.END)
            constrainWidth(clockContainer.id,  clockSizePx)
            constrainHeight(clockContainer.id, clockSizePx)

            // tvMessage: debajo del reloj, ancho 0 (match constraints)
            connect(tvMessage.id, ConstraintSet.TOP,    clockContainer.id,       ConstraintSet.BOTTOM, dpToPx(16))
            connect(tvMessage.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(tvMessage.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
            connect(tvMessage.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constrainWidth(tvMessage.id, 0)

            applyTo(dialogContainer)
        }

        // ── Agregar dialogContainer al root ──────────────────────────────────
        root.addView(dialogContainer)

        ConstraintSet().apply {
            clone(root)
            connect(dialogContainer.id, ConstraintSet.TOP,    ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            connect(dialogContainer.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            connect(dialogContainer.id, ConstraintSet.START,  ConstraintSet.PARENT_ID, ConstraintSet.START)
            connect(dialogContainer.id, ConstraintSet.END,    ConstraintSet.PARENT_ID, ConstraintSet.END)
            constrainPercentWidth(dialogContainer.id, 0.75f)
            constrainWidth(dialogContainer.id, 0)
            applyTo(root)
        }

        dialog.setContentView(root)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setDimAmount(0f)
        }

        dialog.show()

        // ── CountDownTimer ────────────────────────────────────────────────────
        val totalMs = tiempoRestanteMs.toFloat()

        object : CountDownTimer(tiempoRestanteMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                tvCountdown.text    = formatCountdown(millisUntilFinished)
                progressBar.progress = ((millisUntilFinished / totalMs) * 100).toInt()
            }

            override fun onFinish() {
                tvCountdown.text     = "¡Listo!"
                progressBar.progress = 0
                dialog.dismiss()
                intentosFallidos = 0
                showCaptchaDialog()
            }
        }.start()
    }

    // ─────────────────────────────────────────────
    // CAPTCHA
    // ─────────────────────────────────────────────

    private fun showCaptchaDialog() {
        Log.d("CAPTCHA", "showCaptchaDialog() iniciando...")

        if (isFinishing || isDestroyed) {
            Log.e("CAPTCHA", "Activity está finishing o destroyed — abortando")
            return
        }

        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        try {
            dialog.setContentView(R.layout.dialog_control)
        } catch (e: Exception) {
            Log.e("CAPTCHA", "Error en setContentView: ${e.message}", e)
            return
        }

        dialog.setCancelable(false)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val btnCancelar = dialog.findViewById<ImageButton>(R.id.btnCancelar)
        val btnAceptar  = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAceptar)
        val btnRefresh  = dialog.findViewById<AppCompatImageButton>(R.id.btnRefreshCaptcha)
        val edtCaptcha  = dialog.findViewById<EditText>(R.id.edtCaptchaInput)
        val imgCaptcha  = dialog.findViewById<ImageView>(R.id.imgCaptcha)

        if (imgCaptcha == null) {
            Log.e("CAPTCHA", "imgCaptcha es NULL")
            return
        }

        captchaCode = generateCaptchaCode()
        imgCaptcha.setImageBitmap(generateCaptchaBitmap(captchaCode))

        btnRefresh?.setOnClickListener {
            captchaCode = generateCaptchaCode()
            imgCaptcha.setImageBitmap(generateCaptchaBitmap(captchaCode))
        }

        btnCancelar?.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        btnAceptar?.setOnClickListener {
            val userInput = edtCaptcha?.text?.toString()?.trim() ?: ""
            if (userInput.equals(captchaCode, ignoreCase = true)) {
                intentosFallidos = 0
                dialog.dismiss()
            } else {
                intentosFallidos++
                val restantes = MAX_INTENTOS - intentosFallidos

                if (intentosFallidos >= MAX_INTENTOS) {
                    activarBloqueo()
                    dialog.dismiss()
                    Toast.makeText(
                        this,
                        "Demasiados intentos. Bloqueado por 5 minutos.",
                        Toast.LENGTH_LONG
                    ).show()
                    mostrarDialogoBloqueo(BLOQUEO_MS)
                } else {
                    Toast.makeText(
                        this,
                        "Captcha incorrecto. Te quedan $restantes intento(s).",
                        Toast.LENGTH_SHORT
                    ).show()
                    captchaCode = generateCaptchaCode()
                    imgCaptcha.setImageBitmap(generateCaptchaBitmap(captchaCode))
                    edtCaptcha?.text?.clear()
                }
            }
        }

        try {
            dialog.show()
        } catch (e: Exception) {
            Log.e("CAPTCHA", "Error al llamar dialog.show(): ${e.message}", e)
            return
        }

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
            noisePaint.color = Color.rgb(
                random.nextInt(180), random.nextInt(180), random.nextInt(180)
            )
            canvas.drawLine(
                random.nextFloat() * width, random.nextFloat() * height,
                random.nextFloat() * width, random.nextFloat() * height,
                noisePaint
            )
        }

        val dotPaint = Paint().apply { strokeWidth = 3f }
        repeat(80) {
            dotPaint.color = Color.rgb(
                random.nextInt(200), random.nextInt(200), random.nextInt(200)
            )
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
            letterPaint.color = Color.rgb(
                random.nextInt(100), random.nextInt(100), random.nextInt(100)
            )
            canvas.save()
            val x = letterWidth * i + letterWidth / 2
            val y = height / 2f + 20f + random.nextInt(20) - 10
            canvas.rotate((random.nextFloat() * 40f) - 20f, x, y)
            canvas.drawText(char.toString(), x - 18f, y, letterPaint)
            canvas.restore()
        }

        val overPaint = Paint().apply { strokeWidth = 2f; isAntiAlias = true }
        repeat(4) {
            overPaint.color = Color.rgb(
                random.nextInt(150), random.nextInt(150), random.nextInt(150)
            )
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
        startHour   = prefs.getInt("start_hour",   15)
        startMinute = prefs.getInt("start_minute", 0)
        endHour     = prefs.getInt("end_hour",     16)
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
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis
        return baseTime - offsetMillis
    }

    private fun scheduleNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()) {
            scheduleNotificationsFallback()
            return
        }

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
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("control_parental", true)
            putExtra("mensaje", "La aplicación se bloqueará en 10 segundos")
            putExtra("es_cuenta_regresiva", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
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
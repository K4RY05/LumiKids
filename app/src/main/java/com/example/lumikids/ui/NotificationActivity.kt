package com.example.lumikids.ui

import android.app.*
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lumikids.R
import com.example.lumikids.model.NotificationEntity
import com.example.lumikids.network.NotificationApiService
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.NotificationReceiver
import com.example.lumikids.utils.SessionManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

class NotificationActivity : AppCompatActivity() {

    private lateinit var adapter: NotificacionAdapter
    private lateinit var sessionManager: SessionManager
    private val listaNoti = mutableListOf<NotificationEntity>()
    private var idEditando: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        sessionManager = SessionManager(this)

        solicitarPermisos()
        setupRecyclerView()
        initClickListeners()
        cargarDatos()
    }

    private fun solicitarPermisos() {

        // Permiso notificaciones (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        // Permiso exact alarm (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            }
        }
    }
    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvNotificaciones)

        adapter = NotificacionAdapter(
            listaNoti,
            { eliminarNotificacion(it) },
            { editarNotificacion(it) }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter
    }

    private fun initClickListeners() {

        findViewById<ImageButton>(R.id.btnAgregar).setOnClickListener {
            mostrarFormulario(true)
        }

        findViewById<ImageButton>(R.id.btnCancelar).setOnClickListener {

            val formularioVisible =
                findViewById<View>(R.id.layoutContenedorFormulario).visibility == View.VISIBLE

            if (formularioVisible) {
                limpiarFormulario()
            } else {
                finish()
            }
        }

        findViewById<Button>(R.id.btnAceptar).setOnClickListener {
            guardarNotificacion()
        }
        findViewById<TextView>(R.id.tvFecha).setOnClickListener {
            showDatePicker()
        }
        findViewById<TextView>(R.id.tvHora).setOnClickListener {
            showTimePicker()
        }
    }

    private fun cargarDatos() {
        val userId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val api = RetrofitClient.instance.create(NotificationApiService::class.java)
                val response = api.getNotifications(userId)
                listaNoti.clear()
                listaNoti.addAll(response)

                adapter.notifyDataSetChanged()

                findViewById<TextView>(R.id.tvSinNotificaciones).visibility =
                    if (listaNoti.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Log.e("LOAD", e.message.toString())
            }
        }
    }

    private fun guardarNotificacion() {
        val userId = sessionManager.getUserId() ?: return

        val titulo = findViewById<EditText>(R.id.etTitulo).text.toString()
        val mensaje = findViewById<EditText>(R.id.etMensaje).text.toString()
        val fecha = findViewById<TextView>(R.id.tvFecha).text.toString()
        val hora = findViewById<TextView>(R.id.tvHora).text.toString()

        if (titulo.isEmpty() || fecha.contains("dd")) {
            Toast.makeText(this, "Campos incompletos", Toast.LENGTH_SHORT).show()
            return
        }

        val noti = NotificationEntity(
            ID_notification = idEditando,
            ID_user = userId,
            title = titulo,
            message = mensaje,
            date = formatToMySQL(fecha),
            time = if (hora.length == 5) "$hora:00" else hora
        )

        lifecycleScope.launch {
            val api = RetrofitClient.instance.create(NotificationApiService::class.java)

            try {
                if (idEditando == null) {
                    api.addNotification(noti)
                } else {
                    idEditando?.let { api.updateNotification(it, noti) }
                    idEditando = null
                }

                programarAlarmaLocal(titulo, mensaje, noti.date, noti.time)

                limpiarFormulario()
                delay(500)
                cargarDatos()

            } catch (e: Exception) {
                Log.e("SAVE", e.message.toString())
            }
        }
    }

    private fun editarNotificacion(noti: NotificationEntity) {
        mostrarFormulario(true)

        findViewById<EditText>(R.id.etTitulo).setText(noti.title)
        findViewById<EditText>(R.id.etMensaje).setText(noti.message)
        val fechaLimpia = noti.date.split("T")[0]
        val f = fechaLimpia.split("-")

        val dia = f.getOrNull(2)?.trim() ?: "00"
        val mes = f.getOrNull(1)?.trim() ?: "00"
        val anio = f.getOrNull(0)?.trim() ?: "0000"

        findViewById<TextView>(R.id.tvFecha).text = "$dia/$mes/$anio"

        findViewById<TextView>(R.id.tvHora).text =
            noti.time.trim().substring(0, 5)

        idEditando = noti.ID_notification
    }

    private fun eliminarNotificacion(id: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance
                    .create(NotificationApiService::class.java)
                    .deleteNotification(id)

                if (response.isSuccessful) {
                    Toast.makeText(this@NotificationActivity, "Eliminado", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                } else {
                    Toast.makeText(this@NotificationActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Log.e("DELETE", e.message.toString())
            }
        }
    }

    private fun limpiarFormulario() {
        mostrarFormulario(false)

        findViewById<EditText>(R.id.etTitulo).text.clear()
        findViewById<EditText>(R.id.etMensaje).text.clear()
        findViewById<TextView>(R.id.tvFecha).text = "dd/MM/yyyy"
        findViewById<TextView>(R.id.tvHora).text = "hh:mm"

        idEditando = null
    }

    private fun programarAlarmaLocal(titulo: String, mensaje: String, fecha: String, hora: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("titulo", titulo)
            putExtra("mensaje", mensaje)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance()
        val f = fecha.split("-")
        val h = hora.split(":")

        calendar.set(f[0].toInt(), f[1].toInt() - 1, f[2].toInt(), h[0].toInt(), h[1].toInt(), 0)

        if (calendar.timeInMillis > System.currentTimeMillis()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            findViewById<TextView>(R.id.tvFecha).text =
                "%02d/%02d/%04d".format(d, m + 1, y)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker() {
        val c = Calendar.getInstance()
        TimePickerDialog(this, { _, h, m ->
            findViewById<TextView>(R.id.tvHora).text =
                "%02d:%02d".format(h, m)
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
    }

    private fun mostrarFormulario(visible: Boolean) {
        findViewById<View>(R.id.layoutListaPrincipal).visibility =
            if (visible) View.GONE else View.VISIBLE

        findViewById<View>(R.id.layoutContenedorFormulario).visibility =
            if (visible) View.VISIBLE else View.GONE
    }

    private fun formatToMySQL(date: String): String {
        val p = date.split("/")
        return "${p[2]}-${p[1]}-${p[0]}"
    }
}
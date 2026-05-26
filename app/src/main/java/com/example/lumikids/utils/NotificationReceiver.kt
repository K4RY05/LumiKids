package com.example.lumikids.utils

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.lumikids.R
import com.example.lumikids.ui.CountdownDialogActivity
import com.example.lumikids.ui.NotificationActivity

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
        private const val CHANNEL_ID = "lumikids_channel"
        private const val CHANNEL_ALARM_ID = "lumikids_alarm_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarma recibida en NotificationReceiver")

        val esControl = intent.getBooleanExtra("control_parental", false)
        val esCuentaRegresiva = intent.getBooleanExtra("es_cuenta_regresiva", false)
        val titulo = if (esControl) "Control parental" else intent.getStringExtra("titulo") ?: "LumiKids"
        val mensaje = intent.getStringExtra("mensaje") ?: ""

        ensureNotificationChannel(context)
        ensureAlarmChannel(context)

        if (esCuentaRegresiva) {
            handleCountdown(context, titulo, mensaje)
        } else {
            handleStandardNotification(context, intent, titulo, mensaje)
        }
    }

    private fun handleCountdown(context: Context, titulo: String, mensaje: String) {

        // 1. Lanzar la Activity directamente
        val activityIntent = Intent(context, CountdownDialogActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK        or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK      or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        context.startActivity(activityIntent)

        // 2. Notificación como respaldo
        val pendingIntent = PendingIntent.getActivity(
            context, 999, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALARM_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(995, notification)
    }

    private fun handleStandardNotification(context: Context, originalIntent: Intent, titulo: String, mensaje: String) {
        val activityIntent = Intent(context, NotificationActivity::class.java).apply {
            originalIntent.extras?.let { putExtras(it) }
        }

        val safeUniqueId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val pendingIntent = PendingIntent.getActivity(
            context,
            safeUniqueId,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(safeUniqueId, notification)
    }

    // Canal normal para notificaciones estándar
    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Notificaciones LumiKids",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    // Canal exclusivo para la alarma de bloqueo — IMPORTANCE_MAX obligatorio
    private fun ensureAlarmChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ALARM_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ALARM_ID,
                    "Alarma LumiKids",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
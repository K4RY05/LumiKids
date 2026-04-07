package com.example.lumikids.minigame.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.lumikids.R

class PauseDialog(private val context: Context) {

    // ✨ CAMBIO: Agregamos onExit para manejar el cierre de la actividad
    fun showDialog(onResume: () -> Unit, onExit: () -> Unit) {
        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.setContentView(R.layout.dialog_pause)

        // Configuración de la ventana del diálogo
        dialog.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            WindowCompat.setDecorFitsSystemWindows(window, false)
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        val btnPlay = dialog.findViewById<ImageButton>(R.id.btnPlay)
        val btnExit = dialog.findViewById<ImageButton>(R.id.btnExit) // ✨ Nuevo botón configurado
        val seekVolume = dialog.findViewById<SeekBar>(R.id.seekVolume)

        // Botón Reanudar
        btnPlay?.setOnClickListener {
            dialog.dismiss()
            onResume()
        }

        // ✨ NUEVO: Botón Salir (Regresar al menú)
        btnExit?.setOnClickListener {
            dialog.dismiss()
            onExit() // Llama a finish() en la Activity
        }

        // Control de volumen REAL del dispositivo
        val audioService = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioService.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        seekVolume?.max = maxVolume
        seekVolume?.progress = audioService.getStreamVolume(AudioManager.STREAM_MUSIC)

        seekVolume?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioService.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialog.show()
    }
}
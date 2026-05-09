package com.example.lumikids.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import androidx.core.graphics.drawable.toDrawable
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

    private val maxVolumePercentage = 0.7f

    fun showDialog(onResume: () -> Unit, onExit: () -> Unit) {
        val dialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        dialog.setContentView(R.layout.dialog_pause)

        // Configuración de la ventana del diálogo
        dialog.window?.let { window ->
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            WindowCompat.setDecorFitsSystemWindows(window, false)
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }

        val btnPlay = dialog.findViewById<ImageButton>(R.id.btnPlay)
        val btnExit = dialog.findViewById<ImageButton>(R.id.btnExit)
        val seekVolume = dialog.findViewById<SeekBar>(R.id.seekVolume)

        btnPlay?.setOnClickListener {
            dialog.dismiss()
            onResume()
        }

        btnExit?.setOnClickListener {
            dialog.dismiss()
            onExit()
        }


        val audioService = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val deviceMaxVolume = audioService.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val safeMaxVolume = (deviceMaxVolume * maxVolumePercentage).toInt()

        seekVolume?.max = safeMaxVolume

        val currentVolume = audioService.getStreamVolume(AudioManager.STREAM_MUSIC)

        if (currentVolume > safeMaxVolume) {
            audioService.setStreamVolume(AudioManager.STREAM_MUSIC, safeMaxVolume, 0) // 0 evita que salga el popup del sistema
            seekVolume?.progress = safeMaxVolume
        } else {
            seekVolume?.progress = currentVolume
        }

        // Listener para cuando el usuario desliza la barra
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
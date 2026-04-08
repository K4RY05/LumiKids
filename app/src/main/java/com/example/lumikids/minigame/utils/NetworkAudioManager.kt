package com.example.lumikids.minigame.utils

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log

class NetworkAudioManager {
    private var mediaPlayer: MediaPlayer? = null

    fun playAudioFromUrl(url: String) {
        stopAudio()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                setDataSource(url)

                prepareAsync()

                setOnPreparedListener {
                    it.start()
                }

                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }

                setOnErrorListener { _, _, _ ->
                    Log.e("NetworkAudio", "Error al intentar reproducir la URL: $url")
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Función para apagar el audio manualmente
    fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: IllegalStateException) {
            Log.e("NetworkAudio", "El MediaPlayer estaba en un estado inválido al intentar detenerlo.")
        } finally {
            mediaPlayer = null
        }
    }
}
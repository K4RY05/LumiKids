package com.example.lumikids.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameAudioManager(
    private val context: Context,
    private val scope: CoroutineScope // Recibe el lifecycleScope de la Activity
) {
    // Reproductor para audios cortos locales
    private var localMediaPlayer: MediaPlayer? = null
    // Reproductor para audios de internet
    private var networkMediaPlayer: MediaPlayer? = null
    // Variable para controlar el bucle
    private var loopJob: Job? = null

    /**
     * Reproduce un sonido local desde la carpeta res/raw (ej. R.raw.win, R.raw.fail)
     */
    fun playEffect(resId: Int) {
        MediaPlayer.create(context, resId)?.apply {
            setOnCompletionListener { release() }
            start()
        }
    }

    /**
     * Reproduce un sonido local buscando su nombre como String
     */
    fun playObjectAudio(audioIdentifier: String) {
        localMediaPlayer?.release()
        val resId = context.resources.getIdentifier(audioIdentifier, "raw", context.packageName)

        if (resId != 0) {
            localMediaPlayer = MediaPlayer.create(context, resId)
            localMediaPlayer?.start()
        }
    }

    /**
     * Reproduce un audio desde una URL de internet
     */
    fun playUrl(url: String) {
        stopNetworkAudio() // Detenemos cualquier audio anterior antes de iniciar uno nuevo

        try {
            networkMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    networkMediaPlayer = null
                }
                setOnErrorListener { _, _, _ ->
                    Log.e("GameAudioManager", "Error reproduciendo URL: $url")
                    true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Inicia un bucle que reproduce un audio de internet cada X milisegundos
     */
    fun startLoop(url: String, delayMs: Long = 8000L) {
        stopLoop() // Nos aseguramos de detener cualquier bucle anterior

        loopJob = scope.launch {
            while (isActive) {
                playUrl(url)
                delay(delayMs)
            }
        }
    }

    /**
     * Detiene temporalmente el bucle de repetición
     */
    fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    /**
     * Detiene el audio de internet que esté sonando en este momento
     */
    fun stopNetworkAudio() {
        try {
            networkMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: IllegalStateException) {
            Log.e("GameAudioManager", "Estado inválido al detener audio")
        } finally {
            networkMediaPlayer = null
        }
    }

    /**
     * Libera todos los recursos (se debe llamar en el onDestroy de la Activity)
     */
    fun releaseAll() {
        stopLoop()
        stopNetworkAudio()
        localMediaPlayer?.release()
        localMediaPlayer = null
    }
}
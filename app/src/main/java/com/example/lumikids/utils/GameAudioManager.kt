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
    private val scope: CoroutineScope
) {
    private var localMediaPlayer: MediaPlayer? = null
    // Reproductor para audios de internet
    private var networkMediaPlayer: MediaPlayer? = null
    // Variable para controlar el bucle
    private var loopJob: Job? = null


    fun playEffect(resId: Int) {
        stopLocalAudio() // Detiene cualquier efecto anterior antes de iniciar uno nuevo
        localMediaPlayer = MediaPlayer.create(context, resId)?.apply {
            setOnCompletionListener {
                it.release()
                if (localMediaPlayer == this) localMediaPlayer = null
            }
            start()
        }
    }

    // NUEVO MÉTODO: Detiene el audio local (instrucciones, efectos de raw)
    fun stopLocalAudio() {
        try {
            localMediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: IllegalStateException) {
            Log.e("GameAudioManager", "Estado inválido al detener audio local")
        } finally {
            localMediaPlayer = null
        }
    }

    fun playUrl(url: String) {
        stopNetworkAudio()
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

    // Inicia un bucle que reproduce un audio de internet cada X milisegundos
    fun startLoop(url: String, delayMs: Long = 8000L) {
        stopLoop() // Nos aseguramos de detener cualquier bucle anterior

        loopJob = scope.launch {
            while (isActive) {
                playUrl(url)
                delay(delayMs)
            }
        }
    }

    // Inicia un bucle que reproduce un audio LOCAL (res/raw) cada X milisegundos
    fun startLocalLoop(resId: Int, delayMs: Long = 8000L) {
        stopLoop() // Detenemos cualquier bucle anterior (sea de red o local)

        loopJob = scope.launch {
            while (isActive) {
                playEffect(resId)
                delay(delayMs)
            }
        }
    }

    // Detiene temporalmente el bucle de repetición
    fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
    }

    // Detiene el audio de internet que esté sonando en este momento
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

    fun releaseAll() {
        stopLoop()
        stopNetworkAudio()
        stopLocalAudio()
    }
}
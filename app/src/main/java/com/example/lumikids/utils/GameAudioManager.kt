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
    private var networkMediaPlayer: MediaPlayer? = null

    private var loopJob: Job? = null

    private var inactivityJob: Job? = null
    private var inactivityResId: Int? = null
    private var inactivityUrl: String? = null

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

    fun startLoop(url: String, delayMs: Long = 8000L) {
        stopLoop()

        loopJob = scope.launch {
            while (isActive) {
                playUrl(url)
                delay(delayMs)
            }
        }
    }

    fun startLocalLoop(resId: Int, delayMs: Long = 8000L) {
        stopLoop()

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


    fun startInactivityTimer(resId: Int, delayMs: Long = 10000L) {
        inactivityResId = resId
        inactivityUrl = null
        resetInactivityTimer(delayMs)
    }

    fun startNetworkInactivityTimer(url: String, delayMs: Long = 10000L) {
        inactivityUrl = url
        inactivityResId = null
        resetInactivityTimer(delayMs)
    }

    fun resetInactivityTimer(delayMs: Long = 10000L) {
        inactivityJob?.cancel()

        inactivityJob = scope.launch {
            while (isActive) {
                delay(delayMs)
                inactivityResId?.let { playEffect(it) }
                inactivityUrl?.let { playUrl(it) }
            }
        }
    }

    fun stopInactivityTimer() {
        inactivityJob?.cancel()
        inactivityJob = null
        inactivityResId = null
        inactivityUrl = null
    }

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
        stopInactivityTimer()
        stopNetworkAudio()
        stopLocalAudio()
    }
}
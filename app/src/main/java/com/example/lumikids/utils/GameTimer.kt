package com.example.lumikids.utils

import android.os.SystemClock

class GameTimer {
    private var startTime: Long = 0
    private var totalPausedTime: Long = 0
    private var pauseStart: Long = 0
    private var isRunning: Boolean = false

    fun start() {
        startTime = SystemClock.elapsedRealtime()
        totalPausedTime = 0
        pauseStart = 0
        isRunning = true
    }

    fun pause() {
        if (isRunning && pauseStart == 0L) {
            pauseStart = SystemClock.elapsedRealtime()
        }
    }

    fun resume() {
        if (isRunning && pauseStart != 0L) {
            totalPausedTime += (SystemClock.elapsedRealtime() - pauseStart)
            pauseStart = 0L // Reiniciamos para la siguiente pausa
        }
    }

    fun getTotalSeconds(): Long {
        if (!isRunning) return 0L
        val endTime = SystemClock.elapsedRealtime()

        // Fórmula: (Tiempo Total - Tiempo Pausado) / 1000 para segundos
        return (endTime - startTime - totalPausedTime) / 1000
    }
}
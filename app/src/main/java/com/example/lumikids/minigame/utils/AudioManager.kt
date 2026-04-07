package com.example.lumikids.minigame.utils

import android.content.Context
import android.media.MediaPlayer

class AudioManager(private val context: Context) : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    override fun playObjectAudio(audioIdentifier: String) {
        mediaPlayer?.release()

        val resId = context.resources.getIdentifier(audioIdentifier, "raw", context.packageName)

        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer?.start()
        }
    }

    override fun playEffect(resId: Int) {
        MediaPlayer.create(context, resId)?.apply {
            setOnCompletionListener { release() }
            start()
        }
    }

    override fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
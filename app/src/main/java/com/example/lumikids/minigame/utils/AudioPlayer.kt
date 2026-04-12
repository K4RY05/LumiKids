package com.example.lumikids.minigame.utils

interface AudioPlayer {
    fun playObjectAudio(audioIdentifier: String)
    fun playEffect(resId: Int)
    fun release()
}
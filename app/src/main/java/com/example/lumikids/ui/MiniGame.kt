package com.example.lumikids.ui

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lumikids.R
import com.example.lumikids.model.GameResult
import com.example.lumikids.utils.GameAudioManager
import com.example.lumikids.utils.GameTimer
import com.example.lumikids.utils.ScoreManager
import android.view.KeyEvent


abstract class MiniGame : BaseActivity() {

    protected lateinit var theme: String
    protected var errors: Int = 0
    protected val gameTimer = GameTimer()
    protected lateinit var gameAudio: GameAudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val audioService = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioService.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val safeMaxVolume = (maxVolume * 0.7f).toInt()

        if (audioService.getStreamVolume(AudioManager.STREAM_MUSIC) > safeMaxVolume) {
            audioService.setStreamVolume(AudioManager.STREAM_MUSIC, safeMaxVolume, 0)
        }

        theme = intent.getStringExtra("THEME") ?: "furniture"

        gameAudio = GameAudioManager(this, lifecycleScope)
    }

    protected fun showResults(gameTitle: String) {
        gameAudio.playEffect(R.raw.win)
        val finalResult = GameResult(gameTimer.getTotalSeconds(), errors, gameTitle)
        ScoreManager(this).showResults(finalResult) { finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameAudio.releaseAll()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val audioService = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVolume = audioService.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val safeMaxVolume = (maxVolume * 0.8f).toInt()
            val currentVolume = audioService.getStreamVolume(AudioManager.STREAM_MUSIC)

            if (currentVolume >= safeMaxVolume) {
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    abstract fun initViews()
    abstract fun loadGameData()
}
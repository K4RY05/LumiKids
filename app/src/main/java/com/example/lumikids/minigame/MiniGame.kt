package com.example.lumikids.minigame

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lumikids.R
import com.example.lumikids.model.GameResult
import com.example.lumikids.utils.GameAudioManager
import com.example.lumikids.utils.GameTimer
import com.example.lumikids.utils.ScoreManager

abstract class MiniGame : AppCompatActivity() {

    protected lateinit var theme: String
    protected var errors: Int = 0
    protected val gameTimer = GameTimer()
    protected lateinit var gameAudio: GameAudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


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

    abstract fun initViews()
    abstract fun loadGameData()
}
package com.example.lumikids.minigame.objectrecognition.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AppCompatActivity

import com.example.lumikids.R
import com.example.lumikids.model.GameTheme
import com.example.lumikids.minigame.objectrecognition.controller.ObjectRecognitionController
import com.example.lumikids.minigame.utils.AudioPlayer
import com.example.lumikids.minigame.utils.AudioManager
import com.example.lumikids.minigame.utils.ScoreManager
import com.example.lumikids.minigame.utils.PauseDialog // ✨ Diálogo de pausa

class GameActivityN1 : AppCompatActivity() {

    private lateinit var controller: ObjectRecognitionController

    private lateinit var audioManager: AudioPlayer
    private lateinit var theme: GameTheme
    private lateinit var tvInstruction: TextView
    private lateinit var images: List<ImageView>

    private lateinit var btnBack: ImageView
    private lateinit var btnPause: ImageView // ✨ Botón de pausa

    private lateinit var uiHandler: GameUIHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n1)

        // Modo Inmersivo (Pantalla Completa)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        theme = intent.getStringExtra("THEME")?.let {
            GameTheme.valueOf(it)
        } ?: GameTheme.FURNITURE

        val rounds = intent.getIntExtra("NUM_ROUNDS", 3)
        controller = ObjectRecognitionController(this, theme, rounds)

        audioManager = AudioManager(this)

        initViews()

        uiHandler = GameUIHandler(
            images = images,
            audioManager = audioManager,
            onNextRound = { startNewRound() },
            onError = { controller.addError() }
        )

        startNewRound()
    }

    private fun initViews() {
        tvInstruction = findViewById(R.id.tvInstruction)
        btnBack = findViewById(R.id.btnBack)
        btnPause = findViewById(R.id.btnPause)

        images = listOf(
            findViewById(R.id.imgOption1),
            findViewById(R.id.imgOption2),
            findViewById(R.id.imgOption3)
        )

        // Evento Botón Regresar (Este es el que saca al niño del nivel)
        btnBack.setOnClickListener { finish() }

        // ✨ EVENTO BOTÓN PAUSA CORREGIDO ✨
        btnPause.setOnClickListener {
            val pauseDialog = PauseDialog(this)

            // Llamamos a la nueva versión simplificada que solo requiere "onResume"
            pauseDialog.showDialog(
                onResume = {

                }
            )
        }
    }

    private fun startNewRound() {
        if (controller.isGameOver()) {
            mostrarResultadosFinales()
            return
        }

        val round = controller.getNewRound()

        if (round != null) {
            tvInstruction.text = round.correctObject.instructionText
            uiHandler.resetImagesBackground()

            round.options.forEachIndexed { index, gameObject ->
                images[index].apply {
                    setImageResource(gameObject.imageResId)
                    isEnabled = true
                    setOnClickListener {
                        val isCorrect = controller.checkAnswer(gameObject)
                        uiHandler.handleSelection(this, isCorrect)
                    }
                }
            }
        } else {
            Toast.makeText(this, "No hay suficientes objetos", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun mostrarResultadosFinales() {
        audioManager.playEffect(R.raw.win)
        val finalResult = controller.getFinalResult("Identificar Objeto")

        ScoreManager(this).showResults(finalResult) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
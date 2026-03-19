package com.example.lumikids.minigame.objectrecognition.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.minigame.objectrecognition.controller.ObjectRecognitionController
import com.example.lumikids.minigame.objectrecognition.model.GameObject
import com.example.lumikids.minigame.objectrecognition.model.ObjectRound
import com.example.lumikids.minigame.utils.AudioPlayer
import com.example.lumikids.minigame.utils.AudioManager

class GameActivityN1 : AppCompatActivity() {

    private lateinit var controller: ObjectRecognitionController
    private var currentRound: ObjectRound? = null

    private var totalRoundsWanted: Int = 3
    private var currentRoundCount: Int = 0

    private var isFirstRound = true
    private lateinit var audioManager: AudioPlayer

    private lateinit var tvInstruction: TextView
    private lateinit var images: List<ImageView>
    private lateinit var btnBack: ImageView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n1)

        val theme = intent.getStringExtra("THEME") ?: "FURNITURE"

        totalRoundsWanted = intent.getIntExtra("NUM_ROUNDS", 3)

        controller = ObjectRecognitionController(this, theme)
        audioManager = AudioManager(this)

        initViews()

        Toast.makeText(this, "Rondas totales: $totalRoundsWanted", Toast.LENGTH_SHORT).show()

        startNewRound()
    }

    private fun initViews() {
        tvInstruction = findViewById(R.id.tvInstruction)
        btnBack = findViewById(R.id.btnBack)

        images = listOf(
            findViewById(R.id.imgOption1),
            findViewById(R.id.imgOption2),
            findViewById(R.id.imgOption3)
        )

        btnBack.setOnClickListener { finish() }
    }

    private fun startNewRound() {
        if (currentRoundCount >= totalRoundsWanted) {
            finalizarJuego()
            return
        }

        currentRoundCount++

        currentRound = controller.getNewRound()

        currentRound?.let { round ->
            // Actualizar textos y limpiar colores (Código original)
            tvInstruction.text = round.correctObject.instructionText
            resetImagesBackground()

            round.options.forEachIndexed { index, gameObject ->
                images[index].setImageResource(gameObject.imageResId)
                images[index].isEnabled = true
                images[index].setOnClickListener {
                    handleOptionClick(gameObject, images[index])
                }
            }

            audioManager.playObjectAudio(round.correctObject.name)

        } ?: run {
            Toast.makeText(this, "No hay suficientes objetos", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleOptionClick(clickedObject: GameObject, view: ImageView) {

        val correctObj = currentRound?.correctObject ?: return
        val isCorrect = controller.isCorrect(clickedObject, correctObj)

        if (isCorrect) {
            view.setBackgroundResource(R.drawable.rounded_green_bg)
            audioManager.playEffect(R.raw.win)
            Toast.makeText(this, "¡Muy bien!", Toast.LENGTH_SHORT).show()

            images.forEach {
                it.isEnabled = false
                it.setOnClickListener(null)
            }

            tvInstruction.postDelayed({
                startNewRound()
            }, 1000)

        } else {
            view.setBackgroundResource(R.drawable.rounded_red_bg)
            audioManager.playEffect(R.raw.fail)
            Toast.makeText(this, "Intenta de nuevo", Toast.LENGTH_SHORT).show()
            view.isEnabled = false
            view.postDelayed({
                view.setBackgroundResource(R.drawable.rounded_white_bg)
                view.isEnabled = true
            }, 500)
        }
    }

    private fun finalizarJuego() {
        audioManager.playEffect(R.raw.win)

        Toast.makeText(this, "¡Felicidades! Terminaste todas las rondas", Toast.LENGTH_LONG).show()

        images.forEach { it.isEnabled = false }

        tvInstruction.postDelayed({
            finish()
        }, 2000)
    }

    // Métodos auxiliares y onDestroy (Código original)
    private fun resetImagesBackground() {
        images.forEach { it.setBackgroundResource(R.drawable.rounded_white_bg) }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
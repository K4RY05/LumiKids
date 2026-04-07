package com.example.lumikids.minigame.objectrecognition.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.lumikids.R
import com.example.lumikids.minigame.objectrecognition.controller.ObjectRecognitionController
import com.example.lumikids.minigame.utils.AudioManager
import com.example.lumikids.minigame.utils.AudioPlayer
import com.example.lumikids.minigame.utils.PauseDialog
import com.example.lumikids.minigame.utils.ScoreManager
import com.example.lumikids.network.RetrofitClient
import kotlinx.coroutines.launch

class GameActivityN1 : AppCompatActivity() {

    private lateinit var controller: ObjectRecognitionController
    private lateinit var audioManager: AudioPlayer

    private lateinit var theme: String
    private lateinit var tvInstruction: TextView
    private lateinit var images: List<ImageView>

    private lateinit var btnPause: ImageView
    private lateinit var uiHandler: GameUIHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n1)

        // Configuración de pantalla completa
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())

        theme = intent.getStringExtra("THEME") ?: "furniure"
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

        lifecycleScope.launch {
            val success = controller.cargarDatos()
            if (success) {
                startNewRound()
            } else {
                Toast.makeText(this@GameActivityN1, "Error al cargar datos del servidor", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun initViews() {
        tvInstruction = findViewById(R.id.tvInstruction)
        btnPause = findViewById(R.id.btnPause)

        images = listOf(
            findViewById(R.id.imgOption1),
            findViewById(R.id.imgOption2),
            findViewById(R.id.imgOption3)
        )

        // ✨ ACTUALIZACIÓN: El botón de pausa ahora maneja Reanudar y Salir
        btnPause.setOnClickListener {
            val pauseDialog = PauseDialog(this)
            pauseDialog.showDialog(
                onResume = {
                    // El juego continúa normalmente
                },
                onExit = {
                    finish() // ✨ Cierra la actividad y vuelve al menú
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
                    Glide.with(this@GameActivityN1)
                        .load(RetrofitClient.BASE_URL_IMAGES + gameObject.imageUrl)
                        .placeholder(R.drawable.ic_logo)
                        .transform(CenterCrop(), RoundedCorners(30))
                        .into(this)

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
        ScoreManager(this).showResults(finalResult) { finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
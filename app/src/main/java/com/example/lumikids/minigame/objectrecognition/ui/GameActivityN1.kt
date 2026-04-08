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
import com.example.lumikids.minigame.utils.NetworkAudioManager
import com.example.lumikids.minigame.utils.PauseDialog
import com.example.lumikids.minigame.utils.ScoreManager
import com.example.lumikids.network.RetrofitClient
import kotlinx.coroutines.launch

class GameActivityN1 : AppCompatActivity() {

    private lateinit var controller: ObjectRecognitionController
    private lateinit var audioManager: AudioPlayer
    private val networkAudio = NetworkAudioManager()
    private lateinit var uiHandler: GameUIHandler

    private lateinit var theme: String
    private lateinit var tvInstruction: TextView
    private lateinit var images: List<ImageView>

    // ✨ CORRECCIÓN 1: Declaramos la lista para los nuevos íconos flotantes
    private lateinit var resultIcons: List<ImageView>

    private lateinit var btnPause: ImageView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n1)

        setupImmersiveMode()

        theme = intent.getStringExtra("THEME") ?: "furniure"
        val rounds = intent.getIntExtra("NUM_ROUNDS", 3)

        controller = ObjectRecognitionController(this, theme, rounds)
        audioManager = AudioManager(this)

        initViews()

        // ✨ CORRECCIÓN 3: Le pasamos la lista de íconos al UIHandler
        uiHandler = GameUIHandler(
            images = images,
            resultIcons = resultIcons,
            audioManager = audioManager,
            onNextRound = { startNewRound() },
            onError = { controller.addError() }
        )

        cargarDatosDelJuego()
    }

    private fun setupImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun initViews() {
        tvInstruction = findViewById(R.id.tvInstruction)
        btnPause = findViewById(R.id.btnPause)
        btnBack = findViewById(R.id.btnBack)

        images = listOf(
            findViewById(R.id.imgOption1),
            findViewById(R.id.imgOption2),
            findViewById(R.id.imgOption3)
        )

        // ✨ CORRECCIÓN 2: Enlazamos los íconos que agregamos al XML
        resultIcons = listOf(
            findViewById(R.id.imgResult1),
            findViewById(R.id.imgResult2),
            findViewById(R.id.imgResult3)
        )

        btnBack.setOnClickListener { finish() }

        btnPause.setOnClickListener {
            PauseDialog(this).showDialog(
                onResume = { /* El juego continúa normalmente */ },
                onExit = { finish() }
            )
        }
    }

    private fun cargarDatosDelJuego() {
        lifecycleScope.launch {
            val success = controller.cargarDatos()
            if (success) {
                startNewRound()
            } else {
                Toast.makeText(this@GameActivityN1, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startNewRound() {
        if (controller.isGameOver()) {
            mostrarResultadosFinales()
            return
        }

        val round = controller.getNewRound()
        if (round == null) {
            Toast.makeText(this, "No hay suficientes objetos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val objetoCorrecto = round.correctObject
        tvInstruction.text = objetoCorrecto.instructionText

        if (objetoCorrecto.audioInstructionUrl != null) {
            val urlCompleta = RetrofitClient.BASE_URL_SOUNDS + objetoCorrecto.audioInstructionUrl
            networkAudio.playAudioFromUrl(urlCompleta)
        }

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

                    if (gameObject.audioShortUrl != null) {
                        val urlCortaCompleta = RetrofitClient.BASE_URL_SOUNDS + gameObject.audioShortUrl
                        networkAudio.playAudioFromUrl(urlCortaCompleta)
                    }

                    uiHandler.handleSelection(this, isCorrect)
                }
            }
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
        networkAudio.stopAudio()
    }
}
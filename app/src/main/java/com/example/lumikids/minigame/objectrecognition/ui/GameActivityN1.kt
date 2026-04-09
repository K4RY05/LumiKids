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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class GameActivityN1 : AppCompatActivity() {

    private lateinit var controller: ObjectRecognitionController
    private lateinit var audioManager: AudioPlayer
    private val networkAudio = NetworkAudioManager()
    private lateinit var uiHandler: GameUIHandler

    private lateinit var theme: String
    private lateinit var tvInstruction: TextView
    private lateinit var images: List<ImageView>
    private lateinit var resultIcons: List<ImageView>

    private lateinit var btnPause: ImageView
    private lateinit var btnBack: ImageView

    private var repeticionAudioJob: Job? = null
    // ✨ PASO 1: Variable para recordar el audio de la ronda actual
    private var urlAudioActual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n1)

        setupImmersiveMode()

        theme = intent.getStringExtra("THEME") ?: "furniure"
        val rounds = intent.getIntExtra("NUM_ROUNDS", 3)

        controller = ObjectRecognitionController(this, theme, rounds)
        audioManager = AudioManager(this)

        initViews()

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

        resultIcons = listOf(
            findViewById(R.id.imgResult1),
            findViewById(R.id.imgResult2),
            findViewById(R.id.imgResult3)
        )

        btnBack.setOnClickListener { finish() }

        btnPause.setOnClickListener {
            controller.pauseTimer()

            // Cancelamos la repetición para que no suene en el menú de pausa
            repeticionAudioJob?.cancel()

            PauseDialog(this).showDialog(
                onResume = {
                    controller.resumeTimer()
                    // ✨ PASO 3: Reanudamos el bucle al volver al juego
                    iniciarBucleDeAudio()
                },
                onExit = { finish() }
            )
        }
    }

    // ✨ PASO 2: Función dedicada a manejar el bucle de repetición
    private fun iniciarBucleDeAudio() {
        val url = urlAudioActual ?: return // Si no hay audio cargado, salimos

        repeticionAudioJob?.cancel() // Detenemos cualquier proceso previo

        repeticionAudioJob = lifecycleScope.launch {
            while (isActive) {
                networkAudio.playAudioFromUrl(url)
                delay(8000)
            }
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

        // ✨ ACTUALIZACIÓN: Guardamos la URL y lanzamos el bucle inicial
        if (objetoCorrecto.audioInstructionUrl != null) {
            urlAudioActual = RetrofitClient.BASE_URL_SOUNDS + objetoCorrecto.audioInstructionUrl
            iniciarBucleDeAudio()
        } else {
            urlAudioActual = null
            repeticionAudioJob?.cancel()
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
                    // Si el niño toca una opción, el audio debe detenerse por completo
                    repeticionAudioJob?.cancel()

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
        repeticionAudioJob?.cancel()
        audioManager.release()
        networkAudio.stopAudio()
    }
}
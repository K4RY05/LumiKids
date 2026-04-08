package com.example.lumikids.minigame.objectrecognition.ui

import android.os.Bundle
import android.util.Log
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

    private lateinit var btnPause: ImageView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n1)

        setupImmersiveMode()

        // ✨ Corrección: el default ahora está bien escrito ("furniture")
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

        val claveInstruccion = "${theme.lowercase()}_${objetoCorrecto.name}"
        reproducirAudio(claveInstruccion, true)

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

                    // Reproducir el nombre corto (ej. "hat")
                    reproducirAudio(gameObject.name, false)

                    uiHandler.handleSelection(this, isCorrect)
                }
            }
        }
    }

    private fun reproducirAudio(claveBuscada: String, mostrarError: Boolean) {
        val listaSonidos = controller.listaDeSonidos

        if (listaSonidos.isEmpty()) {
            if (mostrarError) Toast.makeText(this, "Lista de audios vacía.", Toast.LENGTH_SHORT).show()
            return
        }

        val sonido = listaSonidos.find { it.namesounds == claveBuscada }

        if (sonido != null) {
            val urlCompleta = RetrofitClient.BASE_URL_SOUNDS + sonido.linksounds
            Log.d("AUDIO_TEST", "Reproduciendo: $urlCompleta")
            networkAudio.playAudioFromUrl(urlCompleta)
        } else {
            if (mostrarError) Toast.makeText(this, "Falta audio: $claveBuscada", Toast.LENGTH_SHORT).show()
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
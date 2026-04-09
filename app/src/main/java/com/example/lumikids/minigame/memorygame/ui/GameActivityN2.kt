package com.example.lumikids.minigame.memorygame.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.lumikids.R
import com.example.lumikids.minigame.memorygame.controller.MemoryGameController
import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.minigame.utils.AudioManager
import com.example.lumikids.minigame.utils.NetworkAudioManager
import com.example.lumikids.minigame.utils.PauseDialog
import com.example.lumikids.minigame.utils.ScoreManager
import com.example.lumikids.network.RetrofitClient
import kotlinx.coroutines.launch

class GameActivityN2 : AppCompatActivity() {

    private lateinit var theme: String
    private var numCards: Int = 6

    private lateinit var controller: MemoryGameController
    private lateinit var boardCards: List<MemoryCard>

    private val uiHandler = MemoryGameUIHandler()
    private lateinit var audioManager: AudioManager
    private val networkAudio = NetworkAudioManager()

    private var firstSelectedCard: MemoryCard? = null
    private var firstSelectedView: ImageView? = null
    private var isBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n2)

        setupImmersiveMode()

        theme = intent.getStringExtra("THEME") ?: "furniure"
        val numPairs = intent.getIntExtra("NUM_CARDS", 6) / 2 // Convertimos total de cartas a pares

        controller = MemoryGameController(numPairs)
        audioManager = AudioManager(this)

        initViews()
        cargarDatosDelJuego()
    }

    private fun setupImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnPause).setOnClickListener {
            // ✨ OPTIMIZACIÓN: Congelamos el reloj interno del juego al pausar
            controller.pauseTimer()

            PauseDialog(this).showDialog(
                onResume = {
                    // ✨ OPTIMIZACIÓN: Reanudamos el reloj cuando el niño vuelve a jugar
                    controller.resumeTimer()
                },
                onExit = { finish() }
            )
        }
    }

    private fun cargarDatosDelJuego() {
        lifecycleScope.launch {
            val success = controller.cargarDatos(theme)
            if (success) {
                boardCards = controller.cards
                if (boardCards.isEmpty()) {
                    Toast.makeText(this@GameActivityN2, "No hay cartas disponibles", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    crearTableroDinamico()
                }
            } else {
                Toast.makeText(this@GameActivityN2, "Error al cargar datos del servidor", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun crearTableroDinamico() {
        val grid = findViewById<GridLayout>(R.id.gridMemorama)
        grid.removeAllViews()

        val columns = boardCards.size / 2
        grid.columnCount = columns
        grid.rowCount = 2

        val displayMetrics = resources.displayMetrics
        val maxWidthPerCard = (displayMetrics.widthPixels * 0.95f) / columns
        val maxHeightPerCard = (displayMetrics.heightPixels * 0.70f) / 2f

        val baseSize = kotlin.math.min(maxWidthPerCard, maxHeightPerCard).toInt()
        val margin = (baseSize * 0.05).toInt()
        val size = baseSize - (margin * 2)

        for (i in boardCards.indices) {
            val cardData = boardCards[i]
            val view = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                    setGravity(Gravity.CENTER)
                }
                setBackgroundResource(R.drawable.bg_card)
                setPadding(margin, margin, margin, margin)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.ic_logo)
                tag = false // Estado visual: boca abajo
            }

            view.setOnClickListener { handleCardClick(view, cardData, i) }
            grid.addView(view)
        }
    }

    private fun handleCardClick(view: ImageView, card: MemoryCard, position: Int) {
        if (isBusy || view.tag == true || card.isMatched) return

        // Volteamos la carta usando la URL del objeto universal
        uiHandler.flipCardUp(view, card.gameObject.imageUrl)
        view.tag = true

        if (firstSelectedCard == null) {
            firstSelectedCard = card
            firstSelectedView = view
        } else {
            isBusy = true
            // Delegamos la lógica de comparación al controlador
            val isMatch = controller.isMatch(firstSelectedCard!!, card)

            if (isMatch) {
                // Reproducimos el sonido corto del objeto como refuerzo positivo
                card.gameObject.audioShortUrl?.let {
                    networkAudio.playAudioFromUrl(RetrofitClient.BASE_URL_SOUNDS + it)
                } ?: audioManager.playEffect(R.raw.win)

                resetSelection()
                isBusy = false
                checkGameFinished()
            } else {
                audioManager.playEffect(R.raw.fail)
                view.postDelayed({
                    uiHandler.flipCardsDown(firstSelectedView!!, view, R.drawable.ic_logo) {
                        view.tag = false
                        firstSelectedView?.tag = false
                        resetSelection()
                        isBusy = false
                    }
                }, 1000)
            }
        }
    }

    private fun checkGameFinished() {
        if (controller.isGameOver()) {
            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                audioManager.playEffect(R.raw.win)
                val finalResult = controller.getFinalResult("Memorama")
                ScoreManager(this@GameActivityN2).showResults(finalResult) { finish() }
            }
        }
    }

    private fun resetSelection() {
        firstSelectedCard = null
        firstSelectedView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
        networkAudio.stopAudio()
    }
}
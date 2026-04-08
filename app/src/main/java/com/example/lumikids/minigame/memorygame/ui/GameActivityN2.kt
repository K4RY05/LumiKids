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
import com.example.lumikids.minigame.utils.ScoreManager
import com.example.lumikids.minigame.utils.AudioManager
import com.example.lumikids.minigame.utils.PauseDialog
import kotlinx.coroutines.launch

class GameActivityN2 : AppCompatActivity() {

    private lateinit var theme: String
    private var numCards: Int = 6

    private lateinit var controller: MemoryGameController
    private lateinit var boardCards: List<MemoryCard>

    private val uiHandler = MemoryGameUIHandler()
    private lateinit var audioManager: AudioManager

    private var firstSelectedCard: MemoryCard? = null
    private var firstSelectedView: ImageView? = null
    private var isBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n2)

        setupImmersiveMode()

        // ✨ Corrección: el nombre por defecto ahora es correcto
        theme = intent.getStringExtra("THEME") ?: "furniture"
        numCards = intent.getIntExtra("NUM_CARDS", 6)

        controller = MemoryGameController(this, theme, numCards)
        audioManager = AudioManager(this)

        initViews()
        cargarDatosDelJuego()
    }

    // ✨ OPTIMIZACIÓN: Modularizamos la pantalla inmersiva
    private fun setupImmersiveMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
    }

    // ✨ OPTIMIZACIÓN: Modularizamos los botones
    private fun initViews() {
        val btnPause = findViewById<ImageView>(R.id.btnPause)
        val btnBack = findViewById<ImageView>(R.id.btnBack) // ✨ NUEVO: Enlazamos el botón back

        btnBack.setOnClickListener {
            finish()
        }

        btnPause.setOnClickListener {
            PauseDialog(this).showDialog(
                onResume = { /* El juego continúa */ },
                onExit = { finish() }
            )
        }
    }

    // ✨ OPTIMIZACIÓN: Descarga de datos en segundo plano
    private fun cargarDatosDelJuego() {
        lifecycleScope.launch {
            val success = controller.cargarDatos()
            if (success) {
                boardCards = controller.generateBoard()
                if (boardCards.isEmpty()) {
                    Toast.makeText(this@GameActivityN2, "No hay cartas disponibles", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    crearTableroDinamico()
                }
            } else {
                Toast.makeText(this@GameActivityN2, "Error de red al cargar el tablero", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun crearTableroDinamico() {
        val grid = findViewById<GridLayout>(R.id.gridMemorama)
        grid.removeAllViews()

        // Ajustamos dinámicamente cuántas columnas tendrá el tablero (Ej: 8 cartas = 4 columnas)
        val columns = numCards / 2
        grid.columnCount = columns
        grid.rowCount = 2

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Cálculo de espacio dinámico para que las cartas no se salgan de la pantalla
        val maxWidthPerCard = (screenWidth * 0.95f) / columns
        val maxHeightPerCard = (screenHeight * 0.75f) / 2f

        val baseSize = kotlin.math.min(maxWidthPerCard, maxHeightPerCard).toInt()
        val margin = (baseSize * 0.05).toInt()
        val size = baseSize - (margin * 2)
        val padding = margin

        for (i in 0 until numCards) {
            val cardData = boardCards[i]
            val view = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                    setGravity(Gravity.CENTER) // ✨ NUEVO: Asegura que las cartas se centren en sus casillas
                }
                setBackgroundResource(R.drawable.bg_card)
                setPadding(padding, padding, padding, padding)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.ic_logo) // Logo del reverso de la carta
                tag = false
            }

            view.setOnClickListener {
                handleCardClick(view, cardData)
            }
            grid.addView(view)
        }
    }

    private fun handleCardClick(view: ImageView, card: MemoryCard) {
        // Evita seleccionar más de 2 cartas al mismo tiempo o cartas ya volteadas
        if (isBusy || view.tag == true) return

        uiHandler.flipCardUp(view, card.imageUrl)
        view.tag = true

        if (firstSelectedCard == null) {
            // Es la primera carta que toca el niño en este turno
            firstSelectedCard = card
            firstSelectedView = view
        } else {
            // Es la segunda carta
            isBusy = true
            val isMatch = controller.isMatch(firstSelectedCard!!, card)

            if (isMatch) {
                // ✨ Acierto: reproducir sonido y mantener volteadas
                audioManager.playEffect(R.raw.win)
                resetSelection()
                isBusy = false
                checkGameFinished()
            } else {
                // ✨ Error: reproducir sonido y regresar cartas
                audioManager.playEffect(R.raw.fail)

                // Esperamos 1 segundo para que el niño memorice las cartas antes de voltearlas
                view.postDelayed({
                    uiHandler.flipCardsDown(firstSelectedView!!, view, R.drawable.ic_logo) {
                        view.tag = false
                        firstSelectedView!!.tag = false
                        resetSelection()
                        isBusy = false
                    }
                }, 1000)
            }
        }
    }

    private fun checkGameFinished() {
        if (controller.isGameOver()) {
            audioManager.playEffect(R.raw.win)
            val finalResult = controller.getFinalResult("Memorama")
            ScoreManager(this).showResults(finalResult) {
                finish()
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
    }
}
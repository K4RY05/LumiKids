package com.example.lumikids.minigame.memorygame.ui

import android.os.Bundle
import android.util.TypedValue
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.model.GameTheme
import com.example.lumikids.minigame.memorygame.controller.MemoryGameController
import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.minigame.utils.ScoreManager
import com.example.lumikids.minigame.utils.AudioManager
import com.example.lumikids.minigame.utils.PauseDialog

class GameActivityN2 : AppCompatActivity() {

    private lateinit var theme: GameTheme
    private var numCards: Int = 6

    private lateinit var controller: MemoryGameController
    private lateinit var boardCards: List<MemoryCard>

    private val uiHandler = MemoryGameUIHandler()

    private lateinit var audioManager: AudioManager

    // Solo conservamos el estado visual temporal
    private var firstSelectedCard: MemoryCard? = null
    private var firstSelectedView: ImageView? = null
    private var isBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n2)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        theme = intent.getStringExtra("THEME")?.let {
            GameTheme.valueOf(it)
        } ?: GameTheme.FURNITURE

        numCards = intent.getIntExtra("NUM_CARDS", 6)

        controller = MemoryGameController(theme, numCards)
        boardCards = controller.generateBoard()

        audioManager = AudioManager(this)

        if (boardCards.isEmpty()) {
            Toast.makeText(this, "No se encontraron cartas para este tema", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // ✨ NUEVO: Evento del botón de pausa
        val btnPause = findViewById<ImageView>(R.id.btnPause)
        btnPause.setOnClickListener {
            val pauseDialog = PauseDialog(this)
            pauseDialog.showDialog(
                onResume = {
                    // El usuario presionó el botón de reanudar (Play).
                    // El diálogo ya se cerró solo, aquí el juego puede continuar.
                }
            )
        }

        crearTableroDinamico()
    }

    private fun crearTableroDinamico() {
        val grid = findViewById<GridLayout>(R.id.gridMemorama)
        grid.removeAllViews()

        val columns = numCards / 2
        grid.columnCount = columns
        grid.rowCount = 2

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

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
                }
                setBackgroundResource(R.drawable.bg_button)
                setPadding(padding, padding, padding, padding)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.ic_logo)
                tag = false
            }

            view.setOnClickListener {
                handleCardClick(view, cardData)
            }
            grid.addView(view)
        }
    }

    private fun handleCardClick(view: ImageView, card: MemoryCard) {
        if (isBusy || view.tag == true) return

        uiHandler.flipCardUp(view, card.imageResId)
        view.tag = true

        if (firstSelectedCard == null) {
            firstSelectedCard = card
            firstSelectedView = view
        } else {
            isBusy = true

            val isMatch = controller.isMatch(firstSelectedCard!!, card)

            if (isMatch) {
                audioManager.playEffect(R.raw.win)

                resetSelection()
                isBusy = false
                checkGameFinished()
            } else {
                audioManager.playEffect(R.raw.fail)

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

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }


    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}
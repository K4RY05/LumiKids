package com.example.lumikids.minigame.memorygame.ui

import android.os.Bundle
import android.util.TypedValue
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.model.GameTheme
import com.example.lumikids.minigame.memorygame.controller.MemoryGameController
import com.example.lumikids.minigame.memorygame.model.MemoryCard

class GameActivityN2 : AppCompatActivity() {

    private lateinit var theme: GameTheme
    private var numCards: Int = 6

    private lateinit var controller: MemoryGameController
    private lateinit var boardCards: List<MemoryCard>

    private var firstSelectedCard: MemoryCard? = null
    private var firstSelectedView: ImageView? = null
    private var isBusy = false

    private var matchedPairs = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n2)

        theme = intent.getStringExtra("THEME")?.let {
            GameTheme.valueOf(it)
        } ?: GameTheme.FURNITURE

        numCards = intent.getIntExtra("NUM_CARDS", 6)

        controller = MemoryGameController(theme, numCards)
        boardCards = controller.generateBoard()

        if (boardCards.isEmpty()) {
            Toast.makeText(this, "Error: No se encontraron cartas para este tema", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

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
        val maxHeightPerCard = (screenHeight * 0.75f) / 2f // 2 filas siempre

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
                setBackgroundResource(R.drawable.rounded_white_bg)
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

        animateFlip(view, card.imageResId)
        view.tag = true

        if (firstSelectedCard == null) {
            firstSelectedCard = card
            firstSelectedView = view
        } else {

            isBusy = true

            val isMatch = controller.isMatch(firstSelectedCard!!, card)

            if (isMatch) {
                Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()

                matchedPairs++

                resetSelection()
                isBusy = false

                checkGameFinished()

            } else {
                view.postDelayed({

                    animateFlip(view, R.drawable.ic_logo)
                    animateFlip(firstSelectedView!!, R.drawable.ic_logo)

                    view.tag = false
                    firstSelectedView!!.tag = false

                    resetSelection()
                    isBusy = false

                }, 1000)
            }
        }
    }

    private fun checkGameFinished() {
        val totalPairs = numCards / 2

        if (matchedPairs == totalPairs) {
            Toast.makeText(this, "¡Ganaste!", Toast.LENGTH_LONG).show()

            window.decorView.postDelayed({
                finish()
            }, 1500)
        }
    }

    private fun resetSelection() {
        firstSelectedCard = null
        firstSelectedView = null
    }

    private fun animateFlip(view: ImageView, newImage: Int) {
        val duration = 150L

        view.animate().rotationY(90f).setDuration(duration).withEndAction {
            view.setImageResource(newImage)
            view.rotationY = -90f
            view.animate().rotationY(0f).setDuration(duration).start()
        }.start()
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }
}
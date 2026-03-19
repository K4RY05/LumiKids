package com.example.lumikids.minigame.memorygame.ui

import android.os.Bundle
import android.util.TypedValue
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.minigame.memorygame.controller.MemoryGameController
import com.example.lumikids.minigame.memorygame.model.MemoryCard

class GameActivityN2 : AppCompatActivity() {

    private var theme: String = "FURNITURE"
    private var numCards: Int = 6

    private lateinit var controller: MemoryGameController
    private lateinit var boardCards: List<MemoryCard>

    private var firstSelectedCard: MemoryCard? = null
    private var firstSelectedView: ImageView? = null
    private var isBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n2)

        theme = intent.getStringExtra("THEME") ?: "FURNITURE"
        numCards = intent.getIntExtra("NUM_CARDS", 6)

        controller = MemoryGameController(theme, numCards)

        // Le pedimos al controlador el mazo ya mezclado
        boardCards = controller.generateBoard()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        crearTableroDinamico()
    }

    private fun crearTableroDinamico() {
        val gridMemorama = findViewById<GridLayout>(R.id.gridMemorama)
        gridMemorama.removeAllViews()

        gridMemorama.columnCount = numCards / 2
        gridMemorama.rowCount = 2

        val sizeInPx = resources.getDimensionPixelSize(R.dimen.tarjeta_memorama)
        val marginInPx = dpToPx(8f)
        val paddingInPx = dpToPx(12f)

        for (i in 0 until numCards) {
            val cardData = boardCards[i]

            val nuevaVistaCarta = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = sizeInPx
                    height = sizeInPx
                    setMargins(marginInPx, marginInPx, marginInPx, marginInPx)
                }
                setBackgroundResource(R.drawable.rounded_white_bg)
                setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.ic_logo) // Inician boca abajo

                // Usamos "tag" para recordar en la pantalla si está boca arriba o boca abajo
                tag = "faceDown"
            }

            // Manejador de clics
            nuevaVistaCarta.setOnClickListener {
                handleCardClick(nuevaVistaCarta, cardData)
            }

            gridMemorama.addView(nuevaVistaCarta)
        }
    }

    // LÓGICA DE INTERACCIÓN DE LA VISTA
    private fun handleCardClick(clickedView: ImageView, cardData: MemoryCard) {
        // Bloqueo Anti-Spam (Si el juego está procesando o la carta ya está volteada, ignoramos el clic)
        if (isBusy || clickedView.tag == "faceUp") return

        // 1. Volteamos visualmente la carta elegida
        animateFlip(clickedView, cardData.imageResId)
        clickedView.tag = "faceUp"

        // 2. Evaluamos la jugada
        if (firstSelectedCard == null) {
            // Es la primera carta que voltea
            firstSelectedCard = cardData
            firstSelectedView = clickedView
        } else {
            // Es la segunda carta que voltea (Momento de validar)
            isBusy = true
            val isMatch = controller.isMatch(firstSelectedCard!!, cardData)

            if (isMatch) {
                // --- ACIERTO ---
                // Aquí en el futuro llamaremos a tu AudioManager para reproducir el nombre
                Toast.makeText(this, "¡Correcto!", Toast.LENGTH_SHORT).show()

                firstSelectedCard = null
                firstSelectedView = null
                isBusy = false
            } else {
                // --- ERROR ---
                clickedView.postDelayed({
                    animateFlip(clickedView, R.drawable.ic_logo)
                    animateFlip(firstSelectedView!!, R.drawable.ic_logo)

                    clickedView.tag = "faceDown"
                    firstSelectedView!!.tag = "faceDown"

                    firstSelectedCard = null
                    firstSelectedView = null
                    isBusy = false // Desbloqueamos el tablero
                }, 1000)
            }
        }
    }

    private fun animateFlip(view: ImageView, newImageResId: Int) {
        val duration = 150L
        view.animate().rotationY(90f).setDuration(duration).withEndAction {
            view.setImageResource(newImageResId)
            view.rotationY = -90f
            view.animate().rotationY(0f).setDuration(duration).start()
        }.start()
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
    }
}
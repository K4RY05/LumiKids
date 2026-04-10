package com.example.lumikids.minigame.memorygame

import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.lumikids.R
import com.example.lumikids.minigame.MiniGame
import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.utils.PauseDialog
import com.example.lumikids.network.AuthApi
import com.example.lumikids.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import kotlin.math.min

class MemoryGame : MiniGame() {

    private var boardCards: List<MemoryCard> = emptyList()
    private var numPairsWanted: Int = 3

    private var firstSelectedCard: MemoryCard? = null
    private var firstSelectedView: ImageView? = null
    private var isBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n2)

        numPairsWanted = intent.getIntExtra("NUM_CARDS", 6) / 2

        initViews()
        loadGameData()
    }

    // ✨ Agregamos 'override' porque es un contrato de la clase padre
    override fun initViews() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.btnPause).setOnClickListener {
            gameTimer.pause()
            PauseDialog(this).showDialog(
                onResume = { gameTimer.resume() },
                onExit = { finish() }
            )
        }
    }

    override fun loadGameData() {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val categoryId = when (theme.lowercase()) {
                        "clothing" -> 3
                        "emotions" -> 5
                        "furniure" -> 8
                        else -> 0
                    }
                    val api = RetrofitClient.instance.create(AuthApi::class.java)
                    val call = api.getGameObjects(categoryId).awaitResponse()

                    if (call.isSuccessful) {
                        val allAvailableObjects = call.body() ?: emptyList()
                        if (allAvailableObjects.size < numPairsWanted) return@withContext false

                        val selectedObjects = allAvailableObjects.shuffled().take(numPairsWanted)
                        val memoryCards = mutableListOf<MemoryCard>()
                        selectedObjects.forEach { obj ->
                            memoryCards.add(MemoryCard(gameObject = obj))
                            memoryCards.add(MemoryCard(gameObject = obj))
                        }
                        boardCards = memoryCards.shuffled()
                        gameTimer.start()
                        return@withContext true
                    }
                    false
                } catch (e: Exception) {
                    false
                }
            }

            if (success) {
                createDynamicBoard()
            } else {
                Toast.makeText(this@MemoryGame, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun createDynamicBoard() {
        val grid = findViewById<GridLayout>(R.id.gridMemorama)
        grid.removeAllViews()

        val columns = boardCards.size / 2
        grid.columnCount = columns
        grid.rowCount = 2

        val displayMetrics = resources.displayMetrics
        val maxWidthPerCard = (displayMetrics.widthPixels * 0.95f) / columns
        val maxHeightPerCard = (displayMetrics.heightPixels * 0.70f) / 2f
        val baseSize = min(maxWidthPerCard, maxHeightPerCard).toInt()
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
                tag = false

                Glide.with(this@MemoryGame)
                    .load(RetrofitClient.BASE_URL_IMAGES + cardData.gameObject.imageUrl)
                    .preload()
            }
            view.setOnClickListener { handleCardClick(view, cardData) }
            grid.addView(view)
        }
    }

    private fun handleCardClick(view: ImageView, card: MemoryCard) {
        if (isBusy || view.tag == true || card.isMatched) return

        flipCardUp(view, card.gameObject.imageUrl)
        view.tag = true

        if (firstSelectedCard == null) {
            firstSelectedCard = card
            firstSelectedView = view
        } else {
            isBusy = true
            val isMatch = firstSelectedCard?.gameObject?.id == card.gameObject.id

            if (isMatch) {
                firstSelectedCard?.isMatched = true
                card.isMatched = true

                card.gameObject.audioShortUrl?.let {
                    gameAudio.playUrl(RetrofitClient.BASE_URL_SOUNDS + it)
                } ?: gameAudio.playEffect(R.raw.win)

                resetSelection()
                isBusy = false
                checkGameFinished()
            } else {
                errors++
                gameAudio.playEffect(R.raw.fail)

                lifecycleScope.launch {
                    delay(1000)
                    firstSelectedView?.let { firstView ->
                        flipCardsDown(firstView, view, R.drawable.ic_logo) {
                            view.tag = false
                            firstView.tag = false
                            resetSelection()
                            isBusy = false
                        }
                    } ?: run {
                        resetSelection()
                        isBusy = false
                    }
                }
            }
        }
    }

    private fun flipCardUp(view: ImageView, imageUrl: String) {
        val duration = 150L

        view.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                Glide.with(this)
                    .load(RetrofitClient.BASE_URL_IMAGES + imageUrl)
                    .placeholder(R.drawable.ic_logo)
                    .transform(CenterCrop(), RoundedCorners(30))
                    .into(view)

                view.rotationY = -90f
                view.animate()
                    .rotationY(0f)
                    .setDuration(duration)
                    .start()
            }.start()
    }

    private fun flipCardsDown(view1: ImageView, view2: ImageView, defaultImage: Int, onComplete: () -> Unit) {
        val duration = 150L

        view1.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                view1.setImageResource(defaultImage)
                view1.rotationY = -90f
                view1.animate().rotationY(0f).setDuration(duration).start()
            }.start()

        view2.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                view2.setImageResource(defaultImage)
                view2.rotationY = -90f
                view2.animate()
                    .rotationY(0f)
                    .setDuration(duration)
                    .withEndAction {
                        onComplete()
                    }.start()
            }.start()
    }

    private fun checkGameFinished() {
        if (boardCards.all { it.isMatched }) {
            lifecycleScope.launch {
                delay(1000)

                showResults("Memorama")
            }
        }
    }

    private fun resetSelection() {
        firstSelectedCard = null
        firstSelectedView = null
    }

}
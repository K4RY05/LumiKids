package com.example.lumikids.ui

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.lumikids.R
import com.example.lumikids.model.MemoryCard
import com.example.lumikids.network.GameApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.InstructionRepository
import com.example.lumikids.utils.PauseDialog
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
        setContentView(R.layout.activity_game_memory)
        enableEdgeToEdge()

        numPairsWanted = intent.getIntExtra("NUM_CARDS", 6) / 2

        initViews()
        loadGameData()
    }

    override fun initViews() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnPause).setOnClickListener {
            gameTimer.pause()
            gameAudio.stopNetworkAudio()
            gameAudio.stopLoop() // Detiene la instrucción al pausar

            PauseDialog(this).showDialog(
                onResume = {
                    gameTimer.resume()
                    // Reanuda la instrucción local cada 7 segundos
                    gameAudio.startLocalLoop(R.raw.intruc_memory, 7000L)
                },
                onExit = { finish() }
            )
        }
    }

    override fun loadGameData() {
        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val categoryId = InstructionRepository.getCategoryId(theme)

                    val baseUrl = RetrofitClient.BASE_URL_IMAGES.replace("images/", "")
                    Log.d("MINIGAME_DEBUG", "Pares requeridos: $numPairsWanted")
                    Log.d("MINIGAME_DEBUG", "GET: ${baseUrl}api/games/game-objects/$categoryId")
                    val api = RetrofitClient.instance.create(GameApi::class.java)
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
                    }else {
                        Log.e("MINIGAME_DEBUG", "Error en la respuesta de la API: ${call.code()} - ${call.message()}")
                    }
                    false
                } catch (e: Exception) {
                    Log.e("MINIGAME_DEBUG", "Error al cargar objetos: ${e.message}")
                    false
                }
            }

            if (success) {
                createDynamicBoard()
                gameAudio.startLocalLoop(R.raw.intruc_memory, 7000L)
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

            val imageUrlCompleta = RetrofitClient.BASE_URL_IMAGES + cardData.gameObject.imageUrl

            val view = ImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = size
                    height = size
                    setMargins(margin, margin, margin, margin)
                    setGravity(Gravity.CENTER)
                }
                setBackgroundResource(R.drawable.bg_white_card)
                setPadding(margin, margin, margin, margin)
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(R.drawable.ic_logo)
                tag = false

                Glide.with(this@MemoryGame)
                    .load(imageUrlCompleta)
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

                gameAudio.stopLoop()

                card.gameObject.audioShortUrl?.let {
                    val audioUrl = RetrofitClient.BASE_URL_SOUNDS + it
                    gameAudio.playUrl(audioUrl)
                } ?: gameAudio.playEffect(R.raw.win)

                lifecycleScope.launch {
                    delay(3000)

                    if (!boardCards.all { it.isMatched }) {
                        gameAudio.startLocalLoop(R.raw.intruc_memory, 10000L)
                    }
                }

                resetSelection()
                isBusy = false
                checkGameFinished()
            } else {
                errors++
                gameAudio.playEffect(R.raw.fail)

                lifecycleScope.launch {
                    delay(1500)
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
            gameAudio.stopLoop()

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
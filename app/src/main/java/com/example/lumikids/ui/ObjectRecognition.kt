package com.example.lumikids.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.lumikids.R
import com.example.lumikids.model.GameObject
import com.example.lumikids.model.ObjectRound
import com.example.lumikids.network.GameApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.InstructionRepository
import com.example.lumikids.utils.PauseDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class ObjectRecognition : MiniGame() {

    private var allItems: MutableList<GameObject> = mutableListOf()
    // 1. LISTA DE PENDIENTES: Para controlar qué no ha salido como correcto
    private var availableItems: MutableList<GameObject> = mutableListOf()

    private var currentRoundCount: Int = 0
    private var currentRound: ObjectRound? = null
    private var totalRoundsWanted: Int = 3
    private var urlAudioActual: String? = null

    private lateinit var tvInstruction: TextView
    private lateinit var images: List<ImageView>
    private lateinit var resultIcons: List<ImageView>

    private lateinit var btnPause: ImageView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_n1)

        totalRoundsWanted = intent.getIntExtra("NUM_ROUNDS", 3)

        initViews()
        loadGameData()
    }

    override fun initViews() {
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
            gameTimer.pause()
            gameAudio.stopLoop()

            PauseDialog(this).showDialog(
                onResume = {
                    gameTimer.resume()
                    urlAudioActual?.let { gameAudio.startLoop(it) }
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
                    val instructionMap =
                        InstructionRepository.loadInstructionsByTheme(this@ObjectRecognition, theme)

                    val api = RetrofitClient.instance.create(GameApi::class.java)
                    val call = api.getGameObjects(categoryId).awaitResponse()

                    if (call.isSuccessful) {
                        val downloadedObjects = call.body() ?: emptyList()
                        if (downloadedObjects.isEmpty()) return@withContext false

                        allItems = downloadedObjects.map { item ->
                            val localText = instructionMap[item.name] ?: "Selecciona el objeto"
                            item.copy(instructionText = localText)
                        }.toMutableList()

                        // 2. INICIALIZACIÓN: Llenamos la lista de pendientes con todos los objetos
                        availableItems = allItems.toMutableList()

                        if (allItems.size >= 3) {
                            gameTimer.start()
                            return@withContext true
                        }
                    }
                    false
                } catch (e: Exception) {
                    false
                }
            }

            if (success) {
                startNewRound()
            } else {
                Toast.makeText(this@ObjectRecognition, "Error al cargar datos", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startNewRound() {
        if (currentRoundCount >= totalRoundsWanted) {
            showResults("Identificar Objeto")
            return
        }

        // 3. VALIDACIÓN: Si ya no hay objetos únicos disponibles, terminamos o reiniciamos lista
        if (availableItems.isEmpty() || allItems.size < 3) {
            showResults("Identificar Objeto")
            return
        }

        currentRoundCount++

        // 4. SELECCIÓN ÚNICA: Sacamos el correcto de la lista de pendientes y lo eliminamos
        availableItems.shuffle()
        val correctObject = availableItems.removeAt(0)

        // 5. DISTRACTORES: Tomamos otros 2 de la lista completa (que no sean el correcto actual)
        val distractors = allItems.filter { it.id != correctObject.id }
            .shuffled()
            .take(2)

        // Unimos y mezclamos para la visualización
        val roundOptions = (distractors + correctObject).shuffled()
        currentRound = ObjectRound(roundOptions, correctObject)

        tvInstruction.text = correctObject.instructionText

        if (correctObject.audioInstructionUrl != null) {
            urlAudioActual = RetrofitClient.BASE_URL_SOUNDS + correctObject.audioInstructionUrl
            gameAudio.startLoop(urlAudioActual!!)
        } else {
            urlAudioActual = null
            gameAudio.stopLoop()
        }

        resetImagesBackground()

        roundOptions.forEachIndexed { index, gameObject ->
            images[index].apply {
                Glide.with(this@ObjectRecognition)
                    .load(RetrofitClient.BASE_URL_IMAGES + gameObject.imageUrl)
                    .placeholder(R.drawable.ic_logo)
                    .transform(CenterCrop(), RoundedCorners(30))
                    .into(this)

                isEnabled = true
                setOnClickListener {
                    gameAudio.stopLoop()
                    val isCorrect = (gameObject.id == currentRound?.correctObject?.id)

                    if (gameObject.audioShortUrl != null) {
                        gameAudio.playUrl(RetrofitClient.BASE_URL_SOUNDS + gameObject.audioShortUrl)
                    }
                    handleSelection(this, isCorrect)
                }
            }
        }
    }

    private fun handleSelection(view: ImageView, isCorrect: Boolean) {
        images.forEach { it.isEnabled = false }

        val index = images.indexOf(view)
        if (index != -1 && index < resultIcons.size) {
            val resultIcon = resultIcons[index]

            if (isCorrect) {
                resultIcon.setImageResource(R.drawable.ic_correct)
            } else {
                resultIcon.setImageResource(R.drawable.ic_error)
            }
            resultIcon.visibility = View.VISIBLE

            lifecycleScope.launch {
                delay(500)
                if (isCorrect) {
                    gameAudio.playEffect(R.raw.win)
                    delay(1000)
                    startNewRound()
                } else {
                    errors++
                    gameAudio.playEffect(R.raw.fail)
                    resultIcon.visibility = View.INVISIBLE
                    images.forEach { it.isEnabled = true }
                }
            }
        }
    }

    private fun resetImagesBackground() {
        resultIcons.forEach { it.visibility = View.INVISIBLE }
        images.forEach { it.isEnabled = true }
    }
}
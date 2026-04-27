package com.example.lumikids.ui

import android.os.Bundle
import android.util.Log
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
        setContentView(R.layout.activity_game_object)

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
            gameAudio.stopNetworkAudio()

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

                    val baseUrl = RetrofitClient.BASE_URL_IMAGES.replace("images/", "")
                    Log.d("MINIGAME_DEBUG", "GET: ${baseUrl}api/games/game-objects/$categoryId")

                    val api = RetrofitClient.instance.create(GameApi::class.java)
                    val call = api.getGameObjects(categoryId).awaitResponse()

                    if (call.isSuccessful) {
                        val downloadedObjects = call.body() ?: emptyList()
                        if (downloadedObjects.isEmpty()) return@withContext false

                        allItems = downloadedObjects.map { item ->
                            val localText = instructionMap[item.name]

                            if (localText == null) {
                                Log.w("MINIGAME_DEBUG", "Instrucción visual faltante para: ${item.name}")
                            }

                            item.copy(instructionText = localText ?: "Selecciona el objeto")
                        }.toMutableList()

                        availableItems = allItems.filter { item ->
                            instructionMap.containsKey(item.name) && item.audioInstructionUrl != null
                        }.toMutableList()

                        if (allItems.size >= 3) {
                            gameTimer.start()
                            return@withContext true
                        }
                    }
                    false
                } catch (e: Exception) {
                    Log.e("MINIGAME_DEBUG", "Error al cargar objetos: ${e.message}")
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

        if (availableItems.isEmpty() || allItems.size < 3) {
            showResults("Identificar Objeto")
            return
        }

        currentRoundCount++

        availableItems.shuffle()
        val correctObject = availableItems.removeAt(0)

        val distractors = allItems.filter { it.id != correctObject.id }
            .shuffled()
            .take(2)

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

            // --- LOG: Carga de Imagen ---
            val imageUrlCompleta = RetrofitClient.BASE_URL_IMAGES + gameObject.imageUrl
            Log.d("MINIGAME_DEBUG", "IMG: $imageUrlCompleta")

            images[index].apply {
                Glide.with(this@ObjectRecognition)
                    .load(imageUrlCompleta)
                    .placeholder(R.drawable.ic_logo)
                    .transform(CenterCrop(), RoundedCorners(30))
                    .into(this)

                isEnabled = true
                setOnClickListener {
                    gameAudio.stopLoop()

                    // --- LOG: Selección de Carta ---
                    Log.d("MINIGAME_DEBUG", "Seleccion: ${gameObject.name} Stage: object")

                    val isCorrect = (gameObject.id == currentRound?.correctObject?.id)

                    if (gameObject.audioShortUrl != null) {
                        val audioUrl = RetrofitClient.BASE_URL_SOUNDS + gameObject.audioShortUrl
                        Log.d("MINIGAME_DEBUG", "Audio URL: $audioUrl")
                        gameAudio.playUrl(audioUrl)
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

                    delay(1000)
                    resultIcon.visibility = View.INVISIBLE
                    images.forEach { it.isEnabled = true }

                    urlAudioActual?.let {
                        gameAudio.startLoop(it)
                    }
                }
            }
        }
    }

    private fun resetImagesBackground() {
        resultIcons.forEach { it.visibility = View.INVISIBLE }
        images.forEach { it.isEnabled = true }
    }
}
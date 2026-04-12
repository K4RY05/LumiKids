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
import com.example.lumikids.ui.MiniGame
import com.example.lumikids.model.GameObject
import com.example.lumikids.model.ObjectRound
import com.example.lumikids.network.AuthApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.utils.InstructionRepository
import com.example.lumikids.utils.PauseDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

// ✨ CAMBIO CLAVE: Heredamos de BaseMiniGameActivity
class ObjectRecognition : MiniGame() {

    private var allItems: MutableList<GameObject> = mutableListOf()
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
        // ✨ El super.onCreate ya configura el modo inmersivo, el theme y el gameAudio
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
                    val api = RetrofitClient.instance.create(AuthApi::class.java)
                    val call = api.getGameObjects(categoryId).awaitResponse()

                    if (call.isSuccessful) {
                        val downloadedObjects = call.body() ?: emptyList()
                        if (downloadedObjects.isEmpty()) return@withContext false

                        allItems = downloadedObjects.map { item ->
                            val localText = instructionMap[item.name] ?: "Selecciona el objeto"
                            item.copy(instructionText = localText)
                        }.toMutableList()

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
            // ✨ MEJORA: Usamos la función integrada de la clase padre para mostrar la victoria
            showResults("Identificar Objeto")
            return
        }

        if (allItems.size < 3) return

        currentRoundCount++
        val roundOptions = allItems.shuffled().take(3)
        val correctObject = roundOptions.random()
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
                    val isCorrect = gameObject == currentRound?.correctObject

                    if (gameObject.audioShortUrl != null) {
                        gameAudio.playUrl(RetrofitClient.BASE_URL_SOUNDS + gameObject.audioShortUrl)
                    }
                    handleSelection(this, isCorrect)
                }
            }
        }
    }

    private fun handleSelection(view: ImageView, isCorrect: Boolean) {
        // Deshabilitamos clics para evitar toques múltiples
        images.forEach { it.isEnabled = false }

        val index = images.indexOf(view)
        if (index != -1 && index < resultIcons.size) {
            val resultIcon = resultIcons[index]

            // Mostramos el icono visual de acierto o error
            if (isCorrect) {
                resultIcon.setImageResource(R.drawable.ic_correct)
            } else {
                resultIcon.setImageResource(R.drawable.ic_error)
            }
            resultIcon.visibility = View.VISIBLE

            lifecycleScope.launch {
                delay(500) // Respuesta visual rápida
                if (isCorrect) {
                    gameAudio.playEffect(R.raw.win)
                    delay(1000) // Esperamos antes de la siguiente ronda
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
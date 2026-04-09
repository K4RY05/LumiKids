package com.example.lumikids.minigame.objectrecognition.controller

import android.content.Context
import com.example.lumikids.minigame.utils.InstructionRepository
import com.example.lumikids.minigame.objectrecognition.model.ObjectRound
import com.example.lumikids.model.GameResult
import com.example.lumikids.network.GamesApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.model.GameObject
import com.example.lumikids.minigame.utils.GameTimer // <-- Importamos tu clase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class ObjectRecognitionController(
    private val context: Context,
    private val theme: String,
    private val totalRoundsWanted: Int
) {
    private var allItems: MutableList<GameObject> = mutableListOf()
    private var currentRoundCount: Int = 0
    private var errors: Int = 0
    private var currentRound: ObjectRound? = null

    // ✨ INSTANCIAMOS EL NUEVO TIMER
    private val gameTimer = GameTimer()

    suspend fun cargarDatos(): Boolean = withContext(Dispatchers.IO) {
        try {
            val categoryId = InstructionRepository.getCategoryId(theme)
            val instructionMap = InstructionRepository.loadInstructionsByTheme(context, theme)
            val api = RetrofitClient.instance.create(GamesApi::class.java)

            val call = api.getGameObjects(categoryId).awaitResponse()

            if (call.isSuccessful) {
                val downloadedObjects = call.body() ?: emptyList()

                if (downloadedObjects.isEmpty()) return@withContext false

                allItems = downloadedObjects.map { item ->
                    val localText = instructionMap[item.name] ?: "Selecciona el objeto"
                    item.copy(instructionText = localText)
                }.toMutableList()

                if (allItems.size >= 3) {
                    // ✨ INICIAMOS EL CRONÓMETRO CON LA NUEVA CLASE
                    gameTimer.start()
                    return@withContext true
                }
            }
            return@withContext false
        } catch (e: Exception) {
            return@withContext false
        }
    }

    fun getNewRound(): ObjectRound? {
        if (allItems.size < 3 || isGameOver()) return null
        currentRoundCount++
        val roundOptions = allItems.shuffled().take(3)
        val correctObject = roundOptions.random()
        currentRound = ObjectRound(roundOptions, correctObject)
        return currentRound
    }

    fun checkAnswer(clicked: GameObject): Boolean = clicked == currentRound?.correctObject
    fun addError() { errors++ }
    fun isGameOver(): Boolean = currentRoundCount >= totalRoundsWanted

    fun pauseTimer() {
        gameTimer.pause()
    }

    fun resumeTimer() {
        gameTimer.resume()
    }

    fun getFinalResult(gameTitle: String): GameResult {
        return GameResult(gameTimer.getTotalSeconds(), errors, gameTitle)
    }
}
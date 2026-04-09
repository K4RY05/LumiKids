package com.example.lumikids.minigame.memorygame.controller

import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.model.GameResult
import com.example.lumikids.network.GamesApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.minigame.utils.GameTimer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class MemoryGameController(private val numPairsWanted: Int) {
    var cards: List<MemoryCard> = emptyList()
    private var errors: Int = 0

    private val gameTimer = GameTimer()

    suspend fun cargarDatos(theme: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val categoryId = obtenerIdDeCategoria(theme)
            val api = RetrofitClient.instance.create(GamesApi::class.java)

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

                cards = memoryCards.shuffled()

                // ✨ INICIAMOS EL CRONÓMETRO CON LA NUEVA CLASE
                gameTimer.start()
                return@withContext true
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun isMatch(card1: MemoryCard, card2: MemoryCard): Boolean {
        return if (card1.gameObject.id == card2.gameObject.id) {
            card1.isMatched = true
            card2.isMatched = true
            true
        } else {
            errors++
            false
        }
    }

    fun isGameOver(): Boolean = cards.all { it.isMatched }

    fun pauseTimer() {
        gameTimer.pause()
    }

    fun resumeTimer() {
        gameTimer.resume()
    }

    // ✨ OBTENEMOS EL RESULTADO DIRECTAMENTE DEL TIMER
    fun getFinalResult(gameTitle: String): GameResult {
        return GameResult(gameTimer.getTotalSeconds(), errors, gameTitle)
    }

    private fun obtenerIdDeCategoria(themeName: String): Int {
        return when (themeName.lowercase()) {
            "clothing" -> 3
            "emotions" -> 5
            "furniure" -> 8
            else -> 0
        }
    }
}
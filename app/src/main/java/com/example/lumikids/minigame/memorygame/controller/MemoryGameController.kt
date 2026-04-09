package com.example.lumikids.minigame.memorygame.controller

import android.util.Log
import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.model.GameObject
import com.example.lumikids.model.GameResult
import com.example.lumikids.network.GamesApi
import com.example.lumikids.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

class MemoryGameController(private val numPairsWanted: Int) {
    var cards: List<MemoryCard> = emptyList()
    private var errors: Int = 0
    private var startTime: Long = 0


    suspend fun cargarDatos(theme: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val categoryId = obtenerIdDeCategoria(theme)
            val api = RetrofitClient.instance.create(GamesApi::class.java)

            // Llamada unificada al servidor
            val call = api.getGameObjects(categoryId).awaitResponse()

            if (call.isSuccessful) {
                val allAvailableObjects = call.body() ?: emptyList()

                if (allAvailableObjects.size < numPairsWanted) {
                    Log.e("MemoryController", "Insuficientes objetos")
                    return@withContext false
                }

                // Generamos los pares usando el GameObject universal
                val selectedObjects = allAvailableObjects.shuffled().take(numPairsWanted)
                val memoryCards = mutableListOf<MemoryCard>()

                selectedObjects.forEach { obj ->
                    memoryCards.add(MemoryCard(gameObject = obj))
                    memoryCards.add(MemoryCard(gameObject = obj))
                }

                cards = memoryCards.shuffled()
                startTime = System.currentTimeMillis() // Iniciamos cronómetro
                return@withContext true
            }
            false
        } catch (e: Exception) {
            Log.e("MemoryController", "Error: ${e.message}")
            false
        }
    }

    /**
     * ✨ SOLUCIÓN ERROR 1: Verifica si dos cartas son pareja comparando sus IDs.
     */
    fun isMatch(card1: MemoryCard, card2: MemoryCard): Boolean {
        return if (card1.gameObject.id == card2.gameObject.id) {
            card1.isMatched = true
            card2.isMatched = true
            true
        } else {
            errors++ // Registramos el error si no coinciden
            false
        }
    }

    /**
     * Verifica si todas las parejas han sido encontradas.
     */
    fun isGameOver(): Boolean = cards.all { it.isMatched }

    /**
     * ✨ SOLUCIÓN ERROR 2: Calcula el tiempo total y errores para la pantalla de resultados.
     */
    fun getFinalResult(gameTitle: String): GameResult {
        val totalTime = (System.currentTimeMillis() - startTime) / 1000
        return GameResult(totalTime, errors, gameTitle)
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
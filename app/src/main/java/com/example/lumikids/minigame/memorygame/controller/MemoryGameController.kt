package com.example.lumikids.minigame.memorygame.controller

import android.os.SystemClock // ✨ IMPORTANTE: Importamos el reloj interno del sistema
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

    // ✨ VARIABLES DE TIEMPO OPTIMIZADAS
    private var startTime: Long = 0
    private var totalPausedTime: Long = 0
    private var pauseStart: Long = 0

    suspend fun cargarDatos(theme: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val categoryId = obtenerIdDeCategoria(theme)
            val api = RetrofitClient.instance.create(GamesApi::class.java)

            val call = api.getGameObjects(categoryId).awaitResponse()

            if (call.isSuccessful) {
                val allAvailableObjects = call.body() ?: emptyList()

                if (allAvailableObjects.size < numPairsWanted) {
                    Log.e("MemoryController", "Insuficientes objetos")
                    return@withContext false
                }

                val selectedObjects = allAvailableObjects.shuffled().take(numPairsWanted)
                val memoryCards = mutableListOf<MemoryCard>()

                selectedObjects.forEach { obj ->
                    memoryCards.add(MemoryCard(gameObject = obj))
                    memoryCards.add(MemoryCard(gameObject = obj))
                }

                cards = memoryCards.shuffled()

                // ✨ INICIAMOS EL CRONÓMETRO CON EL RELOJ DEL SISTEMA
                startTime = SystemClock.elapsedRealtime()
                return@withContext true
            }
            false
        } catch (e: Exception) {
            Log.e("MemoryController", "Error: ${e.message}")
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

    // ========================================================
    // ✨ NUEVAS FUNCIONES PARA CONTROLAR LA PAUSA
    // ========================================================

    /**
     * Congela el tiempo. Llama a esta función cuando se abra el PauseDialog.
     */
    fun pauseTimer() {
        // Solo guardamos el inicio de la pausa si el juego ya había empezado
        if (startTime != 0L && pauseStart == 0L) {
            pauseStart = SystemClock.elapsedRealtime()
        }
    }

    /**
     * Reanuda el tiempo. Llama a esta función cuando se cierre el PauseDialog.
     */
    fun resumeTimer() {
        if (pauseStart != 0L) {
            // Calculamos cuánto duró la pausa y lo acumulamos
            totalPausedTime += (SystemClock.elapsedRealtime() - pauseStart)
            pauseStart = 0L // Reseteamos la marca
        }
    }

    /**
     * Calcula el resultado final restando el tiempo muerto.
     */
    fun getFinalResult(gameTitle: String): GameResult {
        val endTime = SystemClock.elapsedRealtime()

        // ✨ FÓRMULA MÁGICA: Tiempo Transcurrido - Tiempo Pausado
        val totalTimeSegundos = (endTime - startTime - totalPausedTime) / 1000

        return GameResult(totalTimeSegundos, errors, gameTitle)
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
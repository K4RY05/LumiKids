package com.example.lumikids.minigame.objectrecognition.controller

import android.content.Context
import android.os.SystemClock // ✨ IMPORTANTE: Importamos el reloj interno del sistema
import android.util.Log
import com.example.lumikids.minigame.core.InstructionRepository
import com.example.lumikids.minigame.objectrecognition.model.ObjectRound
import com.example.lumikids.model.GameResult
import com.example.lumikids.network.GamesApi
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.model.GameObject
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

    // ✨ VARIABLES DE TIEMPO OPTIMIZADAS
    private var startTime: Long = 0
    private var totalPausedTime: Long = 0
    private var pauseStart: Long = 0

    suspend fun cargarDatos(): Boolean = withContext(Dispatchers.IO) {
        try {
            // ✨ OPTIMIZACIÓN: Usamos el ID centralizado del repositorio
            val categoryId = InstructionRepository.getCategoryId(theme)
            Log.d("Controller", "Iniciando carga. Tema: $theme, ID Categoría: $categoryId")

            val instructionMap = InstructionRepository.loadInstructionsByTheme(context, theme)
            val api = RetrofitClient.instance.create(GamesApi::class.java)

            // Llamada a la API unificada
            val call = api.getGameObjects(categoryId).awaitResponse()

            if (call.isSuccessful) {
                val downloadedObjects = call.body() ?: emptyList()

                if (downloadedObjects.isEmpty()) {
                    Log.e("Controller", "El servidor respondió OK pero la lista está VACÍA.")
                    return@withContext false
                }

                // Mapeo con inyección de texto local usando .copy()
                allItems = downloadedObjects.map { item ->
                    val localText = instructionMap[item.name] ?: "Selecciona el objeto"
                    item.copy(instructionText = localText)
                }.toMutableList()

                if (allItems.size >= 3) {
                    // ✨ INICIAMOS EL CRONÓMETRO CON EL RELOJ DEL SISTEMA
                    startTime = SystemClock.elapsedRealtime()
                    return@withContext true
                } else {
                    Log.e("Controller", "No hay suficientes objetos (mínimo 3). Encontrados: ${allItems.size}")
                }
            } else {
                Log.e("Controller", "Error en el servidor. Código: ${call.code()}. Mensaje: ${call.message()}")
            }

            return@withContext false

        } catch (e: Exception) {
            Log.e("Controller", "Error crítico en cargarDatos: ${e.localizedMessage}")
            e.printStackTrace()
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

    // ========================================================
    // ✨ NUEVAS FUNCIONES PARA CONTROLAR LA PAUSA
    // ========================================================

    /**
     * Congela el tiempo. Llama a esta función cuando se abra el PauseDialog.
     */
    fun pauseTimer() {
        if (startTime != 0L && pauseStart == 0L) {
            pauseStart = SystemClock.elapsedRealtime()
        }
    }

    /**
     * Reanuda el tiempo. Llama a esta función cuando se cierre el PauseDialog.
     */
    fun resumeTimer() {
        if (pauseStart != 0L) {
            totalPausedTime += (SystemClock.elapsedRealtime() - pauseStart)
            pauseStart = 0L
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
}
package com.example.lumikids.minigame.objectrecognition.controller

import android.content.Context
import android.util.Log
import com.example.lumikids.minigame.core.InstructionRepository
import com.example.lumikids.minigame.core.FotogramaRepository
import com.example.lumikids.minigame.objectrecognition.model.GameObject
import com.example.lumikids.minigame.objectrecognition.model.ObjectRound
import com.example.lumikids.model.GameResult
import com.example.lumikids.model.SoundResponse
import com.example.lumikids.network.GamesApi
import com.example.lumikids.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse // ✨ Necesario para convertir Call en suspend
import kotlin.coroutines.resume

class ObjectRecognitionController(
    private val context: Context,
    private val theme: String,
    private val totalRoundsWanted: Int
) {
    private val instructionMap = InstructionRepository.loadInstructionsByTheme(context, theme)
    private var allItems: MutableList<GameObject> = mutableListOf()

    var listaDeSonidos: List<SoundResponse> = emptyList()
        private set

    private var currentRoundCount: Int = 0
    private var errors: Int = 0
    private var startTime: Long = 0
    private var currentRound: ObjectRound? = null

    /**
     * Carga imágenes y sonidos de forma secuencial y síncrona dentro de la corrutina.
     */
    suspend fun cargarDatos(): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Cargamos los fotogramas (imágenes)
            val responseItems = suspendCancellableCoroutine { continuation ->
                FotogramaRepository.getFotogramasByTheme(theme) { response ->
                    continuation.resume(response)
                }
            }

            if (responseItems == null || responseItems.isEmpty()) {
                Log.e("Controller", "No se recibieron imágenes para el tema: $theme")
                return@withContext false
            }

            allItems = responseItems.mapNotNull { item ->
                val name = item.nametheme
                val link = item.linktheme
                if (name != null && link != null) {
                    GameObject(
                        name = name,
                        imageUrl = link,
                        instructionText = instructionMap[name] ?: "Selecciona el objeto"
                    )
                } else null
            }.toMutableList()

            // 2. Cargamos los sonidos (Ahora con awaitResponse para esperar el resultado real)
            val categoryId = obtenerIdDeCategoria(theme)
            val api = RetrofitClient.instance.create(GamesApi::class.java)

            val soundCall = api.getSoundsByCategory(categoryId).awaitResponse()

            if (soundCall.isSuccessful) {
                listaDeSonidos = soundCall.body() ?: emptyList()
                Log.d("Controller", "Audios cargados: ${listaDeSonidos.size}")
            } else {
                Log.e("Controller", "Error en el servidor al pedir sonidos: ${soundCall.code()}")
            }

            // Iniciamos cronómetro si hay datos visuales mínimos
            if (allItems.size >= 3) {
                startTime = System.currentTimeMillis()
                return@withContext true
            }

            false
        } catch (e: Exception) {
            Log.e("Controller", "Error fatal en cargarDatos: ${e.message}")
            false
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
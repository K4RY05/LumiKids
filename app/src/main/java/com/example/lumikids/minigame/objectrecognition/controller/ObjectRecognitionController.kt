package com.example.lumikids.minigame.objectrecognition.controller

import android.content.Context
import android.util.Log
import com.example.lumikids.minigame.core.InstructionRepository
import com.example.lumikids.minigame.core.FotogramaRepository
import com.example.lumikids.minigame.objectrecognition.model.GameObject
import com.example.lumikids.minigame.objectrecognition.model.ObjectRound
import com.example.lumikids.model.GameResult
import com.example.lumikids.network.GamesApi
import com.example.lumikids.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import kotlin.coroutines.resume

class ObjectRecognitionController(
    private val context: Context,
    private val theme: String,
    private val totalRoundsWanted: Int
) {
    // ✨ CORRECCIÓN 1: Se eliminó instructionMap de aquí arriba para no bloquear la pantalla
    private var allItems: MutableList<GameObject> = mutableListOf()

    private var currentRoundCount: Int = 0
    private var errors: Int = 0
    private var startTime: Long = 0
    private var currentRound: ObjectRound? = null

    /**
     * Carga imágenes y sonidos de forma secuencial y síncrona dentro de la corrutina (Hilo de fondo).
     */
    suspend fun cargarDatos(): Boolean = withContext(Dispatchers.IO) {
        try {
            // ✨ CORRECCIÓN 1 (Aplicada): Ahora leemos el JSON local en el hilo de fondo para que la pantalla sea ultra fluida
            val instructionMap = InstructionRepository.loadInstructionsByTheme(context, theme)

            // 1. Cargamos las imágenes
            val responseItems = suspendCancellableCoroutine { continuation ->
                FotogramaRepository.getFotogramasByTheme(theme) { response ->
                    // ✨ CORRECCIÓN 2: Seguro de vida. Verifica que el juego no se haya cerrado antes de continuar.
                    if (continuation.isActive) {
                        continuation.resume(response)
                    }
                }
            }

            if (responseItems.isNullOrEmpty()) {
                Log.e("Controller", "No se recibieron imágenes de la base de datos.")
                return@withContext false
            }

            // 2. Cargamos los audios ANTES de armar los objetos
            val categoryId = obtenerIdDeCategoria(theme)
            val api = RetrofitClient.instance.create(GamesApi::class.java)
            val soundCall = api.getSoundsByCategory(categoryId).awaitResponse()

            // Guardamos temporalmente los audios descargados
            val sonidosDescargados = if (soundCall.isSuccessful) soundCall.body() ?: emptyList() else emptyList()

            // Obtenemos el prefijo correcto (ej. "clothing_")
            val prefijo = InstructionRepository.getPrefix(theme)

            // 3. ENSAMBLAJE MAESTRO: Juntamos imagen, texto y los 2 audios en el GameObject
            allItems = responseItems.mapNotNull { item ->
                val name = item.nametheme
                val link = item.linktheme

                if (name != null && link != null) {

                    // Buscamos las URLs exactas en la lista que acabamos de descargar
                    val shortAudio = sonidosDescargados.find { it.namesounds == name }?.linksounds
                    val instructionAudio = sonidosDescargados.find { it.namesounds == "$prefijo$name" }?.linksounds

                    GameObject(
                        name = name,
                        imageUrl = link,
                        instructionText = instructionMap[name] ?: "Selecciona el objeto",
                        audioShortUrl = shortAudio,
                        audioInstructionUrl = instructionAudio
                    )
                } else null
            }.toMutableList()

            if (allItems.size >= 3) {
                startTime = System.currentTimeMillis()
                return@withContext true
            }

            Log.e("Controller", "Se necesitan mínimo 3 objetos para jugar. Objetos actuales: ${allItems.size}")
            false
        } catch (e: Exception) {
            Log.e("Controller", "Error crítico en cargarDatos: ${e.message}")
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
        // Consistencia de mapeo, quitamos uppercase/lowercase
        return when (themeName) {
            "clothing" -> 3
            "emotions" -> 5
            "furniure" -> 8
            else -> 0
        }
    }
}
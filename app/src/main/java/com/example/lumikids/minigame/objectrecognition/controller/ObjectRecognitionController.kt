package com.example.lumikids.minigame.objectrecognition.controller

import android.content.Context
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
    private var startTime: Long = 0
    private var currentRound: ObjectRound? = null

    suspend fun cargarDatos(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Log para verificar qué tema y categoría estamos buscando
            val categoryId = obtenerIdDeCategoria(theme)
            Log.d("Controller", "Iniciando carga. Tema: $theme, ID Categoría: $categoryId")

            val instructionMap = InstructionRepository.loadInstructionsByTheme(context, theme)
            val api = RetrofitClient.instance.create(GamesApi::class.java)

            // Llamada a la API unificada
            val call = api.getGameObjects(categoryId).awaitResponse()

            if (call.isSuccessful) {
                val downloadedObjects = call.body() ?: emptyList()

                if (downloadedObjects.isEmpty()) {
                    Log.e("Controller", "El servidor respondió OK pero la lista está VACÍA. Verifica la base de datos para la categoría $categoryId.")
                    return@withContext false
                }

                Log.d("Controller", "Se recibieron ${downloadedObjects.size} objetos del servidor.")

                // Mapeo con inyección de texto local usando .copy()
                allItems = downloadedObjects.map { item ->
                    val localText = instructionMap[item.name] ?: "Selecciona el objeto"
                    item.copy(instructionText = localText)
                }.toMutableList()

                if (allItems.size >= 3) {
                    startTime = System.currentTimeMillis()
                    return@withContext true
                } else {
                    Log.e("Controller", "No hay suficientes objetos (mínimo 3). Encontrados: ${allItems.size}")
                }
            } else {
                // Log del error específico de HTTP (ej. 404, 500)
                Log.e("Controller", "Error en el servidor. Código: ${call.code()}. Mensaje: ${call.message()}")
            }

            return@withContext false

        } catch (e: Exception) {
            // Captura errores de red (IP incorrecta) o de parseo JSON
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

    fun getFinalResult(gameTitle: String): GameResult {
        val totalTime = (System.currentTimeMillis() - startTime) / 1000
        return GameResult(totalTime, errors, gameTitle)
    }

    private fun obtenerIdDeCategoria(themeName: String): Int {
        // Asegúrate de que el string coincida exactamente con lo enviado desde el Intent
        return when (themeName.lowercase()) {
            "clothing" -> 3
            "emotions" -> 5
            "furniure" -> 8 // Mantenemos el error de dedo para que coincida con tu SQL
            else -> 0
        }
    }
}
package com.example.lumikids.minigame.objectrecognition.controller

import android.content.Context
import com.example.lumikids.minigame.core.InstructionRepository
import com.example.lumikids.minigame.core.FotogramaRepository // ✨ Importante para la DB
import com.example.lumikids.minigame.objectrecognition.model.GameObject
import com.example.lumikids.minigame.objectrecognition.model.ObjectRound
import com.example.lumikids.model.GameResult
import kotlinx.coroutines.suspendCancellableCoroutine // ✨ Necesario para corrutinas
import kotlin.coroutines.resume

class ObjectRecognitionController(
    private val context: Context,
    private val theme: String,
    private val totalRoundsWanted: Int
) {
    private val instructionMap = InstructionRepository.loadInstructionsByTheme(context, theme)
    private var allItems: MutableList<GameObject> = mutableListOf()
    private var currentRoundCount: Int = 0
    private var errors: Int = 0
    private var startTime: Long = 0
    private var currentRound: ObjectRound? = null

    /**
     * Se conecta al repositorio para bajar los JPGs de la base de datos.
     * Incluye protección contra datos nulos (NPE).
     */
    suspend fun cargarDatos(): Boolean = suspendCancellableCoroutine { continuation ->
        FotogramaRepository.getFotogramasByTheme(theme) { response ->
            if (response != null) {
                // ✨ CAMBIO CLAVE: Usamos mapNotNull para descartar datos corruptos o vacíos
                allItems = response.mapNotNull { item ->
                    val name = item.nametheme
                    val link = item.linktheme

                    // Solo creamos el GameObject si el servidor nos envió un nombre y una URL válidos
                    if (name != null && link != null) {
                        GameObject(
                            name = name,
                            imageUrl = link,
                            instructionText = instructionMap[name] ?: "Selecciona el objeto"
                        )
                    } else {
                        null // Si falta el nombre o la imagen, ignoramos este objeto para que no explote
                    }
                }.toMutableList()

                // Verificamos si después de limpiar los nulos nos quedan al menos 3 objetos para jugar
                if (allItems.size >= 3) {
                    startTime = System.currentTimeMillis()
                    continuation.resume(true) // ✅ Datos listos y seguros
                } else {
                    continuation.resume(false) // ❌ No hay suficientes datos válidos
                }
            } else {
                continuation.resume(false) // ❌ Error de red o respuesta vacía
            }
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
}
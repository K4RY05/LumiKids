package com.example.lumikids.minigame.memorygame.controller

import android.content.Context
import com.example.lumikids.minigame.core.FotogramaRepository
import com.example.lumikids.minigame.core.InstructionRepository // ✨ Importamos el repositorio de audios
import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.model.GameResult
import com.example.lumikids.model.SoundResponse // ✨ Importamos el modelo de los sonidos
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MemoryGameController(
    private val context: Context,
    private val theme: String,
    private val numCards: Int
) {

    private var matchedPairs: Int = 0
    private var errors: Int = 0
    private var startTime: Long = 0

    // Almacenamos temporalmente los datos descargados
    private var allItems: List<Pair<String, String>> = emptyList()

    // ✨ NUEVA VARIABLE: Aquí guardaremos los audios cortos (nombres)
    var listaDeSonidos: List<SoundResponse> = emptyList()

    /**
     * ✨ Descargamos imágenes y audios optimizados desde MySQL
     */
    suspend fun cargarDatos(): Boolean = suspendCancellableCoroutine { continuation ->
        // 1. Descargamos las imágenes
        FotogramaRepository.getFotogramasByTheme(theme) { responseImages ->
            if (responseImages != null) {

                // ✨ 2. AQUÍ ESTÁ LA OPTIMIZACIÓN: Le pedimos a Node.js SOLO los "names"
                InstructionRepository.getInstructionsByTheme(theme, "names") { responseSounds ->
                    if (responseSounds != null) {
                        listaDeSonidos = responseSounds
                    }

                    // Preparamos los datos visuales
                    allItems = responseImages.mapNotNull { item ->
                        val name = item.nametheme
                        val link = item.linktheme
                        if (name != null && link != null) {
                            Pair(name, link)
                        } else {
                            null
                        }
                    }

                    val pairsNeeded = numCards / 2
                    // Verificamos si tenemos suficientes imágenes en la DB para el número de cartas pedido
                    if (allItems.size >= pairsNeeded) {
                        startTime = System.currentTimeMillis()
                        continuation.resume(true) // Éxito
                    } else {
                        continuation.resume(false) // Faltan cartas
                    }
                }
            } else {
                continuation.resume(false) // Error de red
            }
        }
    }

    fun generateBoard(): List<MemoryCard> {
        if (allItems.isEmpty()) return emptyList()

        val pairsNeeded = numCards / 2
        val selectedItems = allItems.shuffled().take(pairsNeeded)

        // Duplicamos las cartas para crear los pares y los revolvemos
        val boardItems = (selectedItems + selectedItems).shuffled()

        return boardItems.mapIndexed { index, pair ->
            MemoryCard(
                id = index,
                name = pair.first,
                imageUrl = pair.second
            )
        }
    }

    fun isMatch(card1: MemoryCard, card2: MemoryCard): Boolean {
        val match = card1.imageUrl == card2.imageUrl
        if (match) {
            matchedPairs++
        } else {
            errors++
        }
        return match
    }

    fun isGameOver(): Boolean {
        val totalPairs = numCards / 2
        return matchedPairs >= totalPairs
    }

    fun getFinalResult(gameTitle: String): GameResult {
        val totalSeconds = (System.currentTimeMillis() - startTime) / 1000
        return GameResult(totalSeconds, errors, gameTitle)
    }
}
package com.example.lumikids.minigame.memorygame.controller

import android.content.Context
import com.example.lumikids.minigame.core.FotogramaRepository // ✨ Conectamos a la base de datos
import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.model.GameResult
import kotlinx.coroutines.suspendCancellableCoroutine // ✨ Importante para asincronía
import kotlin.coroutines.resume

class MemoryGameController(
    private val context: Context, // ✨ Agregamos el contexto que pasaste desde la Activity
    private val theme: String,
    private val numCards: Int
) {

    private var matchedPairs: Int = 0
    private var errors: Int = 0
    private var startTime: Long = 0

    // Almacenamos temporalmente los datos descargados
    private var allItems: List<Pair<String, String>> = emptyList()

    /**
     * ✨ Descargamos los datos desde MySQL antes de armar el tablero
     */
    suspend fun cargarDatos(): Boolean = suspendCancellableCoroutine { continuation ->
        FotogramaRepository.getFotogramasByTheme(theme) { response ->
            if (response != null) {
                // Filtramos los nulos y guardamos los pares (Nombre, Link)
                allItems = response.mapNotNull { item ->
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
                    continuation.resume(true)
                } else {
                    continuation.resume(false)
                }
            } else {
                continuation.resume(false)
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
                imageUrl = pair.second // ✨ Asignamos la ruta JPG
            )
        }
    }

    fun isMatch(card1: MemoryCard, card2: MemoryCard): Boolean {
        // ✨ Comparamos usando la URL de la imagen en vez del Int local
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
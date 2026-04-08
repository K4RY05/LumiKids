package com.example.lumikids.minigame.memorygame.controller

import android.content.Context
import android.util.Log
import com.example.lumikids.minigame.core.FotogramaRepository
import com.example.lumikids.minigame.core.InstructionRepository
import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.model.GameResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MemoryGameController(
    private val context: Context,
    private val theme: String,
    private val numCards: Int
) {

    private var matchedPairs: Int = 0
    private var errors: Int = 0
    private var startTime: Long = 0

    private var allItems: List<Triple<String, String, String?>> = emptyList()

    // ✨ CORRECCIÓN: Agregamos withContext(Dispatchers.IO) y un bloque try-catch
    suspend fun cargarDatos(): Boolean = withContext(Dispatchers.IO) {
        try {
            suspendCancellableCoroutine { continuation ->
                // 1. Descargamos las imágenes
                FotogramaRepository.getFotogramasByTheme(theme) { responseImages ->
                    if (responseImages != null) {

                        // 2. Le pedimos a Node.js SOLO los "names" (audios cortos)
                        InstructionRepository.getInstructionsByTheme(theme, "names") { responseSounds ->

                            // ✨ CORRECCIÓN MÁS IMPORTANTE: Seguro contra cierres inesperados
                            if (continuation.isActive) {
                                val sonidosDescargados = responseSounds ?: emptyList()

                                // 3. ENSAMBLAMOS LOS DATOS
                                allItems = responseImages.mapNotNull { item ->
                                    val name = item.nametheme
                                    val link = item.linktheme

                                    if (name != null && link != null) {
                                        val audioUrl = sonidosDescargados.find { it.namesounds == name }?.linksounds
                                        Triple(name, link, audioUrl)
                                    } else {
                                        null
                                    }
                                }

                                val pairsNeeded = numCards / 2
                                if (allItems.size >= pairsNeeded) {
                                    startTime = System.currentTimeMillis()
                                    continuation.resume(true) // Éxito
                                } else {
                                    Log.e("MemoryController", "Faltan cartas para armar pares. Necesarias: $pairsNeeded, Obtenidas: ${allItems.size}")
                                    continuation.resume(false) // Faltan cartas
                                }
                            }
                        }
                    } else {
                        // ✨ Seguro contra cierres si falla la primera petición
                        if (continuation.isActive) {
                            Log.e("MemoryController", "Error de red: No se pudieron descargar las imágenes.")
                            continuation.resume(false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MemoryController", "Error crítico en cargarDatos: ${e.message}")
            false
        }
    }

    fun generateBoard(): List<MemoryCard> {
        if (allItems.isEmpty()) return emptyList()

        // Reiniciamos los contadores por si el juego se reinicia
        matchedPairs = 0
        errors = 0

        val pairsNeeded = numCards / 2
        val selectedItems = allItems.shuffled().take(pairsNeeded)

        // Duplicamos las cartas para crear los pares y los revolvemos
        val boardItems = (selectedItems + selectedItems).shuffled()

        return boardItems.mapIndexed { index, triple ->
            MemoryCard(
                id = index,
                name = triple.first,
                imageUrl = triple.second,
                audioShortUrl = triple.third
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
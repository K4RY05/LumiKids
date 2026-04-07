package com.example.lumikids.minigame.memorygame.controller

import com.example.lumikids.minigame.core.PictogramRepository
import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.model.GameResult

class MemoryGameController(
    private val theme: String, // ✨ Recibe el String directamente
    private val numCards: Int
) {

    private var matchedPairs: Int = 0
    private var errors: Int = 0
    private var startTime: Long = System.currentTimeMillis()

    // ✨ Función síncrona que retorna la lista directamente
    fun generateBoard(): List<MemoryCard> {

        // Llamamos a tu PictogramRepository que ya está adaptado para recibir el String
        val allItems = PictogramRepository.getPictogramsByTheme(theme)

        if (allItems.isEmpty()) return emptyList()

        val pairsNeeded = numCards / 2

        // Evitamos un error si pairsNeeded es mayor que la cantidad de imágenes disponibles
        val safePairsNeeded = minOf(pairsNeeded, allItems.size)
        val selectedItems = allItems.shuffled().take(safePairsNeeded)

        val boardItems = (selectedItems + selectedItems).shuffled()

        return boardItems.mapIndexed { index, pair ->
            MemoryCard(
                id = index,
                name = pair.first,
                imageResId = pair.second // ✨ Usamos imageResId (Int) para recursos locales
            )
        }
    }

    fun isMatch(card1: MemoryCard, card2: MemoryCard): Boolean {
        // Comparamos los identificadores enteros locales
        val match = card1.imageResId == card2.imageResId
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
package com.example.lumikids.minigame.memorygame.controller

import com.example.lumikids.model.GameTheme
import com.example.lumikids.minigame.core.PictogramRepository // Importamos el repositorio central
import com.example.lumikids.minigame.memorygame.model.MemoryCard
import com.example.lumikids.model.GameResult

class MemoryGameController(
    private val theme: GameTheme,
    private val numCards: Int
) {

    // Variables de estado
    private var matchedPairs: Int = 0
    private var errors: Int = 0
    private var startTime: Long = System.currentTimeMillis()

    fun generateBoard(): List<MemoryCard> {

        val allItems = PictogramRepository.getPictogramsByTheme(theme)

        val pairsNeeded = numCards / 2
        val selectedItems = allItems.shuffled().take(pairsNeeded)

        val boardItems = (selectedItems + selectedItems).shuffled()

        return boardItems.mapIndexed { index, pair ->
            MemoryCard(
                id = index,
                name = pair.first,
                imageResId = pair.second
            )
        }
    }

    fun isMatch(card1: MemoryCard, card2: MemoryCard): Boolean {
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
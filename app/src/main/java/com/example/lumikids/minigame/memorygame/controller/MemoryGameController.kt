package com.example.lumikids.minigame.memorygame.controller

import com.example.lumikids.R
import com.example.lumikids.model.GameTheme   // 🔥 IMPORTANTE
import com.example.lumikids.minigame.memorygame.model.MemoryCard

class MemoryGameController(
    private val theme: GameTheme,
    private val numCards: Int
) {

    fun generateBoard(): List<MemoryCard> {

        val prefix = when (theme) {
            GameTheme.FURNITURE -> "furniture_"
            GameTheme.EMOTIONS -> "emo_"
            GameTheme.CLOTHES -> "clother_"
        }

        val allItems = mutableListOf<Pair<String, Int>>()
        val fields = R.drawable::class.java.fields

        for (field in fields) {
            val name = field.name

            if (!name.startsWith(prefix)) continue

            try {
                allItems.add(name to field.getInt(null))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

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
        return card1.imageResId == card2.imageResId
    }
}
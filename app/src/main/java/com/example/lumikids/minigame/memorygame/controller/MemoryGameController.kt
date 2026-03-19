package com.example.lumikids.minigame.memorygame.controller

import com.example.lumikids.R
import com.example.lumikids.minigame.memorygame.model.MemoryCard

class MemoryGameController(private val theme: String, private val numCards: Int) {

    // 1. Genera el tablero listo y revuelto
    fun generateBoard(): List<MemoryCard> {
        val prefix = when (theme) {
            "FOOD" -> "food_"
            "EMOTIONS" -> "emo_"
            "CLOTHES" -> "clother_"
            "FURNITURE" -> "furniture_"
            else -> ""
        }

        // Buscamos todas las imágenes de este tema dinámicamente
        val allItems = mutableListOf<Pair<String, Int>>()
        val fields = R.drawable::class.java.fields

        for (field in fields) {
            val name = field.name
            if (name.startsWith(prefix)) {
                try {
                    allItems.add(Pair(name, field.getInt(null)))
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        // Si el usuario pidió 8 cartas, necesitamos 4 pares
        val pairsNeeded = numCards / 2
        val selectedItems = allItems.shuffled().take(pairsNeeded)

        // Duplicamos y revolvemos todo el mazo
        val boardItems = (selectedItems + selectedItems).shuffled()

        // Convertimos a nuestro Modelo puro
        return boardItems.mapIndexed { index, pair ->
            MemoryCard(
                id = index,
                name = pair.first,
                imageResId = pair.second
            )
        }
    }

    // 2. Regla del juego: ¿Son iguales?
    fun isMatch(card1: MemoryCard, card2: MemoryCard): Boolean {
        return card1.imageResId == card2.imageResId
    }
}
package com.example.lumikids.minigame.memorygame.model

import com.example.lumikids.model.GameObject
data class MemoryCard(
    val gameObject: GameObject,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)
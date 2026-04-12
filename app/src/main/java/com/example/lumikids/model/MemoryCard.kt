package com.example.lumikids.model

data class MemoryCard(
    val gameObject: GameObject,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)
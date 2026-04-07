package com.example.lumikids.minigame.objectrecognition.model

data class GameObject(
    val name: String,
    val imageUrl: String, // ✨ Ahora almacena la URL completa del servidor
    val instructionText: String
)
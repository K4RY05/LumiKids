package com.example.lumikids.minigame.objectrecognition.model


data class GameObject(
    val name: String,
    val imageUrl: String,
    val instructionText: String,
    val audioShortUrl: String?,
    val audioInstructionUrl: String?
)
package com.example.lumikids.model

data class GameObject(
    val id: Int? = null,
    val name: String,
    val imageUrl: String,
    val audioShortUrl: String?,
    val audioInstructionUrl: String?,
    var instructionText: String = ""
)
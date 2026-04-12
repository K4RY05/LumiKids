package com.example.lumikids.minigame.objectrecognition.model
import com.example.lumikids.model.GameObject

data class ObjectRound(
    val options: List<GameObject>,
    val correctObject: GameObject
)
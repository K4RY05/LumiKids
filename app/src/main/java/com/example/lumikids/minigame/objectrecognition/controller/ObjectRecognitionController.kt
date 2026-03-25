package com.example.lumikids.minigame.objectrecognition.controller

import android.content.Context
import com.example.lumikids.R
import com.example.lumikids.model.GameTheme
import com.example.lumikids.minigame.core.InstructionRepository
import com.example.lumikids.minigame.objectrecognition.model.GameObject
import com.example.lumikids.minigame.objectrecognition.model.ObjectRound

class ObjectRecognitionController(
    private val context: Context,
    private val theme: GameTheme
) {

    private val instructionMap =
        InstructionRepository.loadInstructionsByTheme(context, theme.name)

    private var allItems: List<GameObject> = loadItemsFromResources()

    fun getNewRound(): ObjectRound? {
        if (allItems.size < 3) return null

        val roundOptions = allItems.shuffled().take(3)
        val correctObject = roundOptions.random()

        return ObjectRound(roundOptions, correctObject)
    }

    fun isCorrect(clicked: GameObject, correct: GameObject): Boolean {
        return clicked == correct
    }

    private fun loadItemsFromResources(): List<GameObject> {

        val prefix = when (theme) {
            GameTheme.FURNITURE -> "furniture_"
            GameTheme.EMOTIONS -> "emo_"
            GameTheme.CLOTHES -> "clother_"
        }

        val list = mutableListOf<GameObject>()
        val fields = R.drawable::class.java.fields

        for (field in fields) {
            val name = field.name

            if (!name.startsWith(prefix)) continue

            try {
                val imgId = field.getInt(null)
                val instruction =
                    instructionMap[name] ?: "Selecciona el objeto"

                list.add(GameObject(name, imgId, instruction))

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return list
    }
}
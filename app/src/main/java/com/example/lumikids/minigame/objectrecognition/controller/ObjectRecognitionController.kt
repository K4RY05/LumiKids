package com.example.lumikids.minigame.objectrecognition.controller

import android.content.Context
import com.example.lumikids.minigame.core.InstructionRepository
import com.example.lumikids.minigame.core.PictogramRepository // Importamos el nuevo repositorio
import com.example.lumikids.minigame.objectrecognition.model.GameObject
import com.example.lumikids.minigame.objectrecognition.model.ObjectRound
import com.example.lumikids.model.GameResult

class ObjectRecognitionController(
    private val context: Context,
    private val theme: String,
    private val totalRoundsWanted: Int
) {

    private val instructionMap =
        InstructionRepository.loadInstructionsByTheme(context, theme)

    private var allItems: List<GameObject> = loadItemsFromResources()

    private var currentRoundCount: Int = 0
    private var errors: Int = 0
    private var startTime: Long = System.currentTimeMillis()
    private var currentRound: ObjectRound? = null

    fun getNewRound(): ObjectRound? {
        if (allItems.size < 3 || isGameOver()) return null

        currentRoundCount++

        val roundOptions = allItems.shuffled().take(3)
        val correctObject = roundOptions.random()

        currentRound = ObjectRound(roundOptions, correctObject)
        return currentRound
    }

    fun checkAnswer(clicked: GameObject): Boolean {
        return clicked == currentRound?.correctObject
    }

    fun addError() {
        errors++
    }

    fun isGameOver(): Boolean {
        return currentRoundCount >= totalRoundsWanted
    }

    fun getFinalResult(gameTitle: String): GameResult {
        val totalTime = (System.currentTimeMillis() - startTime) / 1000
        return GameResult(totalTime, errors, gameTitle)
    }

    private fun loadItemsFromResources(): List<GameObject> {
        val rawPictograms = PictogramRepository.getPictogramsByTheme(theme)

        return rawPictograms.map { pair ->
            val name = pair.first
            val imgId = pair.second
            val instruction = instructionMap[name] ?: "Selecciona el objeto"

            GameObject(name, imgId, instruction)
        }
    }
}
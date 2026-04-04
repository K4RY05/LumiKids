package com.example.lumikids.minigame.core

import com.example.lumikids.R
import com.example.lumikids.model.GameTheme

object PictogramRepository {

    fun getPictogramsByTheme(theme: GameTheme): List<Pair<String, Int>> {

        val prefix = when (theme) {
            GameTheme.FURNITURE -> "furniture_"
            GameTheme.EMOTIONS -> "emo_"
            GameTheme.CLOTHES -> "clother_"
        }

        val pictograms = mutableListOf<Pair<String, Int>>()
        val fields = R.drawable::class.java.fields

        for (field in fields) {
            val name = field.name
            if (!name.startsWith(prefix)) continue

            try {
                val imgId = field.getInt(null)
                pictograms.add(name to imgId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return pictograms
    }
}
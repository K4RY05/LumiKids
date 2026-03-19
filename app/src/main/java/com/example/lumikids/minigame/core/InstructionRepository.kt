package com.example.lumikids.minigame.core

import android.content.Context
import org.json.JSONObject

object InstructionRepository {

    fun loadInstructionsByTheme(context: Context, theme: String): Map<String, String> {

        val prefix = when (theme) {
            // "FOOD" -> "food_"
            "EMOTIONS" -> "emo_"
            "CLOTHES" -> "clother_"
            "FURNITURE" -> "furniture_"
            else -> ""
        }

        val jsonString = context.assets.open("instructions.json")
            .bufferedReader()
            .use { it.readText() }

        val jsonObject = JSONObject(jsonString)

        val map = mutableMapOf<String, String>()

        val keys = jsonObject.keys()

        while (keys.hasNext()) {

            val key = keys.next()

            if (key.startsWith(prefix)) {
                map[key] = jsonObject.getString(key)
            }
        }

        return map
    }
}
package com.example.lumikids.utils

import android.content.Context
import org.json.JSONObject
import java.nio.charset.Charset

object InstructionRepository {


    fun getPrefix(theme: String): String {
        return when (theme.lowercase()) {
            "furniure" -> "furniure_"
            "clothing" -> "clothing_"
            "emotions" -> "emotions_"
            else -> ""
        }
    }


    fun getCategoryId(theme: String): Int {
        return when (theme.lowercase()) {
            "clothing" -> 3
            "emotions" -> 5
            "furniure" -> 8
            else -> 0
        }
    }


    fun loadInstructionsByTheme(context: Context, themeName: String): Map<String, String> {
        val instructionsMap = mutableMapOf<String, String>()

        try {
            val jsonString = context.assets.open("instructions.json")
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }

            val jsonObject = JSONObject(jsonString)
            val prefix = getPrefix(themeName)

            if (prefix.isEmpty()) return instructionsMap

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                // Si la llave empieza con 'clothing_', guardamos el texto para la clave limpia 'pants'
                if (key.startsWith(prefix)) {
                    val cleanKey = key.removePrefix(prefix)
                    instructionsMap[cleanKey] = jsonObject.getString(key)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return instructionsMap
    }
}
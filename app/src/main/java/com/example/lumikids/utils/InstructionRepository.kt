package com.example.lumikids.utils

import android.content.Context
import org.json.JSONObject
import java.nio.charset.Charset

object InstructionRepository {

    /**
     * Retorna el prefijo usado en la base de datos para los sonidos de instrucción.
     * Esto ayuda al servidor y a la app a encontrar audios como 'clothing_pants.mp3'.
     */
    fun getPrefix(theme: String): String {
        return when (theme.lowercase()) {
            "furniture" -> "furniture_"
            "clothing" -> "clothing_"
            "emotions" -> "emotions_"
            else -> ""
        }
    }

    /**
     * Mapeo centralizado de IDs de categoría.
     * Mantener esto aquí asegura que todos los minijuegos usen los mismos IDs que el SQL.
     */
    fun getCategoryId(theme: String): Int {
        return when (theme.lowercase()) {
            "clothing" -> 3
            "emotions" -> 5
            "furniture" -> 8
            else -> 0
        }
    }

    /**
     * Carga los textos desde 'assets/instructions.json'.
     * Útil para mostrar "Toca el pantalón" basado en el nombre del objeto.
     */
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
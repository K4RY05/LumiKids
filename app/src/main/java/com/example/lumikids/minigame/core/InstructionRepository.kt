package com.example.lumikids.minigame.core

import android.content.Context
import org.json.JSONObject
import java.nio.charset.Charset

object InstructionRepository {

    fun loadInstructionsByTheme(context: Context, themeName: String): Map<String, String> {
        val instructionsMap = mutableMapOf<String, String>()

        try {
            // ✨ OPTIMIZACIÓN: bufferedReader().use lee el archivo y lo cierra automáticamente,
            // evitando fugas de memoria en el celular.
            val jsonString = context.assets.open("instructions.json")
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }

            val jsonObject = JSONObject(jsonString)

            // Contemplamos ambas formas de escribir "furniture" por seguridad
            val prefix = when (themeName.uppercase()) {
                "FURNITURE", "FURNIURE" -> "furniture_"
                "EMOTIONS" -> "emo_"
                "CLOTHES" -> "clother_"
                else -> ""
            }

            if (prefix.isEmpty()) return instructionsMap

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()

                if (key.startsWith(prefix)) {
                    // ✨ CORRECCIÓN CRÍTICA MANTENIDA: Quitamos el prefijo para que coincida con MySQL
                    // Ejemplo: "furniture_chair" del JSON se convierte en "chair"
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
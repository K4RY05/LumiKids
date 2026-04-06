package com.example.lumikids.minigame.core

import com.example.lumikids.R
// 🔥 Se eliminó la importación de GameTheme

object PictogramRepository {

    // ✨ Ahora recibe un String directamente
    fun getPictogramsByTheme(theme: String): List<Pair<String, Int>> {

        // ✨ Comparamos el texto (ignorando mayúsculas/minúsculas)
        // Agregamos "FURNIURE" para soportar el valor que manda el servidor y la vista
        val prefix = when (theme.uppercase()) {
            "FURNITURE", "FURNIURE" -> "furniture_"
            "EMOTIONS" -> "emo_"
            "CLOTHES" -> "clother_"
            else -> ""
        }

        // Si mandan un tema que no existe, devolvemos una lista vacía para evitar crasheos
        if (prefix.isEmpty()) return emptyList()

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
package com.example.lumikids.minigame.core

import android.content.Context
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.Charset

object InstructionRepository {

    /**
     * Carga las instrucciones desde el archivo assets/instructions.json
     * y filtra solo las que pertenecen a la temática (tema) seleccionada.
     */
    fun loadInstructionsByTheme(context: Context, themeName: String): Map<String, String> {
        val instructionsMap = mutableMapOf<String, String>()

        try {
            // 1. Abrir y leer el archivo JSON desde la carpeta assets
            val inputStream: InputStream = context.assets.open("instructions.json")
            val size: Int = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            val jsonString = String(buffer, Charset.forName("UTF-8"))
            val jsonObject = JSONObject(jsonString)

            // 2. Definir el prefijo según el tema
            // ✨ CORRECCIÓN: Se añade "FURNIURE" para soportar el texto exacto que enviamos desde las vistas
            val prefix = when (themeName.uppercase()) {
                "FURNITURE", "FURNIURE" -> "furniture_"
                "EMOTIONS" -> "emo_"
                "CLOTHES" -> "clother_"
                else -> ""
            }

            // Si el prefijo está vacío, evitamos cargar todo el JSON por accidente
            if (prefix.isEmpty()) return instructionsMap

            // 3. Recorrer todas las llaves del JSON
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()

                // Solo agregamos las que coinciden con la temática actual
                if (key.startsWith(prefix)) {
                    instructionsMap[key] = jsonObject.getString(key)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return instructionsMap
    }
}
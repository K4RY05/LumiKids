package com.example.lumikids.minigame.core

import android.content.Context
import com.example.lumikids.model.SoundResponse
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.network.GamesApi // ✨ IMPORTANTE: Asegúrate de importar tu interfaz aquí
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.Charset

object InstructionRepository {

    // =========================================================================
    // ✨ FUNCIÓN NUEVA OPTIMIZADA: Descarga los audios desde tu servidor
    // Acepta un 'filter' para decirle a Node.js si solo queremos los nombres.
    // =========================================================================
    fun getInstructionsByTheme(
        themeName: String,
        filter: String? = null, // ✨ Recibe el filtro ("names" o null)
        onResult: (List<SoundResponse>?) -> Unit
    ) {

        val categoryId = when (themeName) {
            "furniture" -> 1
            "clothing" -> 2
            "emotions" -> 3
            else -> 1
        }

        // ✨ CORRECCIÓN AQUÍ: Fabricamos la conexión al vuelo sin tocar RetrofitClient
        RetrofitClient.instance.create(GamesApi::class.java)
            .getSoundsByCategory(categoryId, filter)
            .enqueue(object : Callback<List<SoundResponse>> {

                override fun onResponse(
                    call: Call<List<SoundResponse>>,
                    response: Response<List<SoundResponse>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        onResult(response.body()) // Entregamos la lista optimizada
                    } else {
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<List<SoundResponse>>, t: Throwable) {
                    t.printStackTrace()
                    onResult(null) // Error de red
                }
            })
    }


    fun loadInstructionsByTheme(context: Context, themeName: String): Map<String, String> {
        val instructionsMap = mutableMapOf<String, String>()

        try {

            val jsonString = context.assets.open("instructions.json")
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }

            val jsonObject = JSONObject(jsonString)

            // Contemplamos ambas formas de escribir "furniture" por seguridad
            val prefix = when (themeName.uppercase()) {
                "FURNITURE", "FURNIURE" -> "furniure_"
                "EMOTIONS" -> "emotions_"
                "CLOTHING", "CLOTHES" -> "clothing_"
                else -> ""
            }

            if (prefix.isEmpty()) return instructionsMap

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()

                if (key.startsWith(prefix)) {
                    // ✨ CORRECCIÓN CRÍTICA MANTENIDA: Quitamos el prefijo para que coincida con MySQL
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
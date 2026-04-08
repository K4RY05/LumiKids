package com.example.lumikids.minigame.core

import android.content.Context
import com.example.lumikids.model.SoundResponse
import com.example.lumikids.network.RetrofitClient
import com.example.lumikids.network.GamesApi
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.nio.charset.Charset

object InstructionRepository {

    fun getPrefix(theme: String): String {
        return when (theme) {
            "furniure" -> "furniure_"
            "clothing" -> "clothing_"
            "emotions" -> "emotions_"
            else -> ""
        }
    }

    /**
     * Descarga los audios desde el servidor (MySQL)
     */
    fun getInstructionsByTheme(
        themeName: String,
        filter: String? = null,
        onResult: (List<SoundResponse>?) -> Unit
    ) {
        val categoryId = when (themeName) {
            "furniure" -> 8
            "clothing" -> 3
            "emotions" -> 5
            else -> 1
        }

        RetrofitClient.instance.create(GamesApi::class.java)
            .getSoundsByCategory(categoryId, filter)
            .enqueue(object : Callback<List<SoundResponse>> {

                override fun onResponse(
                    call: Call<List<SoundResponse>>,
                    response: Response<List<SoundResponse>>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        onResult(response.body())
                    } else {
                        onResult(null)
                    }
                }

                override fun onFailure(call: Call<List<SoundResponse>>, t: Throwable) {
                    onResult(null)
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

            val prefix = getPrefix(themeName)

            if (prefix.isEmpty()) return instructionsMap

            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
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
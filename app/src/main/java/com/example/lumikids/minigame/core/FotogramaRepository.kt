package com.example.lumikids.minigame.core

import com.example.lumikids.model.ThemeResponse
import com.example.lumikids.network.GamesApi // ✨ Usamos la interfaz GamesApi corregida
import com.example.lumikids.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object FotogramaRepository {

    private val api = RetrofitClient.instance.create(GamesApi::class.java)


    fun getFotogramasByTheme(themeName: String, onResult: (List<ThemeResponse>?) -> Unit) {

        val categoryId = when (themeName.uppercase()) {
            "FURNIURE" -> 8  // ID 8 en tu tabla category
            "EMOTIONS" -> 5              // ID 5 en tu tabla category
            "CLOTHES" -> 3               // ID 3 en tu tabla category
            else -> -1
        }

        if (categoryId == -1) {
            onResult(null)
            return
        }

        // 2. Realizamos la llamada a la API
        api.getThemesByCategory(categoryId).enqueue(object : Callback<List<ThemeResponse>> {
            override fun onResponse(
                call: Call<List<ThemeResponse>>,
                response: Response<List<ThemeResponse>>
            ) {
                if (response.isSuccessful) {
                    onResult(response.body()) // Devolvemos la lista de fotogramas
                } else {
                    onResult(null)
                }
            }

            override fun onFailure(call: Call<List<ThemeResponse>>, t: Throwable) {
                t.printStackTrace()
                onResult(null)
            }
        })
    }
}
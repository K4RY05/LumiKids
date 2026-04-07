package com.example.lumikids.network

import com.example.lumikids.model.ThemeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GamesApi {

    @GET("api/games/themes/{id_category}")
    fun getThemesByCategory(
        @Path("id_category") categoryId: Int
    ): Call<List<ThemeResponse>>
}
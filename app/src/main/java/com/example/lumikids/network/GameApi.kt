package com.example.lumikids.network

import com.example.lumikids.model.ThemeResponse
import com.example.lumikids.model.SoundResponse
import com.example.lumikids.model.GameObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GamesApi {

    @GET("api/games/themes/{id_category}")
    fun getThemesByCategory(
        @Path("id_category") categoryId: Int
    ): Call<List<ThemeResponse>>

    @GET("api/games/sounds/{id_category}")
    fun getSoundsByCategory(
        @Path("id_category") categoryId: Int,
        @Query("filter") filter: String? = null
    ): Call<List<SoundResponse>>

    // ✨ ESTA ES LA NUEVA FUNCIÓN QUE TE ESTÁ PIDIENDO EL CONTROLADOR ✨
    @GET("api/games/game-objects/{id_category}")
    fun getGameObjects(
        @Path("id_category") categoryId: Int
    ): Call<List<GameObject>>
}
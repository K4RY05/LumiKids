package com.example.lumikids.network

import com.example.lumikids.model.GameObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
interface GameApi {

    @GET("api/games/game-objects/{categoryId}")
    fun getGameObjects(
        @Path("categoryId") categoryId: Int,
        @Query("juego") forGame: Boolean = true
    ): Call<List<GameObject>>
}
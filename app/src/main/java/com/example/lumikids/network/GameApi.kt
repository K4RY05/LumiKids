package com.example.lumikids.network

import com.example.lumikids.model.GameObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface GameApi {

    @GET("api/games/game-objects/{id_category}")
    fun getGameObjects(
        @Path("id_category") categoryId: Int
    ): Call<List<GameObject>>
}
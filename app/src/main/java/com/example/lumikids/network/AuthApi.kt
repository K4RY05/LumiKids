package com.example.lumikids.network

import com.example.lumikids.model.ApiResponse
import com.example.lumikids.model.GameObject
import com.example.lumikids.model.LoginRequest
import com.example.lumikids.model.RegisterRequest
import com.example.lumikids.model.UpdateProfileRequest
import retrofit2.Response
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path


interface AuthApi {

    @POST("api/auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<ApiResponse>

    @POST("api/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<ApiResponse>
    @PUT("api/auth/update")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse>

    @GET("api/games/game-objects/{id_category}")
    fun getGameObjects(
        @Path("id_category") categoryId: Int
    ): Call<List<GameObject>>
}
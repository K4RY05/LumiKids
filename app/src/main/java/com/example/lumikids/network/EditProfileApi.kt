package com.example.lumikids.network

import com.example.lumikids.model.ApiResponse
import com.example.lumikids.model.UpdateProfileRequest
import com.example.lumikids.model.UserProfileResponse // 👈 Importa tu nuevo modelo
import retrofit2.Response
import retrofit2.http.*

interface EditProfileApi {

    @GET("api/auth/user/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): Response<UserProfileResponse>

    @PUT("api/auth/update")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse>

    @DELETE("api/auth/delete/{id}")
    suspend fun deleteAccount(@Path("id") userId: String): Response<ApiResponse>
}
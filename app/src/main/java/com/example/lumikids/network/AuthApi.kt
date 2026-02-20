package com.example.lumikids.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class ApiResponse(
    val message: String
)

interface AuthApi {

    @POST("api/auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<ApiResponse>
}


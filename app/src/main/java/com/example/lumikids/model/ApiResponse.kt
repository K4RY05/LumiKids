package com.example.lumikids.model

import com.google.gson.annotations.SerializedName

data class ApiResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("ID_user")
    val userId: String? = null
)
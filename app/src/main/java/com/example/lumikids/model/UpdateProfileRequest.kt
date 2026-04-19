package com.example.lumikids.model

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    val userId: String,
    val name: String,
    val email: String,
    val currentPassword: String,
    val newPassword: String?
)
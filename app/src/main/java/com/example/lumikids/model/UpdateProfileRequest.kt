package com.example.lumikids.model


data class UpdateProfileRequest(
    val userId: String,
    val name: String,
    val email: String,
    val currentPassword: String,
    val newPassword: String?
)
package com.example.lumikids.model

data class UpdateProfileRequest(
    val ID_user: String,
    val name: String,
    val email: String,
    val currentPassword: String,
    val newPassword: String?
)
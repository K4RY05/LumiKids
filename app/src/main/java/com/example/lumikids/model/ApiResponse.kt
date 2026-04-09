package com.example.lumikids.model

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val ID_user: String?,
    val name: String?,
    val email: String?
)
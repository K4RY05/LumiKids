package com.example.lumikids.model

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val userId: String?,
    val name: String?,
    val email: String?
)
package com.example.lumikids.model

data class RegisterRequest(
    val ID_user: String,
    val name: String,
    val email: String,
    val password: String
)


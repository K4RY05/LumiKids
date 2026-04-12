package com.example.lumikids.model

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("userId")
    val ID_user: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("currentPassword")
    val currentPassword: String,

    @SerializedName("newPassword")
    val newPassword: String?
)
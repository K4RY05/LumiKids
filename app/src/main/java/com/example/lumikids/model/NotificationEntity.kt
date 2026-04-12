package com.example.lumikids.model

import com.google.gson.annotations.SerializedName

data class NotificationEntity(
    @SerializedName("ID_notification")
    var ID_notification: Int? = null, //
    @SerializedName("ID_user")
    val ID_user: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("time")
    val time: String,
    var isExpanded: Boolean = false
)
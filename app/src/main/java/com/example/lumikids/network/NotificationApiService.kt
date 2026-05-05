package com.example.lumikids.network

import com.example.lumikids.model.NotificationEntity
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface NotificationApiService {

    @GET("api/notifications/{user}")
    suspend fun getNotifications(@Path("user") userId: String): List<NotificationEntity>

    @POST("api/notifications/add")
    suspend fun addNotification(@Body notification: NotificationEntity): Response<ResponseBody>

    @DELETE("api/notifications/delete/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): Response<ResponseBody>

    @PUT("api/notifications/update/{id}")
    suspend fun updateNotification(
        @Path("id") id: Int,
        @Body notification: NotificationEntity
    ): Response<ResponseBody>
}
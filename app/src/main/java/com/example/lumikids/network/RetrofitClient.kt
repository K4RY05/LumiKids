package com.example.lumikids.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://10.100.244.104:3000/" // My home Carlos

    //private const val BASE_URL = "http://192.168.100.251:3000/" // My home Carlos
    //private const val BASE_URL = "http://10.100.77.90:3000/"   //School
    //private const val BASE_URL = "http://10.106.250.40:3000/" // Phone

    // private const val BASE_URL = "http://192.168.208.104:3000/" // Phone 2
    // private const val BASE_URL = "http://192.168.100.251:3000/" // My home Carlos

    //    private const val BASE_URL = "http://192.168.100.132:3000/" // My home
    const val BASE_URL_IMAGES = "${BASE_URL}images/"
    const val BASE_URL_SOUNDS = "${BASE_URL}sounds/"
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


}

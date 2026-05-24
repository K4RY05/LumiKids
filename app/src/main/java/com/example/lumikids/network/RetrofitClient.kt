package com.example.lumikids.network

import com.example.lumikids.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    //const val BASE_URL= "http://10.122.146.40:3000/"
    //const val BASE_URL= "http://192.168.100.132:3000/"

    const val BASE_URL= "http://10.24.128.40:3000/"
    const val BASE_URL_IMAGES = "${BASE_URL}images/"
    const val BASE_URL_SOUNDS = "${BASE_URL}sounds/"

    @Volatile private var _client: OkHttpClient? = null
    @Volatile private var _retrofit: Retrofit?   = null

    val httpClient: OkHttpClient
        get() = getClient()

    val instance: Retrofit
        get() = _retrofit ?: synchronized(this) {
            _retrofit ?: Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .also { _retrofit = it }
        }

    private fun getClient(): OkHttpClient {
        return _client ?: synchronized(this) {
            _client ?: OkHttpClient.Builder()
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
                .build()
                .also { _client = it }
        }
    }
}
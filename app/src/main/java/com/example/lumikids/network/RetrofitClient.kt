package com.example.lumikids.network

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitClient {

    const val BASE_URL = "http://192.168.100.13:3000/"

    const val BASE_URL_IMAGES = "${BASE_URL}images/"
    const val BASE_URL_SOUNDS = "${BASE_URL}sounds/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var _client: OkHttpClient? = null

    fun getClient(context: Context): OkHttpClient {
        if (_client != null) return _client!!

        val cacheDir = File(context.cacheDir, "http_cache")
        val cacheSize = 30L * 1024 * 1024 // Aumentado a 30MB

        _client = OkHttpClient.Builder()
            .cache(Cache(cacheDir, cacheSize))
            .connectTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val url = chain.request().url.toString()

                // CORRECCIÓN: Aplicar caché a CUALQUIER imagen (ya sea /images/ o /pecs/img/)[cite: 2]
                if (url.contains("/images/") || url.contains("/api/pecs/img/")) {
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=604800") // 7 días[cite: 2]
                        .removeHeader("Pragma")
                        .build()
                } else {
                    response
                }
            }
            .build()

        return _client!!
    }

    fun getInstance(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Actualizado para usar el cliente con interceptor por defecto
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
    package com.example.lumikids.network

    import android.content.Context
    import okhttp3.Cache
    import okhttp3.OkHttpClient
    import okhttp3.logging.HttpLoggingInterceptor
    import retrofit2.Retrofit
    import retrofit2.converter.gson.GsonConverterFactory
    import java.io.File

    object RetrofitClient {

        const val BASE_URL        = "http://192.168.100.132:3000/"

        const val BASE_URL_IMAGES = "${BASE_URL}img/"
        const val BASE_URL_SOUNDS = "${BASE_URL}sounds/"

        private val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // OkHttpClient con caché — se inicializa una sola vez con contexto
        private var _client: OkHttpClient? = null

        fun getClient(context: Context): OkHttpClient {
            if (_client != null) return _client!!

            val cacheDir  = File(context.cacheDir, "http_cache")
            val cacheSize = 20L * 1024 * 1024   // 20 MB para imágenes en disco

            _client = OkHttpClient.Builder()
                .cache(Cache(cacheDir, cacheSize))
                .addInterceptor(logging)
                .addNetworkInterceptor { chain ->
                    // Si el servidor no manda Cache-Control, lo forzamos nosotros
                    val response = chain.proceed(chain.request())
                    val url = chain.request().url.toString()

                    // Solo cachear imágenes, no las APIs dinámicas
                    if (url.contains("/api/pecs/img/")) {
                        response.newBuilder()
                            .header("Cache-Control", "public, max-age=604800") // 7 días
                            .build()
                    } else {
                        response
                    }
                }
                .build()

            return _client!!
        }

        // Retrofit sigue igual pero usa el cliente con caché
        fun getInstance(context: Context): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getClient(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        // Mantener el lazy solo si hay código legacy que lo usa
        val instance: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }
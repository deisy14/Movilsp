package com.example.movilmanupuladora.data.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://backend-sirae-pyim.onrender.com/api/"

    // Variable en memoria para autorización con Bearer JWT
    var authToken: String? = null

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        // No adjuntar token en login o registro inicial
        val isAuthEndpoint = request.url.encodedPath.contains("auth/login") ||
                (request.url.encodedPath.contains("usuarios") && request.method == "POST")

        if (!isAuthEndpoint && !authToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $authToken")
        }

        chain.proceed(requestBuilder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

package com.example.movilmanupuladora.data.api

import android.content.Context
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL =
        "https://backend-sirae-pyim.onrender.com/api/"

    private const val PREFS_NAME = "user_prefs"
    private const val TOKEN_KEY = "auth_token"

    @Volatile
    var authToken: String? = null

    /**
     * Actualiza el token JWT en memoria.
     */
    fun establecerToken(token: String?) {
        authToken = token
    }

    /**
     * Guarda el token JWT recibido del backend.
     */
    fun guardarToken(context: Context, token: String) {
        authToken = token

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }

    /**
     * Recupera el token guardado al iniciar la aplicación.
     */
    fun cargarToken(context: Context): String? {
        val token = context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(TOKEN_KEY, null)

        authToken = token
        return token
    }

    /**
     * Elimina el token al cerrar sesión.
     */
    fun cerrarSesion(context: Context) {
        authToken = null

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .remove(TOKEN_KEY)
            .apply()
    }

    /**
     * Configura el registro de las peticiones HTTP.
     */
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Agrega el token Bearer a las peticiones protegidas.
     */
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        val path = request.url.encodedPath

        val isAuthEndpoint =
            path.contains("/auth/") ||
                    (
                            path.contains("/usuarios/") &&
                                    request.method == "POST"
                            )

        // Recupera el token guardado si aún no está en memoria.
        val token = authToken
            ?: run {
                null
            }

        if (!isAuthEndpoint && !token.isNullOrBlank()) {
            requestBuilder.header(
                "Authorization",
                "Bearer $token"
            )
        }

        chain.proceed(requestBuilder.build())
    }

    /**
     * Cliente HTTP con tiempos de espera y autenticación.
     */
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .build()

    /**
     * Servicio Retrofit de la API.
     */
    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

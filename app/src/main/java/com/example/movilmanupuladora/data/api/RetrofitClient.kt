
package com.example.movilmanupuladora.data.api

import android.content.Context
import com.example.movilmanupuladora.SiraeApp
import com.example.movilmanupuladora.utils.SessionManager
import java.util.concurrent.TimeUnit
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://backend-sirae-pyim.onrender.com/api/"

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
        level = HttpLoggingInterceptor.Level.BASIC
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
                    path.contains("/verify-face") ||
                    (
                            path.contains("/usuarios/") &&
                                    request.method == "POST"
                            )

        if (!isAuthEndpoint) {
            val token = authToken ?: try {
                SessionManager(SiraeApp.instance).fetchAuthToken()
            } catch (_: Throwable) { null }

            if (!token.isNullOrEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        chain.proceed(requestBuilder.build())
    }

    /**
     * Cliente HTTP con tiempos de espera holgados para soportar arranque en frío de Render y autenticación.
     */
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .build()

    /**
     * Gson configurado con soporte para respuestas paginadas de Django REST Framework ({ results: [...] })
     * y deserializador tolerante a esquemas para Usuario.
     */
    val gson = GsonBuilder()
        .registerTypeAdapterFactory(PaginatedListTypeAdapterFactory())
        .registerTypeAdapter(com.example.movilmanupuladora.data.model.Usuario::class.java, com.example.movilmanupuladora.data.model.UsuarioDeserializer())
        .registerTypeAdapter(com.example.movilmanupuladora.data.model.UsuarioInfo::class.java, com.example.movilmanupuladora.data.model.UsuarioInfoDeserializer())
        .create()

    /**
     * Servicio Retrofit de la API.
     */
    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)

    /**
     * Envía una petición GET ligera en segundo plano para precalentar Render
     * y despertar el contenedor antes de que el usuario presione ingresar.
     */
    fun despertarBackend() {
        try {
            val request = okhttp3.Request.Builder()
                .url(BASE_URL)
                .get()
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    // Silencioso: solo nos interesa que la petición llegue al balanceador de Render
                }
                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.close()
                }
            })
        } catch (_: Throwable) { }
    }
}

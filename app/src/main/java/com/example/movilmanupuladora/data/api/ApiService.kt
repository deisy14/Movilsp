package com.example.movilmanupuladora.data.api

import com.example.movilmanupuladora.data.model.LoginRequest
import com.example.movilmanupuladora.data.model.LoginResponse
import com.example.movilmanupuladora.data.model.RegistroResponse
import com.example.movilmanupuladora.data.model.Usuario
import com.example.movilmanupuladora.data.model.Rol
import com.example.movilmanupuladora.data.model.grados
import com.example.movilmanupuladora.data.model.gramaje
import com.example.movilmanupuladora.data.model.inventario
import com.example.movilmanupuladora.data.model.menus
import com.example.movilmanupuladora.data.model.pasos_preparacion
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("roles/")
    suspend fun obtenerRoles(): Response<List<Rol>>

    // Ruta exacta de autenticación en Django/SimpleJWT
    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    //  Si tu registro de usuarios está bajo /api/usuarios/
    @POST("usuarios/")
    suspend fun registrarUsuario(@Body usuario: Usuario): Response<RegistroResponse>

    @GET("inventario/")
    suspend fun obtenerInventario(): Response<List<inventario>>

    @POST("inventario/")
    suspend fun crearInventario(@Body inventarioData: inventario): Response<inventario>

    @GET("grados/")
    suspend fun obtenerGrados(): Response<List<grados>>

    @GET("gramajes/")
    suspend fun obtenerGramajes(): Response<List<gramaje>>

    @POST("gramajes/")
    suspend fun crearGramaje(@Body gramajeData: gramaje): Response<gramaje>


    @GET("pasos_preparacion/")
    suspend fun obtenerPasosPreparacion(): Response<List<pasos_preparacion>>

    @GET("menus/")
    suspend fun obtenerMenus(): Response<List<menus>>
}




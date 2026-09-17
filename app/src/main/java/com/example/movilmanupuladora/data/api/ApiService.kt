package com.example.movilmanipuladora.data.api

import com.example.movilmanipuladora.data.model.LoginRequest
import com.example.movilmanipuladora.data.model.LoginResponse
import com.example.movilmanipuladora.data.model.RegistroResponse
import com.example.movilmanipuladora.data.model.Rol
import com.example.movilmanipuladora.data.model.Usuario
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    // Roles
    @GET("api/roles/")
    suspend fun obtenerRoles(): Response<List<Rol>>

    // Autenticación
    @POST("api/usuarios/login/") // Ajusta la ruta exacta del login si cambia en urls.py
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/usuarios/registro/") // Ajusta la ruta exacta del registro si cambia en urls.py
    suspend fun registrarUsuario(@Body usuario: Usuario): Response<RegistroResponse>
}
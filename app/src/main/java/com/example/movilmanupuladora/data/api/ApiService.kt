package com.example.movilmanupuladora.data.api

import com.example.movilmanupuladora.data.model.LoginRequest
import com.example.movilmanupuladora.data.model.LoginResponse
import com.example.movilmanupuladora.data.model.RegistroResponse
import com.example.movilmanupuladora.data.model.Rol
import com.example.movilmanupuladora.data.model.Usuario
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
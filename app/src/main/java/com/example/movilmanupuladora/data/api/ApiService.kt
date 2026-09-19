package com.example.movilmanupuladora.data.api

import com.example.movilmanupuladora.data.model.DetallePlato
import com.example.movilmanupuladora.data.model.LoginRequest
import com.example.movilmanupuladora.data.model.LoginResponse
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.RegistroResponse
import com.example.movilmanupuladora.data.model.Rol
import com.example.movilmanupuladora.data.model.SeccionMenu
import com.example.movilmanupuladora.data.model.Usuario
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // Roles
    @GET("api/roles/")
    suspend fun obtenerRoles(): Response<List<Rol>>

    // Autenticación
    @POST("api/auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/usuarios/")
    suspend fun registrarUsuario(@Body usuario: Usuario): Response<RegistroResponse>

    // Secciones de Menú
    @GET("api/secciones_menu/")
    suspend fun obtenerSeccionesMenu(): Response<List<SeccionMenu>>

    @GET("api/secciones_menu/{id}/")
    suspend fun obtenerSeccionMenuPorId(@Path("id") id: Int): Response<SeccionMenu>

    // Platos
    @GET("api/platos/")
    suspend fun obtenerPlatos(): Response<List<PlatoResponse>>

    @GET("api/platos/{id}/")
    suspend fun obtenerPlatoPorId(@Path("id") id: Int): Response<PlatoResponse>

    // Detalle de Plato
    @GET("api/detalle_plato/")
    suspend fun obtenerDetallePlatos(): Response<List<DetallePlato>>

    @GET("api/detalle_plato/{id}/")
    suspend fun obtenerDetallePlatoPorId(@Path("id") id: Int): Response<DetallePlato>
}
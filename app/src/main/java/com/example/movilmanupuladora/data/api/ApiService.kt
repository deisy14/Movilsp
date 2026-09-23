package com.example.movilmanupuladora.data.api

import com.example.movilmanupuladora.data.model.DetallePlato
import com.example.movilmanupuladora.data.model.Ingrediente
import com.example.movilmanupuladora.data.model.LoginRequest
import com.example.movilmanupuladora.data.model.LoginResponse
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.RegistroResponse
import com.example.movilmanupuladora.data.model.Rol
import com.example.movilmanupuladora.data.model.SeccionMenu
import com.example.movilmanupuladora.data.model.Usuario
import com.example.movilmanupuladora.data.model.grados
import com.example.movilmanupuladora.data.model.gramaje
import com.example.movilmanupuladora.data.model.inventario
import com.example.movilmanupuladora.data.model.menus
import com.example.movilmanupuladora.data.model.pasos_preparacion
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // Roles
    @GET("roles/")
    suspend fun obtenerRoles(): Response<List<Rol>>

    // Autenticación
    @POST("auth/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Usuarios
    @POST("usuarios/")
    suspend fun registrarUsuario(@Body usuario: Usuario): Response<RegistroResponse>

    // Secciones de Menú
    @GET("secciones_menu/")
    suspend fun obtenerSeccionesMenu(): Response<List<SeccionMenu>>

    @GET("secciones_menu/{id}/")
    suspend fun obtenerSeccionMenuPorId(@Path("id") id: Int): Response<SeccionMenu>

    // Platos
    @GET("platos/")
    suspend fun obtenerPlatos(): Response<List<PlatoResponse>>

    @GET("platos/{id}/")
    suspend fun obtenerPlatoPorId(@Path("id") id: Int): Response<PlatoResponse>

    // Detalle de Plato
    @GET("detalle_plato/")
    suspend fun obtenerDetallePlatos(): Response<List<DetallePlato>>

    @GET("detalle_plato/{id}/")
    suspend fun obtenerDetallePlatoPorId(@Path("id") id: Int): Response<DetallePlato>

    // Inventario
    @GET("inventario/")
    suspend fun obtenerInventario(): Response<List<inventario>>

    @POST("inventario/")
    suspend fun crearInventario(@Body inventarioData: inventario): Response<inventario>

    @retrofit2.http.PUT("inventario/{id}/")
    suspend fun actualizarInventario(@Path("id") id: Int, @Body inventarioData: inventario): Response<inventario>

    // Unidades de Medida
    @GET("unidades_medida/")
    suspend fun obtenerUnidadesMedida(): Response<List<com.example.movilmanupuladora.data.model.UnidadMedida>>

    // Notificaciones / Avisos
    @GET("notificaciones/")
    suspend fun obtenerNotificaciones(): Response<List<com.example.movilmanupuladora.data.model.Notificacion>>

    // Turnos
    @GET("turnos/")
    suspend fun obtenerTurnos(): Response<List<com.example.movilmanupuladora.data.model.Turno>>

    // Ingredientes
    @GET("ingredientes/")
    suspend fun obtenerIngredientes(): Response<List<Ingrediente>>

    // Grados
    @GET("grados/")
    suspend fun obtenerGrados(): Response<List<grados>>

    // Gramajes
    @GET("gramajes/")
    suspend fun obtenerGramajes(): Response<List<gramaje>>

    @POST("gramajes/")
    suspend fun crearGramaje(@Body gramajeData: gramaje): Response<gramaje>

    // Pasos Preparación
    @GET("pasos_preparacion/")
    suspend fun obtenerPasosPreparacion(): Response<List<pasos_preparacion>>

    // Menús
    @GET("menus/")
    suspend fun obtenerMenus(): Response<List<menus>>
}

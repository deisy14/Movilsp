
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

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

import com.google.gson.JsonObject

interface ApiService {

    // Roles
    @GET("roles/")
    suspend fun obtenerRoles(): Response<List<Rol>>

    // Autenticación
    @POST("auth/login/")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // Recuperación de contraseña por código
    @POST("auth/recuperar-password/")
    suspend fun solicitarCodigoRecuperacion(
        @Body request: com.example.movilmanupuladora.data.model.SolicitarRecuperacionRequest
    ): Response<com.example.movilmanupuladora.data.model.MensajeResponse>

    @POST("auth/confirmar-recuperacion-password/")
    suspend fun confirmarCodigoRecuperacion(
        @Body request: com.example.movilmanupuladora.data.model.ConfirmarRecuperacionRequest
    ): Response<com.example.movilmanupuladora.data.model.MensajeResponse>

    // Usuarios
    @GET("usuarios/")
    suspend fun obtenerUsuarios(): Response<List<Usuario>>

    @POST("usuarios/")
    suspend fun registrarUsuario(
        @Body usuario: Usuario
    ): Response<RegistroResponse>

    // Secciones de menú
    @GET("secciones_menu/")
    suspend fun obtenerSeccionesMenu(): Response<List<SeccionMenu>>

    @GET("secciones_menu/{id}/")
    suspend fun obtenerSeccionMenuPorId(
        @Path("id") id: Int
    ): Response<SeccionMenu>

    // Platos
    @GET("platos/")
    suspend fun obtenerPlatos(): Response<List<PlatoResponse>>

    @GET("platos/{id}/")
    suspend fun obtenerPlatoPorId(
        @Path("id") id: Int
    ): Response<PlatoResponse>

    // Detalle de plato
    @GET("detalle_plato/")
    suspend fun obtenerDetallePlatos(): Response<List<DetallePlato>>

    @GET("detalle_plato/{id}/")
    suspend fun obtenerDetallePlatoPorId(
        @Path("id") id: Int
    ): Response<DetallePlato>

    // Inventario
    @GET("inventario/")
    suspend fun obtenerInventario(): Response<List<inventario>>

    @POST("inventario/")
    suspend fun crearInventario(
        @Body inventarioData: inventario
    ): Response<inventario>

    @PUT("inventario/{id}/")
    suspend fun actualizarInventario(
        @Path("id") id: Int,
        @Body inventarioData: inventario
    ): Response<inventario>

    // Unidades de medida
    @GET("unidades_medida/")
    suspend fun obtenerUnidadesMedida():
            Response<List<com.example.movilmanupuladora.data.model.UnidadMedida>>

    // Notificaciones: consultar avisos recibidos
    @GET("notificaciones/")
    suspend fun obtenerNotificaciones():
            Response<List<com.example.movilmanupuladora.data.model.Notificacion>>

    // Notificaciones: enviar aviso al administrador o supervisor.
    // El backend recibe multipart/form-data.
    // La imagen es opcional: enviar null cuando no se adjunte una.
    @Multipart
    @POST("notificaciones/enviar/")
    suspend fun enviarNotificacion(
        @Part("titulo") titulo: RequestBody,
        @Part("mensaje") mensaje: RequestBody,
        @Part imagen: MultipartBody.Part?
    ): Response<JsonObject>

    // Turnos
    @GET("turnos/")
    suspend fun obtenerTurnos():
            Response<List<com.example.movilmanupuladora.data.model.Turno>>

    @POST("turnos/")
    suspend fun crearTurno(@Body turno: com.example.movilmanupuladora.data.model.Turno): Response<com.example.movilmanupuladora.data.model.Turno>

    @retrofit2.http.PATCH("turnos/{id}/")
    suspend fun patchTurno(@Path("id") id: Int, @Body body: Map<String, String>): Response<com.example.movilmanupuladora.data.model.Turno>

    @retrofit2.http.DELETE("turnos/{id}/")
    suspend fun eliminarTurno(@Path("id") id: Int): Response<Unit>

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
    suspend fun crearGramaje(
        @Body gramajeData: gramaje
    ): Response<gramaje>

    // Pasos de preparación
    @GET("pasos_preparacion/")
    suspend fun obtenerPasosPreparacion():
            Response<List<pasos_preparacion>>

    // Menús
    @GET("menus/")
    suspend fun obtenerMenus(): Response<List<menus>>

    // Asistencia diaria
    @GET("asistencia-diaria/")
    suspend fun obtenerAsistenciaDiaria():
            Response<List<com.example.movilmanupuladora.data.model.AsistenciaDiaria>>

    @POST("asistencia-diaria/")
    suspend fun registrarAsistenciaDiaria(
        @Body asistencia: com.example.movilmanupuladora.data.model.AsistenciaDiaria
    ): Response<com.example.movilmanupuladora.data.model.AsistenciaDiaria>

    @PUT("asistencia-diaria/{id}/")
    suspend fun actualizarAsistenciaDiaria(
        @Path("id") id: Int,
        @Body asistencia: com.example.movilmanupuladora.data.model.AsistenciaDiaria
    ): Response<com.example.movilmanupuladora.data.model.AsistenciaDiaria>

    @DELETE("asistencia-diaria/{id}/")
    suspend fun eliminarAsistenciaDiaria(
        @Path("id") id: Int
    ): Response<Unit>

    // Sugerencias de recetas según el inventario de la sede
    @POST("ia/sugerencias-recetas/")
    suspend fun sugerirRecetas(
        @Body request: com.example.movilmanupuladora.data.model.SugerenciasRecetasRequest
    ): Response<com.example.movilmanupuladora.data.model.SugerenciasRecetasResponse>
}

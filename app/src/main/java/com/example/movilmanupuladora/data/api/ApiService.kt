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
import com.example.movilmanupuladora.data.model.SugerenciaRequest
import com.example.movilmanupuladora.data.model.SugerenciaRecetaResponse

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    // Sugerencia de receta con IA adaptada al inventario
    @POST("ia-sugerencias/sugerir/")
    suspend fun obtenerSugerenciaReceta(
        @Body request: SugerenciaRequest
    ): Response<SugerenciaRecetaResponse>

    // Autenticación
    @POST("auth/login/")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // Recuperación de contraseña
    @POST("auth/recuperar-password/")
    suspend fun solicitarCodigoRecuperacion(
        @Body request: com.example.movilmanupuladora.data.model.SolicitarRecuperacionRequest
    ): Response<com.example.movilmanupuladora.data.model.MensajeResponse>

    @POST("auth/confirmar-recuperacion-password/")
    suspend fun confirmarCodigoRecuperacion(
        @Body request: com.example.movilmanupuladora.data.model.ConfirmarRecuperacionRequest
    ): Response<com.example.movilmanupuladora.data.model.MensajeResponse>

    // Roles
    @GET("roles/")
    suspend fun obtenerRoles(): Response<List<Rol>>

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

    // Categorías de inventario
    @GET("categorias_inventario/")
    suspend fun obtenerCategoriasInventario():
            Response<List<com.example.movilmanupuladora.data.model.CategoriaInventario>>

    // Notificaciones
    @GET("notificaciones/")
    suspend fun obtenerNotificaciones():
            Response<List<com.example.movilmanupuladora.data.model.Notificacion>>

    @Multipart
    @POST("notificaciones/enviar/")
    suspend fun enviarNotificacion(
        @Part("titulo") titulo: RequestBody,
        @Part("mensaje") mensaje: RequestBody,
        @Part imagen: MultipartBody.Part?
    ): Response<JsonObject>

    @POST("notificaciones/{id}/marcar-leida/")
    suspend fun marcarNotificacionLeida(
        @Path("id") id: Int
    ): Response<JsonObject>

    @POST("notificaciones/marcar-todas-leidas/")
    suspend fun marcarTodasLeidas(): Response<JsonObject>

    // Turnos
    @GET("turnos/")
    suspend fun obtenerTurnos():
            Response<List<com.example.movilmanupuladora.data.model.Turno>>

    @POST("turnos/")
    suspend fun crearTurno(
        @Body turno: com.example.movilmanupuladora.data.model.Turno
    ): Response<com.example.movilmanupuladora.data.model.Turno>

    @PATCH("turnos/{id}/")
    suspend fun patchTurno(
        @Path("id") id: Int,
        @Body body: Map<String, String>
    ): Response<com.example.movilmanupuladora.data.model.Turno>

    @DELETE("turnos/{id}/")
    suspend fun eliminarTurno(
        @Path("id") id: Int
    ): Response<Unit>

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

    @GET("menus/")
    suspend fun obtenerMenusRaw(): Response<com.google.gson.JsonElement>

    @POST("menus/")
    suspend fun registrarMenu(
        @Body menuData: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Map<String, Any?>>

    @PUT("menus/{id}/")
    suspend fun actualizarMenu(
        @Path("id") id: Int,
        @Body menuData: Map<String, @JvmSuppressWildcards Any?>
    ): Response<com.google.gson.JsonElement>

    // Asistencia diaria
    @GET("asistencia-diaria/")
    suspend fun obtenerAsistenciaDiaria():
            Response<List<com.example.movilmanupuladora.data.model.AsistenciaDiaria>>

    @POST("asistencia-diaria/")
    suspend fun registrarAsistenciaDiaria(
        @Body asistencia: com.example.movilmanupuladora.data.model.AsistenciaDiaria
    ): Response<com.example.movilmanupuladora.data.model.AsistenciaDiaria>

    @POST("asistencia-diaria/")
    suspend fun registrarAsistenciaDiariaMap(
        @Body asistencia: Map<String, @JvmSuppressWildcards Any?>
    ): Response<com.example.movilmanupuladora.data.model.AsistenciaDiaria>

    @PUT("asistencia-diaria/{id}/")
    suspend fun actualizarAsistenciaDiaria(
        @Path("id") id: Int,
        @Body asistencia: com.example.movilmanupuladora.data.model.AsistenciaDiaria
    ): Response<com.example.movilmanupuladora.data.model.AsistenciaDiaria>

    @PATCH("asistencia-diaria/{id}/")
    suspend fun patchAsistenciaDiaria(
        @Path("id") id: Int,
        @Body asistencia: Map<String, @JvmSuppressWildcards Any?>
    ): Response<com.example.movilmanupuladora.data.model.AsistenciaDiaria>

    @DELETE("asistencia-diaria/{id}/")
    suspend fun eliminarAsistenciaDiaria(
        @Path("id") id: Int
    ): Response<Unit>

    @POST("ia/sugerencias-recetas/")
    suspend fun sugerirRecetas(
        @Body request: com.example.movilmanupuladora.data.model.SugerenciasRecetasRequest
    ): Response<com.example.movilmanupuladora.data.model.SugerenciasRecetasResponse>

    // Descontar inventario al confirmar preparación con IA
    @POST("ia/descontar-inventario/")
    suspend fun descontarInventario(
        @Body request: com.example.movilmanupuladora.data.model.DescontarInventarioRequest
    ): Response<com.example.movilmanupuladora.data.model.DescontarInventarioResponse>

    // Entregas de alimentos desde supervisor
    @GET("entregas/")
    suspend fun obtenerEntregas(
        @retrofit2.http.Query("id_sede") idSede: Int? = null,
        @retrofit2.http.Query("estado") estado: String? = null
    ): Response<List<com.example.movilmanupuladora.data.model.EntregaItem>>

    @GET("entregas/")
    suspend fun obtenerEntregasRaw(
        @retrofit2.http.Query("id_sede") idSede: Int? = null,
        @retrofit2.http.Query("estado") estado: String? = null
    ): Response<com.google.gson.JsonElement>

    @POST("entregas/{id}/confirmar-recepcion/")
    suspend fun confirmarRecepcionEntrega(
        @Path("id") idEntrega: Int,
        @Body request: com.example.movilmanupuladora.data.model.ConfirmarRecepcionRequest
    ): Response<JsonObject>

    // Contratos y Secciones de Menú asignadas a la sede
    @GET("contratos/")
    suspend fun obtenerContratos(): Response<List<com.example.movilmanupuladora.data.model.ContratoModel>>

    @GET("contratos/")
    suspend fun obtenerContratosRaw(): Response<com.google.gson.JsonElement>

    @GET("contratos-secciones-menu/")
    suspend fun obtenerContratosSeccionesMenu(): Response<List<com.example.movilmanupuladora.data.model.ContratoSeccionMenuModel>>

    @GET("contratos-secciones-menu/")
    suspend fun obtenerContratosSeccionesMenuRaw(): Response<com.google.gson.JsonElement>

    @GET("contratos/{id}/distribucion/")
    suspend fun obtenerDistribucionContrato(
        @retrofit2.http.Path("id") id: Int
    ): Response<com.google.gson.JsonElement>

    @GET("distribucion-cupos/")
    suspend fun obtenerDistribucionCupos(): Response<com.google.gson.JsonElement>

    @GET("sedes/")
    suspend fun obtenerSedes(): Response<com.google.gson.JsonElement>

    @GET("sedes/{id}/configuracion/")
    suspend fun obtenerConfiguracionSede(
        @retrofit2.http.Path("id") id: Int
    ): Response<com.google.gson.JsonElement>

    @GET("jornadas/")
    suspend fun obtenerJornadasRaw(): Response<com.google.gson.JsonElement>

    // =========================================================
    // RECONOCIMIENTO FACIAL BIOMÉTRICO (SIRAE)
    // =========================================================
    @POST("reconocimiento-facial/verify-face/")
    suspend fun verificarRostroFacial(
        @Body request: com.example.movilmanupuladora.data.model.VerifyFaceRequest
    ): Response<com.example.movilmanupuladora.data.model.VerifyFaceResponse>

    @POST("reconocimiento-facial/register-face/")
    suspend fun registrarRostroFacial(
        @Body request: com.example.movilmanupuladora.data.model.RegisterFaceRequest
    ): Response<com.example.movilmanupuladora.data.model.RegisterFaceResponse>

    @GET("reconocimiento-facial/estado/")
    suspend fun consultarEstadoBiometria(): Response<com.example.movilmanupuladora.data.model.EstadoBiometriaResponse>

    @DELETE("reconocimiento-facial/eliminar/")
    suspend fun eliminarBiometriaFacial(): Response<JsonObject>
}

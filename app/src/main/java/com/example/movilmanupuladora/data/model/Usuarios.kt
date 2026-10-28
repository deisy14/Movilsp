
package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

// Modelo para enviar o recibir un usuario completo
data class Usuario(
    @SerializedName("id_usuario")
    val idUsuario: Int? = null,

    val nombre: String,
    val apellido: String = "",
    val correo: String = "",

    @SerializedName("tipo_documento")
    val tipoDocumento: String = "",

    @SerializedName("numero_documento")
    val numeroDocumento: String = "",

    // ID del rol seleccionado al registrar
    @SerializedName("rol", alternate = ["rol_id", "id_rol"])
    val rol: Int? = null,

    // Nombre del rol (viene en el listado de usuarios)
    @SerializedName("nombre_rol", alternate = ["rol_nombre", "nombreRol", "role"])
    val nombreRol: String? = null,

    val password: String? = null,

    @SerializedName("is_active")
    val isActive: Boolean? = true,

    @SerializedName("is_staff")
    val isStaff: Boolean? = false
) {
    /** Nombre completo del usuario */
    val nombreCompleto: String
        get() = if (apellido.isNotBlank()) "$nombre $apellido" else nombre
}

// Modelo para la petición de Login
data class LoginRequest(
    val correo: String,
    val password: String
)

// Modelo para la respuesta de Login
data class LoginResponse(
    @SerializedName("token")
    val token: String? = null,

    @SerializedName("access")
    val access: String? = null,

    @SerializedName("refresh")
    val refresh: String? = null,

    @SerializedName("usuario")
    val usuario: UsuarioInfo? = null
) {
    val authToken: String?
        get() = access ?: token
}

// Datos del usuario que entrega la respuesta del Login
data class UsuarioInfo(
    @SerializedName("id_usuario")
    val idUsuario: Int,

    val nombre: String,
    val apellido: String,
    val correo: String,
    val rol: String?,

    // Sede asignada al usuario autenticado
    @SerializedName("id_sede")
    val idSede: Int? = null,

    @SerializedName("sede_nombre")
    val sedeNombre: String? = null,

    @SerializedName("id_institucion")
    val idInstitucion: Int? = null,

    @SerializedName("institucion_nombre")
    val institucionNombre: String? = null,

    @SerializedName("is_superuser")
    val isSuperuser: Boolean? = false,

    @SerializedName("debe_cambiar_password")
    val debeCambiarPassword: Boolean? = false
)

// Modelo para la respuesta del Registro
data class RegistroResponse(
    val mensaje: String,
    val data: Usuario
)

// Modelos para recuperación de contraseña con código
data class SolicitarRecuperacionRequest(
    val correo: String
)

data class ConfirmarRecuperacionRequest(
    val correo: String,
    val codigo: String,
    val nueva_password: String,
    val confirmar_password: String
)

data class MensajeResponse(
    val mensaje: String? = null,
    val error: String? = null,
    val status: String? = null,

    @SerializedName("debug_code")
    val debugCode: String? = null,

    @SerializedName("debug_link")
    val debugLink: String? = null
)

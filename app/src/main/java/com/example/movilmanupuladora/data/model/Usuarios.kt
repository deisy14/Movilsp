package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

// Modelo para enviar o recibir un usuario completo
data class Usuario(
    @SerializedName("id_usuario")
    val idUsuario: Int? = null,

    val nombre: String,
    val apellido: String,
    val correo: String,

    @SerializedName("tipo_documento")
    val tipoDocumento: String,

    @SerializedName("numero_documento")
    val numeroDocumento: String,

    // ID del rol seleccionado al registrar
    val rol: Int? = null,

    val password: String? = null,

    @SerializedName("is_active")
    val isActive: Boolean? = true,

    @SerializedName("is_staff")
    val isStaff: Boolean? = false
)

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
    val rol: String?
)

// Modelo para la respuesta del Registro
data class RegistroResponse(
    val mensaje: String,
    val data: Usuario
)

package com.example.movilmanipuladora.data.model

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

    // ID del rol seleccionado al registrar (Ej: 11, 4, 10, etc.)
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
    val token: String,
    val refresh: String,
    val usuario: UsuarioInfo
)

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
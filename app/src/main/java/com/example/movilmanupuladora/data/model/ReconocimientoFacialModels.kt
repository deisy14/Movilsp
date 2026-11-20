package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

/**
 * Petición para verificar el rostro y realizar login biométrico.
 */
data class VerifyFaceRequest(
    @SerializedName("image")
    val image: String
)

/**
 * Usuario retornado en la respuesta exitosa de verificación facial.
 */
data class VerifyFaceUsuario(
    @SerializedName("id_usuario", alternate = ["id", "pk"])
    val idUsuario: Int? = null,

    @SerializedName("correo", alternate = ["email"])
    val correo: String = "",

    @SerializedName("nombre", alternate = ["first_name"])
    val nombre: String = "",

    @SerializedName("apellido", alternate = ["last_name"])
    val apellido: String = "",

    @SerializedName("rol", alternate = ["rol_nombre", "nombre_rol"])
    val rol: String = ""
) {
    val nombreCompleto: String
        get() = if (apellido.isNotBlank()) "$nombre $apellido".trim() else nombre.trim()
}

/**
 * Respuesta del backend al verificar rostro (emite JWT y datos de usuario).
 */
data class VerifyFaceResponse(
    @SerializedName("access")
    val access: String? = null,

    @SerializedName("refresh")
    val refresh: String? = null,

    @SerializedName("usuario")
    val usuario: VerifyFaceUsuario? = null,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("detail")
    val detail: String? = null
)

/**
 * Petición para registrar o actualizar la biometría facial de un usuario.
 */
data class RegisterFaceRequest(
    @SerializedName("image")
    val image: String,

    @SerializedName("correo")
    val correo: String? = null
)

/**
 * Respuesta del registro biométrico.
 */
data class RegisterFaceResponse(
    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("usuario")
    val usuario: String? = null,

    @SerializedName("detail")
    val detail: String? = null
)

/**
 * Respuesta al consultar el estado de la biometría facial del usuario autenticado.
 */
data class EstadoBiometriaResponse(
    @SerializedName("tiene_biometria")
    val tieneBiometria: Boolean = false,

    @SerializedName("correo")
    val correo: String = ""
)

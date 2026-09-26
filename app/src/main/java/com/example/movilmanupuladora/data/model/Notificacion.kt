package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class Notificacion(
    @SerializedName("id_notificacion")
    val idNotificacion: Int? = null,

    @SerializedName("titulo")
    val titulo: String? = null,

    @SerializedName("mensaje")
    val mensaje: String? = null,

    @SerializedName("fecha")
    val fecha: String? = null,

    @SerializedName("tipo")
    val tipo: String? = null,

    @SerializedName("leido")
    val leido: Boolean? = false
)

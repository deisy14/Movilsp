package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class Notificacion(
    @SerializedName("id_notificacion")
    val idNotificacion: Int? = null,

    @SerializedName("titulo")
    val titulo: String? = null,

    @SerializedName("mensaje")
    val mensaje: String? = null,

    @SerializedName(value = "fecha_hora", alternate = ["fecha", "fecha_creacion", "created_at"])
    val fecha: String? = null,

    @SerializedName("tipo")
    val tipo: String? = null,

    @SerializedName(value = "leida", alternate = ["leido"])
    val leido: Boolean? = false
)

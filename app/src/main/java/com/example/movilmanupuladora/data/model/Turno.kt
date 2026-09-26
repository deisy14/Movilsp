package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class Turno(
    @SerializedName("id_turno")
    val idTurno: Int,

    @SerializedName("nombre_turno")
    val nombreTurno: String? = null,

    @SerializedName("hora_inicio")
    val horaInicio: String? = null,

    @SerializedName("hora_fin")
    val horaFin: String? = null,

    @SerializedName("fecha")
    val fecha: String? = null,

    @SerializedName("observaciones")
    val observaciones: String? = null
)

package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class AsistenciaDiaria(
    @SerializedName("id_asistencia")
    val idAsistencia: Int? = null,

    @SerializedName("id_grado")
    val idGrado: Int,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("ninos_presentes")
    val ninosPresentes: Int,

    @SerializedName("id_usuario_manipuladora")
    val idUsuarioManipuladora: Int? = null
)

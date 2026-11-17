package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class AsistenciaDiaria(
    @SerializedName("id_asistencia")
    val idAsistencia: Int? = null,

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("id_grado")
    val idGrado: Int? = null,

    @SerializedName("grado")
    val grado: Int? = null,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("ninos_presentes")
    val ninosPresentes: Int,

    @SerializedName("id_usuario_manipuladora")
    val idUsuarioManipuladora: Int? = null,

    @SerializedName("usuario_manipuladora")
    val usuarioManipuladora: Int? = null
) {
    fun obtenerIdReal(): Int? = idAsistencia ?: id
    fun obtenerIdGradoReal(): Int = idGrado ?: grado ?: 16
}

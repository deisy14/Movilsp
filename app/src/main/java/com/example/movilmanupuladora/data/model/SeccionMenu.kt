package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class SeccionMenu(
    @SerializedName("id_seccion")
    val idSeccion: Int,

    @SerializedName("id_jornada")
    val idJornada: Int? = null,

    @SerializedName("nombre_seccion")
    val nombreSeccion: String? = null
)

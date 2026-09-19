package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class menus(

    @SerializedName("id_menu")
    val id_menu: Int,
    val id_jornada: Int,
    val fecha: Int,
    val ninos_presentes: Int,
    val estado: String,
    val informacion_nutricional: String,
    val id_contrato: Int,
)

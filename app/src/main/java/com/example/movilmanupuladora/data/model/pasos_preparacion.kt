package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class pasos_preparacion(

    @SerializedName("id_pasos_preparacion")
    val id_plato: Int,
    val numero_paso: Int,
    val descripcion_paso: String
)

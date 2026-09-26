package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class UnidadMedida(
    @SerializedName("id_unidad_medida")
    val idUnidadMedida: Int,

    @SerializedName("nombre_unidad")
    val nombreUnidad: String,

    @SerializedName("abreviatura")
    val abreviatura: String? = null
)

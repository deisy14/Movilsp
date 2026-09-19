package com.example.movilmanupuladora.data.model
import com.google.gson.annotations.SerializedName

data class grados(
    @SerializedName("id_grado") val idGrado: Int,
    @SerializedName("nombre_grado") val nombreGrado: String
)

package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class Rol(
    @SerializedName("id_rol")
    val idRol: Int,

    val nombre: String,

    val descripcion: String? = null
)
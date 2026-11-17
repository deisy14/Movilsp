package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class grados(
    @SerializedName("id_grado") val idGrado: Int = 0,
    @SerializedName("id") val id: Int? = null,
    @SerializedName("nombre_grado") val nombreGrado: String? = null,
    @SerializedName("nombre") val nombre: String? = null
) {
    fun obtenerIdReal(): Int = if (idGrado > 0) idGrado else (id ?: 0)
    fun obtenerNombreReal(): String = nombreGrado ?: nombre ?: ""
}

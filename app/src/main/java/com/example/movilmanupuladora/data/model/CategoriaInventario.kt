package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class CategoriaInventario(
    @SerializedName("id_categoria_inventario")
    val idCategoriaInventario: Int,

    @SerializedName("nombre_categoria")
    val nombreCategoria: String? = null,

    @SerializedName("nombre")
    val nombre: String? = null
) {
    fun getNombreMostrar(): String {
        return nombreCategoria ?: nombre ?: "General"
    }
}

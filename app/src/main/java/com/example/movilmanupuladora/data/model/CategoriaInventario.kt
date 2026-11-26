package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class CategoriaInventario(
    @SerializedName(value = "id_categoria_inventario", alternate = ["id"])
    val idCategoriaInventario: Int = 0,

    @SerializedName(value = "nombre_categoria", alternate = ["nombre"])
    val nombreCategoria: String = ""
)

package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class Ingrediente(
    @SerializedName("id_ingrediente")
    val idIngrediente: Int,

    @SerializedName("nombre_ingrediente")
    val nombreIngrediente: String? = null,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("imagen_ingrediente")
    val imagenIngrediente: String? = null,

    @SerializedName("marca_ingrediente")
    val marcaIngrediente: String? = null,

    @SerializedName("id_categoria_inventario")
    val idCategoriaInventario: Int? = null,

    @SerializedName("id_unidad_medida")
    val idUnidadMedida: Int? = null
)

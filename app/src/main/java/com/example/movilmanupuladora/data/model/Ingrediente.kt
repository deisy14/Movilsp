package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class Ingrediente(
    @SerializedName("id_ingrediente")
    val idIngrediente: Int = 0,

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("nombre_ingrediente")
    val nombreIngrediente: String? = null,

    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("imagen_ingrediente")
    val imagenIngrediente: String? = null,

    @SerializedName("marca_ingrediente")
    val marcaIngrediente: String? = null,

    @SerializedName("id_categoria_inventario")
    val idCategoriaInventario: Int? = null,

    @SerializedName("categoria")
    val categoria: Int? = null,

    @SerializedName("id_unidad_medida")
    val idUnidadMedida: Int? = null,

    @SerializedName("unidad_medida")
    val unidadMedida: Int? = null
) {
    fun obtenerIdReal(): Int = if (idIngrediente > 0) idIngrediente else (id ?: 0)
    fun obtenerNombreReal(): String = nombreIngrediente ?: nombre ?: ""
    fun obtenerCategoriaReal(): Int? = idCategoriaInventario ?: categoria
    fun obtenerUnidadReal(): Int? = idUnidadMedida ?: unidadMedida
}

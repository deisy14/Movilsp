package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class inventario(
    @SerializedName("id_inventario")
    val idInventario: Int = 0,

    @SerializedName("id")
    val id: Int? = null,

    // Campos según estructura SQL / Django API
    @SerializedName("id_ingrediente")
    val idIngrediente: Int? = null,

    @SerializedName("ingrediente")
    val ingrediente: Int? = null,

    @SerializedName("cantidad_actual")
    val cantidadActual: String? = null,   // La API lo devuelve como "15.00" (texto)

    @SerializedName("stock_minimo")
    val stockMinimo: String? = null,      // La API lo devuelve como "4.00" (texto)

    @SerializedName("id_unidad_medida")
    val idUnidadMedida: Int? = null,

    @SerializedName("unidad_medida_id")
    val unidadMedidaId: Int? = null,

    @SerializedName("id_sede")
    val idSede: Int? = null,

    @SerializedName("sede")
    val sede: Int? = null,

    // Campos de compatibilidad
    val nombre: String? = null,

    @SerializedName("nombre_ingrediente")
    val nombreIngrediente: String? = null,

    val cantidad: Double? = null,
    val unidad_medida: String? = null,
    val fecha_ingreso: String? = null
) {
    fun obtenerIdReal(): Int = if (idInventario > 0) idInventario else (id ?: 0)
    fun obtenerIdIngredienteReal(): Int? = idIngrediente ?: ingrediente
    fun obtenerIdUnidadReal(): Int? = idUnidadMedida ?: unidadMedidaId
}

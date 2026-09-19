package com.example.movilmanupuladora.data.model
import com.google.gson.annotations.SerializedName

data class inventario(
    @SerializedName("id_inventario")
    val idInventario: Int,

    // Campos según estructura SQL
    @SerializedName("id_ingrediente")
    val idIngrediente: Int? = null,
    @SerializedName("cantidad_actual")
    val cantidadActual: String? = null,   // La API lo devuelve como "15.00" (texto)
    @SerializedName("stock_minimo")
    val stockMinimo: String? = null,      // La API lo devuelve como "4.00" (texto)
    @SerializedName("id_unidad_medida")
    val idUnidadMedida: Int? = null,

    // Campos de compatibilidad
    val id: Int? = null,
    val nombre: String? = null,
    val cantidad: Double? = null,
    val unidad_medida: String? = null,
    val fecha_ingreso: String? = null
)


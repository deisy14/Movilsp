package com.example.movilmanupuladora.data.model
import com.google.gson.annotations.SerializedName

data class inventario(
    @SerializedName("id_inventario")
    val idInventario: Int,
    val id: Int? = null,
    val nombre: String,
    val cantidad: Int,
    val unidad_medida: String? = null,
    val fecha_ingreso: String? = null
)

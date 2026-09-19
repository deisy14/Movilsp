package com.example.movilmanupuladora.data.model

data class inventario(
    val id: Int? = null,
    val nombre: String,
    val cantidad: Int,
    val unidad_medida: String? = null,
    val fecha_ingreso: String? = null
)

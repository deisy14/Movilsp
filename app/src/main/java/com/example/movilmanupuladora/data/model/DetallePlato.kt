package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class DetallePlato(
    @SerializedName("id_detalle_plato")
    val idDetallePlato: Int,

    @SerializedName("id_menu")
    val idMenu: Int? = null,

    @SerializedName("id_plato")
    val idPlato: Int? = null,

    @SerializedName("porcion_por_nino")
    val porcionPorNino: String? = null,

    @SerializedName("total_a_preparar")
    val totalAPreparar: String? = null,

    @SerializedName("unidad_total")
    val unidadTotal: String? = null,

    @SerializedName("estado_preparacion")
    val estadoPreparacion: String? = null
)

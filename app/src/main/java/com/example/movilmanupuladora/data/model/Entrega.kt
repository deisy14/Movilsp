package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class EntregaItem(
    @SerializedName("id_entrega") val idEntrega: Int? = null,
    @SerializedName("id_ingrediente") val idIngrediente: Int? = null,
    @SerializedName("ingrediente_nombre") val ingredienteNombre: String? = null,
    @SerializedName("cantidad") val cantidad: Double? = null,
    @SerializedName("unidad") val unidad: String? = null,
    @SerializedName("fecha_entrega") val fechaEntrega: String? = null,
    @SerializedName("estado") val estado: String? = null,
    @SerializedName("documento_entrega") val documentoEntrega: String? = null,
    @SerializedName("id_sede") val idSede: Int? = null,
    @SerializedName("sede_nombre") val sedeNombre: String? = null,
    @SerializedName("id_institucion") val idInstitucion: Int? = null,
    @SerializedName("institucion_nombre") val institucionNombre: String? = null,
    @SerializedName("observaciones") val observaciones: String? = null
)

data class ConfirmarRecepcionRequest(
    @SerializedName("cantidad_recibida") val cantidadRecibida: Double,
    @SerializedName("cantidad_rechazada") val cantidadRechazada: Double = 0.0,
    @SerializedName("motivo_rechazo") val motivoRechazo: String = ""
)

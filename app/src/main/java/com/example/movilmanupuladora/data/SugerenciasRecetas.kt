
package com.example.movilmanupuladora.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class SugerenciasRecetasRequest(
    @SerializedName("id_sede")
    val idSede: Int,

    @SerializedName("ninos_presentes")
    val ninosPresentes: Int,

    @SerializedName("preferencia")
    val preferencia: String = ""
)

data class IngredienteDisponible(
    @SerializedName("id_ingrediente")
    val idIngrediente: Int? = null,

    @SerializedName("nombre")
    val nombre: String = "",

    @SerializedName("cantidad_disponible")
    val cantidadDisponible: String = "0",

    @SerializedName("id_unidad_medida")
    val idUnidadMedida: Int? = null
)

data class SugerenciasRecetasResponse(
    @SerializedName("id_sede")
    val idSede: Int? = null,

    @SerializedName("ninos_presentes")
    val ninosPresentes: Int? = null,

    @SerializedName("ingredientes")
    val ingredientes: List<IngredienteDisponible> = emptyList(),

    @SerializedName("sugerencias")
    val sugerencias: JsonElement? = null,

    @SerializedName("mensaje")
    val mensaje: String? = null,

    @SerializedName("modo")
    val modo: String? = null
)

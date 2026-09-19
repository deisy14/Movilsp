package com.example.movilmanupuladora.data.model
import com.google.gson.annotations.SerializedName

data class gramaje(
    @SerializedName("id_gramaje") val idGramaje: Int,
    @SerializedName("id_ingrediente") val idIngrediente: Int,
    @SerializedName("id_grado") val idGrado: Int,
    @SerializedName("id_unidad_medida") val idUnidadMedida: Int,
    @SerializedName("cantidad_gramaje") val cantidadGramaje: Double,
    @SerializedName("descripcion") val descripcion: String?
)

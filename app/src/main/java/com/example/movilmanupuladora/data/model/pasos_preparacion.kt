package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class pasos_preparacion(
    @SerializedName("id_plato")
    val idPlato: Int,
    @SerializedName("numero_paso")
    val numeroPaso: String,
    @SerializedName("descripcion_paso")
    val descripcionPaso: String
)

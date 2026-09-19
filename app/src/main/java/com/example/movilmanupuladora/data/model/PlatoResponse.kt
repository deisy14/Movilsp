package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class PlatoResponse(
    @SerializedName("id_plato")
    val idPlato: Int,

    @SerializedName("id_seccion")
    val idSeccion: Int? = null,

    @SerializedName("nombre_plato")
    val nombrePlato: String? = null,

    @SerializedName("componente")
    val componente: String? = null,

    @SerializedName("imagen_url")
    val imagenUrl: String? = null
) {
    // Propiedades de acceso para compatibilidad con código existente
    val nombre: String?
        get() = nombrePlato

    val componentes: String?
        get() = componente
}

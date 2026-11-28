package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class PlatoResponse(
    @SerializedName("id_plato")
    val idPlato: Int,

    @SerializedName("id_seccion")
    val idSeccion: Int? = null,

    @SerializedName("nombre_seccion", alternate = ["seccion", "seccion_nombre"])
    val nombreSeccion: String? = null,

    @SerializedName("nombre_plato")
    val nombrePlato: String? = null,

    @SerializedName("componente")
    val componente: String? = null,

    @SerializedName("imagen_url")
    val imagenUrl: String? = null,

    @SerializedName("receta_componentes")
    val recetaComponentes: List<RecetaComponenteItem>? = null,

    @SerializedName("pasos_preparacion")
    val pasosPreparacion: List<PasoPreparacionItem>? = null
) {
    data class RecetaComponenteItem(
        @SerializedName("id_receta_componente")
        val idRecetaComponente: Int? = null,
        @SerializedName("id_ingrediente")
        val idIngrediente: Int? = null,
        @SerializedName("nombre_ingrediente", alternate = ["ingrediente_nombre"])
        val nombreIngrediente: String? = null,
        @SerializedName("cantidad")
        val cantidad: Double? = null,
        @SerializedName("base_cantidad")
        val baseCantidad: String? = null
    )

    data class PasoPreparacionItem(
        @SerializedName("id")
        val id: Int? = null,
        @SerializedName("numero_paso")
        val numeroPaso: String? = null,
        @SerializedName("descripcion_paso", alternate = ["descripcion"])
        val descripcionPaso: String? = null
    )

    // Propiedades de acceso para compatibilidad con código existente
    val nombre: String?
        get() = nombrePlato

    val componentes: String?
        get() = componente
}

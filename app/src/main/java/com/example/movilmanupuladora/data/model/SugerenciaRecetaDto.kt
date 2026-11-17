package com.example.movilmanupuladora.data.model

data class SugerenciaRequest(
    val id_menu: Int,
    val id_plato: Int
)

data class IngredienteSugerido(
    val id_ingrediente: Int,
    val nombre: String,
    val cantidad_por_nino: String,
    val cantidad_total: String,
    val unidad: String,
    val cantidad_disponible: String?,
    val alcanza: Boolean,
    val unidad_compatible: Boolean
)

data class SugerenciaRecetaResponse(
    val id_menu: Int,
    val id_plato: Int,
    val nombre_plato: String,
    val ninos_presentes: Int,
    val ingredientes: List<IngredienteSugerido>,
    val es_sugerencia: Boolean
)
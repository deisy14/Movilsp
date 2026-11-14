package com.example.movilmanupuladora.data.model

import com.google.gson.annotations.SerializedName

data class SugerenciasRecetasRequest(
    @SerializedName("id_sede")
    val idSede: Int,

    @SerializedName("ninos_presentes")
    val ninosPresentes: Int,

    @SerializedName("id_seccion")
    val idSeccion: Int = 1,

    @SerializedName("preferencia")
    val preferencia: String = ""
)

data class IngredienteCalculadoIA(
    @SerializedName("id_ingrediente")
    val idIngrediente: Int? = null,

    @SerializedName("id_inventario")
    val idInventario: Int? = null,

    @SerializedName("nombre_ingrediente")
    val nombreIngrediente: String = "",

    @SerializedName("cantidad_por_nino")
    val cantidadPorNino: String = "",

    @SerializedName("cantidad_por_nino_valor")
    val cantidadPorNinoValor: String = "",

    @SerializedName("cantidad_total")
    val cantidadTotal: String = "",

    @SerializedName("cantidad_total_valor")
    val cantidadTotalValor: String = "",

    @SerializedName("unidad")
    val unidad: String = "",

    @SerializedName("stock_actual")
    val stockActual: String = "0.0",

    @SerializedName("stock_restante_proyectado")
    val stockRestanteProyectado: String = "0.0",

    @SerializedName("alcanza")
    val alcanza: Boolean = true
)

data class SugerenciaIAItem(
    @SerializedName("id_plato")
    val idPlato: Int? = null,

    @SerializedName("nombre_plato")
    val nombrePlato: String = "",

    @SerializedName("id_seccion")
    val idSeccion: Int = 1,

    @SerializedName("seccion_nombre")
    val seccionNombre: String = "Desayuno",

    @SerializedName("componente")
    val componente: String = "",

    @SerializedName("componentes_detalle")
    val componentesDetalle: Map<String, String>? = null,

    @SerializedName("ninos_presentes")
    val ninosPresentes: Int = 200,

    @SerializedName("ingredientes")
    val ingredientes: List<IngredienteCalculadoIA> = emptyList(),

    @SerializedName("pasos")
    val pasos: List<String> = emptyList(),

    @SerializedName("origen")
    val origen: String = "SUGERENCIA_IA",

    @SerializedName("etiqueta")
    val etiqueta: String? = null,

    @SerializedName("viable_con_inventario")
    val viableConInventario: Boolean = true
)

data class SugerenciasRecetasResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("seccion")
    val seccion: String? = null,

    @SerializedName("id_seccion")
    val idSeccion: Int? = null,

    @SerializedName("ninos_presentes")
    val ninosPresentes: Int? = null,

    @SerializedName("id_sede")
    val idSede: Int? = null,

    @SerializedName("sugerencias")
    val sugerencias: String? = null,

    @SerializedName("sugerencias_ia_estructuradas")
    val sugerenciasIaEstructuradas: List<SugerenciaIAItem> = emptyList(),

    @SerializedName("platos_estipulados_pae")
    val platosEstipuladosPae: List<SugerenciaIAItem> = emptyList()
)

data class DescontarInventarioRequest(
    @SerializedName("id_sede")
    val idSede: Int,

    @SerializedName("id_plato")
    val idPlato: Int? = null,

    @SerializedName("nombre_plato")
    val nombrePlato: String = "",

    @SerializedName("ninos_presentes")
    val ninosPresentes: Int = 200,

    @SerializedName("ingredientes")
    val ingredientes: List<IngredienteCalculadoIA> = emptyList()
)

data class DescuentoItem(
    @SerializedName("id_ingrediente")
    val idIngrediente: Int? = null,

    @SerializedName("id_inventario")
    val idInventario: Int? = null,

    @SerializedName("ingrediente")
    val ingrediente: String = "",

    @SerializedName("cantidad_descontada")
    val cantidadDescontada: String = "0.0",

    @SerializedName("unidad")
    val unidad: String = "",

    @SerializedName("stock_anterior")
    val stockAnterior: String = "0.0",

    @SerializedName("stock_restante")
    val stockRestante: String = "0.0"
)

data class DescontarInventarioResponse(
    @SerializedName("status")
    val status: String = "success",

    @SerializedName("mensaje")
    val mensaje: String = "",

    @SerializedName("descuentos")
    val descuentos: List<DescuentoItem> = emptyList()
)

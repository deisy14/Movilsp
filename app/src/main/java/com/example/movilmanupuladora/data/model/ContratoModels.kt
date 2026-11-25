package com.example.movilmanupuladora.data.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ContratoModel(
    @SerializedName("id_contrato")
    val idContrato: Int? = null,

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("id_sede")
    val idSede: Int? = null,

    @SerializedName("sede_nombre")
    val sedeNombre: String? = null,

    @SerializedName("id_institucion")
    val idInstitucion: Int? = null,

    @SerializedName("institucion_nombre")
    val institucionNombre: String? = null,

    @SerializedName("institucion")
    val institucionRaw: JsonElement? = null,

    @SerializedName("id_jornada")
    val idJornada: Int? = null,

    @SerializedName("jornada_nombre")
    val jornadaNombre: String? = null,

    @SerializedName("jornada")
    val jornadaRaw: JsonElement? = null,

    @SerializedName("estado")
    val estado: String? = null
) {
    fun obtenerIdContratoReal(): Int? = idContrato ?: id

    val institucionTexto: String?
        get() = when {
            !institucionNombre.isNullOrBlank() -> institucionNombre
            institucionRaw != null && institucionRaw.isJsonPrimitive -> institucionRaw.asString
            institucionRaw != null && institucionRaw.isJsonObject -> {
                val obj = institucionRaw.asJsonObject
                obj.get("nombre")?.asString ?: obj.get("nombre_institucion")?.asString
            }
            else -> null
        }
}

data class ContratoSeccionMenuModel(
    @SerializedName("id_contrato_seccion")
    val idContratoSeccion: Int? = null,

    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("id_contrato")
    val idContratoRaw: JsonElement? = null,

    @SerializedName("contrato")
    val contratoRaw: JsonElement? = null,

    @SerializedName("id_seccion")
    val idSeccionRaw: JsonElement? = null,

    @SerializedName("seccion")
    val seccionRaw: JsonElement? = null,

    @SerializedName("estado")
    val estado: String? = null
) {
    fun obtenerIdContratoNum(): Int? {
        val elem = idContratoRaw ?: contratoRaw ?: return null
        return try {
            if (elem.isJsonPrimitive) elem.asInt
            else if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                when {
                    obj.has("id_contrato") -> obj.get("id_contrato").asInt
                    obj.has("id") -> obj.get("id").asInt
                    else -> null
                }
            } else null
        } catch (_: Exception) { null }
    }

    fun obtenerIdSeccionNum(): Int? {
        val elem = idSeccionRaw ?: seccionRaw ?: return null
        return try {
            if (elem.isJsonPrimitive) elem.asInt
            else if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                when {
                    obj.has("id_seccion") -> obj.get("id_seccion").asInt
                    obj.has("id") -> obj.get("id").asInt
                    else -> null
                }
            } else null
        } catch (_: Exception) { null }
    }

    fun obtenerNombreSeccion(): String? {
        val elem = idSeccionRaw ?: seccionRaw ?: return null
        return try {
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                obj.get("nombre_seccion")?.asString ?: obj.get("nombre")?.asString
            } else null
        } catch (_: Exception) { null }
    }
}

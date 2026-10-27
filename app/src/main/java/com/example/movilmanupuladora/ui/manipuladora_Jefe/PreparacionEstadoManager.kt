package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Context
import java.text.Normalizer

object PreparacionEstadoManager {

    private const val PREFS_NAME = "preparacion_estados_prefs"

    const val ESTADO_POR_HACER = "Por hacer"
    const val ESTADO_EN_PROCESO = "En proceso"
    const val ESTADO_FINALIZADA = "Finalizada"

    private fun normalizar(texto: String?): String {
        if (texto.isNullOrBlank()) return ""
        val sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return sinTildes.lowercase().trim()
            .replace("@", "_")
            .replace(".", "_")
            .replace(" ", "_")
    }

    /**
     * Guarda el estado actual de la preparación tanto por componente como por persona y correo.
     * Funciona en tiempo real a través de SharedPreferences compartidas en la app.
     */
    fun guardarEstado(
        context: Context,
        persona: String,
        componente: String? = null,
        estado: String,
        correo: String? = null
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // 1. Guardar por persona (nombre completo)
        val keyPersona = "est_per_" + normalizar(persona)
        editor.putString(keyPersona, estado)

        // 2. Guardar por primer nombre (ej: "Andrea", "Isa")
        val primerNombre = persona.trim().split(" ").firstOrNull() ?: ""
        if (primerNombre.isNotBlank()) {
            editor.putString("est_per_" + normalizar(primerNombre), estado)
        }

        // 3. Guardar por correo si existe
        if (!correo.isNullOrBlank()) {
            editor.putString("est_per_" + normalizar(correo), estado)
        }

        // 4. Guardar por componente
        if (!componente.isNullOrBlank()) {
            val keyComp = "est_comp_" + normalizar(componente)
            editor.putString(keyComp, estado)
        }

        // Guardar también en SIRAE_JEFA / ManipuladoraJefe para sincronización instantánea
        val jefaPrefs = context.getSharedPreferences("ManipuladoraJefe", Context.MODE_PRIVATE)
        jefaPrefs.edit()
            .putString("estado_comp_" + normalizar(persona), estado)
            .apply()

        editor.apply()
    }

    /**
     * Obtiene el estado actual de la preparación de una persona o de su componente.
     * Si no tiene estado previo, devuelve "Por hacer".
     */
    fun obtenerEstado(
        context: Context,
        persona: String,
        componente: String? = null,
        correo: String? = null
    ): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Buscar por correo
        if (!correo.isNullOrBlank()) {
            val estCorreo = prefs.getString("est_per_" + normalizar(correo), null)
            if (!estCorreo.isNullOrBlank()) return estCorreo
        }

        // 2. Buscar por persona (nombre completo)
        val keyPersona = "est_per_" + normalizar(persona)
        val estadoPersona = prefs.getString(keyPersona, null)
        if (!estadoPersona.isNullOrBlank()) {
            return estadoPersona
        }

        // 3. Buscar por primer nombre
        val primerNombre = persona.trim().split(" ").firstOrNull() ?: ""
        if (primerNombre.isNotBlank()) {
            val estPrimerNombre = prefs.getString("est_per_" + normalizar(primerNombre), null)
            if (!estPrimerNombre.isNullOrBlank()) return estPrimerNombre
        }

        // 4. Buscar por componente
        if (!componente.isNullOrBlank()) {
            val keyComp = "est_comp_" + normalizar(componente)
            val estadoComp = prefs.getString(keyComp, null)
            if (!estadoComp.isNullOrBlank()) {
                return estadoComp
            }
        }

        return ESTADO_POR_HACER
    }

    /**
     * Limpia los estados de preparación (por ejemplo al cambiar de día o menú)
     */
    fun reiniciarEstados(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}


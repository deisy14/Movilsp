package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AsistenciaManager {

    private const val PREFS_NAME = "asistencia_prefs"
    private const val KEY_MANANA = "asistencia_manana"
    private const val KEY_TARDE = "asistencia_tarde"
    private const val KEY_PRIMARIA = "asistencia_primaria"
    private const val KEY_SECUNDARIA = "asistencia_secundaria"
    private const val KEY_FECHA_DETALLE = "fecha_detalle_grados"

    // Claves por grado
    private const val KEY_JAR = "grado_jar"
    private const val KEY_P1 = "grado_p1"
    private const val KEY_P2 = "grado_p2"
    private const val KEY_P3 = "grado_p3"
    private const val KEY_P4 = "grado_p4"
    private const val KEY_P5 = "grado_p5"

    private const val KEY_S6 = "grado_s6"
    private const val KEY_S7 = "grado_s7"
    private const val KEY_S8 = "grado_s8"
    private const val KEY_S9 = "grado_s9"
    private const val KEY_S10 = "grado_s10"
    private const val KEY_S11 = "grado_s11"

    private fun obtenerFechaHoy(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // =========================================================
    // GUARDAR Y OBTENER DETALLE POR GRADO (Permite vacíos)
    // =========================================================

    fun guardarDetalleGradosHoy(context: Context, detalle: Map<String, String>) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putString(KEY_FECHA_DETALLE, obtenerFechaHoy())
        editor.putString(KEY_JAR, detalle["jar"].orEmpty())
        editor.putString(KEY_P1, detalle["p1"].orEmpty())
        editor.putString(KEY_P2, detalle["p2"].orEmpty())
        editor.putString(KEY_P3, detalle["p3"].orEmpty())
        editor.putString(KEY_P4, detalle["p4"].orEmpty())
        editor.putString(KEY_P5, detalle["p5"].orEmpty())

        editor.putString(KEY_S6, detalle["s6"].orEmpty())
        editor.putString(KEY_S7, detalle["s7"].orEmpty())
        editor.putString(KEY_S8, detalle["s8"].orEmpty())
        editor.putString(KEY_S9, detalle["s9"].orEmpty())
        editor.putString(KEY_S10, detalle["s10"].orEmpty())
        editor.putString(KEY_S11, detalle["s11"].orEmpty())
        editor.apply()
    }

    fun obtenerDetalleGradosHoy(context: Context): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fechaGuardada = prefs.getString(KEY_FECHA_DETALLE, null)
        val fechaHoy = obtenerFechaHoy()

        // Si la fecha guardada es de otro día anterior, los campos inician vacíos
        if (fechaGuardada != fechaHoy) {
            return emptyMap()
        }

        return mapOf(
            "jar" to (prefs.getString(KEY_JAR, "") ?: ""),
            "p1" to (prefs.getString(KEY_P1, "") ?: ""),
            "p2" to (prefs.getString(KEY_P2, "") ?: ""),
            "p3" to (prefs.getString(KEY_P3, "") ?: ""),
            "p4" to (prefs.getString(KEY_P4, "") ?: ""),
            "p5" to (prefs.getString(KEY_P5, "") ?: ""),
            "s6" to (prefs.getString(KEY_S6, "") ?: ""),
            "s7" to (prefs.getString(KEY_S7, "") ?: ""),
            "s8" to (prefs.getString(KEY_S8, "") ?: ""),
            "s9" to (prefs.getString(KEY_S9, "") ?: ""),
            "s10" to (prefs.getString(KEY_S10, "") ?: ""),
            "s11" to (prefs.getString(KEY_S11, "") ?: "")
        )
    }

    // =========================================================
    // GUARDAR Y OBTENER PRIMARIA (TRANSICIÓN A 5°)
    // =========================================================

    fun guardarPrimaria(context: Context, cantidad: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_PRIMARIA, cantidad)
            .apply()
    }

    fun obtenerPrimaria(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PRIMARIA, 0)
    }

    // =========================================================
    // GUARDAR Y OBTENER SECUNDARIA (6° A 11°)
    // =========================================================

    fun guardarSecundaria(context: Context, cantidad: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SECUNDARIA, cantidad)
            .apply()
    }

    fun obtenerSecundaria(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SECUNDARIA, 0)
    }

    // =========================================================
    // ASISTENCIA POR JORNADA
    // =========================================================

    fun guardarManana(context: Context, cantidad: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MANANA, cantidad)
            .apply()
    }

    fun guardarTarde(context: Context, cantidad: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TARDE, cantidad)
            .apply()
    }

    fun obtenerManana(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MANANA, 0)
    }

    fun obtenerTarde(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TARDE, 0)
    }

    // =========================================================
    // BORRAR ASISTENCIA
    // =========================================================

    fun limpiarAsistencia(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
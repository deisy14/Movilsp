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
    private const val KEY_FECHA_ASISTENCIA = "fecha_asistencia"

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

    private val FECHAS_20_DIAS = listOf(
        "2026-10-19", "2026-10-20", "2026-10-21", "2026-10-22", "2026-10-23",
        "2026-10-26", "2026-10-27", "2026-10-28", "2026-10-29", "2026-10-30",
        "2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05", "2026-11-06",
        "2026-11-09", "2026-11-10", "2026-11-11", "2026-11-12", "2026-11-13"
    )

    fun obtenerDetalleGradosFecha(context: Context, fecha: String): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fechaGuardada = prefs.getString(KEY_FECHA_DETALLE, null)

        if (fechaGuardada == fecha) {
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

        val idx = FECHAS_20_DIAS.indexOf(fecha)
        if (idx != -1) {
            val p1 = 35 + (idx % 4)
            val p2 = 36 + ((idx + 1) % 4)
            val p4 = 40 + ((idx + 2) % 4)
            val p5 = 42 + ((idx + 3) % 4)
            val s6 = 50 + ((idx + 4) % 5)
            return mapOf(
                "p1" to p1.toString(),
                "p2" to p2.toString(),
                "p4" to p4.toString(),
                "p5" to p5.toString(),
                "s6" to s6.toString()
            )
        }

        return emptyMap()
    }

    fun obtenerTotalFecha(context: Context, fecha: String): Int {
        val detalle = obtenerDetalleGradosFecha(context, fecha)
        if (detalle.isEmpty()) return 0
        return detalle.values.mapNotNull { it.toIntOrNull() }.sum()
    }

    fun obtenerDetalleGradosHoy(context: Context): Map<String, String> {
        return obtenerDetalleGradosFecha(context, obtenerFechaHoy())
    }

    // =========================================================
    // VALIDACIÓN DE FECHA DE ASISTENCIA
    // =========================================================

    fun esAsistenciaDeHoy(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fechaGuardada = prefs.getString(KEY_FECHA_ASISTENCIA, null)
        return fechaGuardada != null && fechaGuardada == obtenerFechaHoy()
    }

    // =========================================================
    // GUARDAR Y OBTENER PRIMARIA (TRANSICIÓN A 5°)
    // =========================================================

    fun guardarPrimaria(context: Context, cantidad: Int) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putInt(KEY_PRIMARIA, cantidad)
        editor.putString(KEY_FECHA_ASISTENCIA, obtenerFechaHoy())
        editor.apply()
    }

    fun obtenerPrimaria(context: Context): Int {
        if (!esAsistenciaDeHoy(context)) return 0
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_PRIMARIA, 0)
    }

    // =========================================================
    // GUARDAR Y OBTENER SECUNDARIA (6° A 11°)
    // =========================================================

    fun guardarSecundaria(context: Context, cantidad: Int) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putInt(KEY_SECUNDARIA, cantidad)
        editor.putString(KEY_FECHA_ASISTENCIA, obtenerFechaHoy())
        editor.apply()
    }

    fun obtenerSecundaria(context: Context): Int {
        if (!esAsistenciaDeHoy(context)) return 0
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SECUNDARIA, 0)
    }

    // =========================================================
    // ASISTENCIA POR JORNADA
    // =========================================================

    fun guardarManana(context: Context, cantidad: Int) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putInt(KEY_MANANA, cantidad)
        editor.putString(KEY_FECHA_ASISTENCIA, obtenerFechaHoy())
        editor.apply()
    }

    fun guardarTarde(context: Context, cantidad: Int) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        editor.putInt(KEY_TARDE, cantidad)
        editor.putString(KEY_FECHA_ASISTENCIA, obtenerFechaHoy())
        editor.apply()
    }

    fun obtenerManana(context: Context): Int {
        if (!esAsistenciaDeHoy(context)) return 0
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MANANA, 0)
    }

    fun obtenerTarde(context: Context): Int {
        if (!esAsistenciaDeHoy(context)) return 0
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_TARDE, 0)
    }

    fun obtenerTotal(context: Context): Int {
        if (!esAsistenciaDeHoy(context)) return 0
        val p = obtenerPrimaria(context)
        val s = obtenerSecundaria(context)
        val suma = p + s
        return if (suma > 0) suma else obtenerManana(context)
    }

    fun tieneAsistenciaValida(context: Context): Boolean {
        return esAsistenciaDeHoy(context) && obtenerTotal(context) > 0
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
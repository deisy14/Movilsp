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
    // GUARDAR Y OBTENER DETALLE POR GRADO (Por Fecha Dinámica)
    // =========================================================

    fun guardarDetalleGradosFecha(context: Context, fecha: String, detalle: Map<String, String>) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        val prefix = "asist_${fecha}_"
        editor.putString(KEY_FECHA_DETALLE, fecha)
        editor.putString(prefix + KEY_JAR, detalle["jar"].orEmpty())
        editor.putString(prefix + KEY_P1, detalle["p1"].orEmpty())
        editor.putString(prefix + KEY_P2, detalle["p2"].orEmpty())
        editor.putString(prefix + KEY_P3, detalle["p3"].orEmpty())
        editor.putString(prefix + KEY_P4, detalle["p4"].orEmpty())
        editor.putString(prefix + KEY_P5, detalle["p5"].orEmpty())

        editor.putString(prefix + KEY_S6, detalle["s6"].orEmpty())
        editor.putString(prefix + KEY_S7, detalle["s7"].orEmpty())
        editor.putString(prefix + KEY_S8, detalle["s8"].orEmpty())
        editor.putString(prefix + KEY_S9, detalle["s9"].orEmpty())
        editor.putString(prefix + KEY_S10, detalle["s10"].orEmpty())
        editor.putString(prefix + KEY_S11, detalle["s11"].orEmpty())
        editor.apply()
    }

    fun guardarDetalleGradosHoy(context: Context, detalle: Map<String, String>) {
        guardarDetalleGradosFecha(context, obtenerFechaHoy(), detalle)
    }

    fun obtenerDetalleGradosFecha(context: Context, fecha: String): Map<String, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefix = "asist_${fecha}_"

        val jar = prefs.getString(prefix + KEY_JAR, "") ?: ""
        val p1 = prefs.getString(prefix + KEY_P1, "") ?: ""
        val p2 = prefs.getString(prefix + KEY_P2, "") ?: ""
        val p3 = prefs.getString(prefix + KEY_P3, "") ?: ""
        val p4 = prefs.getString(prefix + KEY_P4, "") ?: ""
        val p5 = prefs.getString(prefix + KEY_P5, "") ?: ""
        val s6 = prefs.getString(prefix + KEY_S6, "") ?: ""
        val s7 = prefs.getString(prefix + KEY_S7, "") ?: ""
        val s8 = prefs.getString(prefix + KEY_S8, "") ?: ""
        val s9 = prefs.getString(prefix + KEY_S9, "") ?: ""
        val s10 = prefs.getString(prefix + KEY_S10, "") ?: ""
        val s11 = prefs.getString(prefix + KEY_S11, "") ?: ""

        // Si se guardó específicamente para esta fecha
        if (jar.isNotBlank() || p1.isNotBlank() || p2.isNotBlank() || p3.isNotBlank() ||
            p4.isNotBlank() || p5.isNotBlank() || s6.isNotBlank() || s7.isNotBlank() ||
            s8.isNotBlank() || s9.isNotBlank() || s10.isNotBlank() || s11.isNotBlank()) {
            return mapOf(
                "jar" to jar, "p1" to p1, "p2" to p2, "p3" to p3,
                "p4" to p4, "p5" to p5, "s6" to s6, "s7" to s7,
                "s8" to s8, "s9" to s9, "s10" to s10, "s11" to s11
            )
        }

        // Retrocompatibilidad con la fecha del día registrada previamente
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

        // Si no hay registro para esta fecha (ej. no hubo clases, día futuro o sin reporte): devuelve vacío
        return emptyMap()
    }

    fun obtenerTotalFecha(context: Context, fecha: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val p = prefs.getInt("asist_${fecha}_primaria", -1)
        val s = prefs.getInt("asist_${fecha}_secundaria", -1)
        if (p >= 0 || s >= 0) {
            return (p.coerceAtLeast(0)) + (s.coerceAtLeast(0))
        }
        val detalle = obtenerDetalleGradosFecha(context, fecha)
        if (detalle.isEmpty()) return 0
        return detalle.values.mapNotNull { it.toIntOrNull() }.sum()
    }

    fun guardarTotalFecha(context: Context, fecha: String, primaria: Int, secundaria: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("asist_${fecha}_primaria", primaria)
            .putInt("asist_${fecha}_secundaria", secundaria)
            .apply()
    }

    fun obtenerPrimariaFecha(context: Context, fecha: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getInt("asist_${fecha}_primaria", -1)
        if (saved >= 0) return saved
        val det = obtenerDetalleGradosFecha(context, fecha)
        if (det.isEmpty()) return 0
        return listOf("jar", "p1", "p2", "p3", "p4", "p5").sumOf { det[it]?.toIntOrNull() ?: 0 }
    }

    fun obtenerSecundariaFecha(context: Context, fecha: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getInt("asist_${fecha}_secundaria", -1)
        if (saved >= 0) return saved
        val det = obtenerDetalleGradosFecha(context, fecha)
        if (det.isEmpty()) return 0
        return listOf("s6", "s7", "s8", "s9", "s10", "s11").sumOf { det[it]?.toIntOrNull() ?: 0 }
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
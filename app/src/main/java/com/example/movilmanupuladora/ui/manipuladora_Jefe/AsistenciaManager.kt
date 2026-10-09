package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Context

object AsistenciaManager {

    private const val PREFS_NAME = "asistencia_prefs"
    private const val KEY_MANANA = "asistencia_manana"
    private const val KEY_TARDE = "asistencia_tarde"
    private const val KEY_PRIMARIA = "asistencia_primaria"
    private const val KEY_SECUNDARIA = "asistencia_secundaria"

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
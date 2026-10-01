package com.example.manipuladorajefe

import android.content.Context

object AsistenciaManager {

    private const val PREFS_NAME = "asistencia_prefs"

    private const val KEY_MANANA = "asistencia_manana"

    private const val KEY_TARDE = "asistencia_tarde"


    // =========================================================
    // GUARDAR ASISTENCIA DE LA MAÑANA
    // =========================================================

    fun guardarManana(
        context: Context,
        cantidad: Int
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putInt(
                KEY_MANANA,
                cantidad
            )
            .apply()
    }


    // =========================================================
    // GUARDAR ASISTENCIA DE LA TARDE
    // =========================================================

    fun guardarTarde(
        context: Context,
        cantidad: Int
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putInt(
                KEY_TARDE,
                cantidad
            )
            .apply()
    }


    // =========================================================
    // OBTENER ASISTENCIA DE LA MAÑANA
    // =========================================================

    fun obtenerManana(
        context: Context
    ): Int {

        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getInt(
                KEY_MANANA,
                0
            )
    }


    // =========================================================
    // OBTENER ASISTENCIA DE LA TARDE
    // =========================================================

    fun obtenerTarde(
        context: Context
    ): Int {

        return context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getInt(
                KEY_TARDE,
                0
            )
    }


    // =========================================================
    // BORRAR ASISTENCIA
    // =========================================================

    fun limpiarAsistencia(
        context: Context
    ) {

        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .clear()
            .apply()
    }
}
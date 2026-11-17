package com.example.movilmanupuladora.ui.manipuladora_Jefa

object PreparacionesManager {
    private val preparaciones = mutableListOf<Triple<String, String, Int>>()

    fun agregarPreparacion(menu: String, jornada: String, ninos: Int) {
        preparaciones.add(Triple(menu, jornada, ninos))
    }

    fun obtenerPreparaciones(): List<Triple<String, String, Int>> {
        return preparaciones
    }
}

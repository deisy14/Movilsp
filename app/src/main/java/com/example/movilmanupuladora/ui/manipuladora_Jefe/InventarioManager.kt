package com.example.manipuladorajefe

object InventarioManager {

    // =========================================================
    // INVENTARIO
    // =========================================================

    private val inventario =
        mutableMapOf<String, Double>()

    // =========================================================
    // MOVIMIENTOS
    // =========================================================

    private val movimientos =
        mutableListOf<String>()

    // =========================================================
    // GUARDAR CANTIDAD
    // =========================================================

    fun guardarCantidad(
        alimento: String,
        cantidad: Double,
        jornada: String
    ) {

        inventario[alimento] =
            cantidad

        val jornadaTexto =
            if (jornada == "MANANA") {
                "mañana"
            } else {
                "tarde"
            }

        movimientos.add(
            "Entrega registrada\n" +
                    "Jornada: $jornadaTexto\n" +
                    "Alimento: $alimento\n" +
                    "Cantidad: $cantidad kg"
        )
    }

    // =========================================================
    // OBTENER CANTIDAD
    // =========================================================

    fun obtenerCantidad(
        alimento: String
    ): Double {

        return inventario[alimento]
            ?: 0.0
    }

    // =========================================================
    // OBTENER TODO EL INVENTARIO
    // =========================================================

    fun obtenerInventario():
            Map<String, Double> {

        return inventario.toMap()
    }

    // =========================================================
    // OBTENER MOVIMIENTOS
    // =========================================================

    fun obtenerMovimientos():
            List<String> {

        return movimientos.toList()
    }

    // =========================================================
    // LIMPIAR INVENTARIO
    // =========================================================

    fun limpiarInventario() {

        inventario.clear()

        movimientos.clear()
    }
}
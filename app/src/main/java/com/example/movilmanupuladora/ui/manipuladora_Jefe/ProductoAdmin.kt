package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R
import java.util.Locale

data class ProductoAdmin(
    val id: Int,
    val nombre: String,
    var cantidad: Double,
    val unidadMedida: String,
    val categoria: String, // "Granos", "Lácteos", "Verduras", etc.
    val descripcion: String = "",
    val imagenRes: Int = R.drawable.ic_food,
    val imagenUrl: String? = null
) {
    /**
     * Muestra la cantidad y la unidad de medida JUNTOS (ejemplo: 26K, 7L, 55K, 500u)
     */
    val cantidadConUnidad: String
        get() {
            val cantStr = if (cantidad % 1.0 == 0.0) {
                cantidad.toInt().toString()
            } else {
                String.format(Locale.US, "%.1f", cantidad)
            }

            val unidadAbrev = when (unidadMedida.lowercase().trim()) {
                "kg", "kilogramo", "kilogramos", "k" -> "K"
                "l", "litro", "litros", "ltr" -> "L"
                "ml", "mililitros" -> "ml"
                "u", "unidad", "unidades" -> "u"
                else -> unidadMedida
            }

            return "$cantStr$unidadAbrev"
        }
}

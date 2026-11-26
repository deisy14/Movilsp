package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Context
import com.example.movilmanupuladora.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object InventarioManager {

    private val gson = Gson()
    private const val PREFS_NAME = "INVENTARIO_JEFA_PREFS"
    private const val KEY_PRODUCTOS = "PRODUCTOS_ADMIN_LIST"

    // =========================================================
    // PRODUCTOS ADMIN EN PERSISTENCIA LOCAL
    // =========================================================

    fun obtenerProductos(context: Context): MutableList<ProductoAdmin> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PRODUCTOS, null)
        if (!json.isNullOrBlank()) {
            try {
                val type = object : TypeToken<MutableList<ProductoAdmin>>() {}.type
                val lista: MutableList<ProductoAdmin>? = gson.fromJson(json, type)
                if (!lista.isNullOrEmpty()) return lista
            } catch (_: Exception) {}
        }
        return mutableListOf()
    }

    fun guardarProductos(context: Context, lista: List<ProductoAdmin>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PRODUCTOS, gson.toJson(lista)).apply()
    }

    /**
     * Limpia un nombre de insumo eliminando paréntesis superfluos y espacios repetidos.
     */
    fun limpiarNombre(nombre: String): String {
        return nombre.replace("(", "")
            .replace(")", "")
            .replace("[", "")
            .replace("]", "")
            .trim()
            .replace("\\s+".toRegex(), " ")
    }

    /**
     * Agrega un nuevo producto o, si ya existe en el inventario, incrementa su cantidad.
     */
    fun agregarOIncrementarAlimentos(context: Context, itemsRecibidos: List<RegistrarEntregaActivity.ItemIngredienteVerificar>) {
        val lista = obtenerProductos(context)

        for (item in itemsRecibidos) {
            val nombreLimpio = limpiarNombre(item.nombre)
            val nombreKey = nombreLimpio.lowercase()

            val productoExistente = lista.firstOrNull { p ->
                val pKey = limpiarNombre(p.nombre).lowercase()
                pKey == nombreKey
            }

            if (productoExistente != null) {
                productoExistente.cantidad += item.cantidad
            } else {
                val maxId = (lista.maxOfOrNull { it.id } ?: 0) + 1
                val cat = deducirCategoria(nombreLimpio)
                lista.add(
                    ProductoAdmin(
                        id = maxId,
                        nombre = nombreLimpio,
                        cantidad = item.cantidad,
                        unidadMedida = item.unidad,
                        categoria = cat,
                        descripcion = item.descripcion.ifBlank { "Víveres recibidos por remisión" },
                        imagenRes = R.drawable.ic_food
                    )
                )
            }

            // También reflejarlo en el mapa en memoria de inventario
            inventario[nombreLimpio] = (inventario[nombreLimpio] ?: 0.0) + item.cantidad
        }

        guardarProductos(context, lista)
    }

    private fun deducirCategoria(nombre: String): String {
        val n = nombre.lowercase()
        return when {
            n.contains("leche") || n.contains("huevo") || n.contains("queso") || n.contains("yogur") -> "Lácteos"
            n.contains("tomate") || n.contains("cebolla") || n.contains("zanahoria") || n.contains("papa") || n.contains("verdura") || n.contains("ahuyama") || n.contains("cilantro") || n.contains("habichuela") -> "Verduras"
            n.contains("pollo") || n.contains("pechuga") || n.contains("carne") || n.contains("pescado") || n.contains("cerdo") || n.contains("atun") -> "Carnes"
            else -> "Granos"
        }
    }

    fun listaPorDefecto(): MutableList<ProductoAdmin> {
        return mutableListOf(
            ProductoAdmin(1, "Arroz", 20.0, "kg", "Granos", "Arroz blanco de primera", R.drawable.ic_food, idInventario = 23, idIngrediente = 3),
            ProductoAdmin(2, "Frijoles", 20.0, "kg", "Granos", "Frijol rojo seco", R.drawable.ic_food, idInventario = 24, idIngrediente = 5),
            ProductoAdmin(3, "Carne de res", 0.0, "kg", "Granos", "Carne magra refrigerada", R.drawable.ic_food, idInventario = 25, idIngrediente = 7),
            ProductoAdmin(4, "Aceite vegetal", 15.0, "L", "Granos", "Aceite vegetal puro", R.drawable.ic_food, idInventario = 20, idIngrediente = 2),
            ProductoAdmin(5, "Leche entera", 30.0, "L", "Lácteos", "Leche pasteurizada", R.drawable.ic_food, idInventario = 22, idIngrediente = 1),
            ProductoAdmin(6, "Huevos", 50.0, "u", "Lácteos", "Huevos frescos tipo A", R.drawable.ic_food, idInventario = 21, idIngrediente = 8),
            ProductoAdmin(7, "Tomate", 11.2, "kg", "Verduras", "Tomate chonto fresco", R.drawable.ic_food, idInventario = 26, idIngrediente = 4)
        )
    }

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

        inventario[alimento] = (inventario[alimento] ?: 0.0) + cantidad

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
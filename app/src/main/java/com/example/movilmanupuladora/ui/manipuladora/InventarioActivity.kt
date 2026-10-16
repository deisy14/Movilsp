package com.example.movilmanupuladora.ui.manipuladora

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityInventarioBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.ProductoAdmin
import com.example.movilmanupuladora.ui.manipuladora_Jefe.ProductoAdminAdapter
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventarioBinding
    private lateinit var adapter: ProductoManipuladoraAdapter

    // Categoría activa actualmente: null = Todos, "Granos", "Lácteos", "Verduras"
    private var categoriaFiltroActiva: String? = null

    // Lista en memoria de productos del inventario del Admin
    private val listaProductosAdmin = mutableListOf<ProductoAdmin>()

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        inicializarListaProductos()
        configurarRecyclerView()
        configurarBotonesCategorias()
        configurarBarraNavegacion()
        cargarDatosDesdeServidor()
    }

    override fun onResume() {
        super.onResume()
        RetrofitClient.authToken = sessionManager.fetchAuthToken()
        cargarDatosDesdeServidor()
    }

    /**
     * Carga inicial con los productos del inventario del Admin registrados en la base de datos
     */
    private fun inicializarListaProductos() {
        listaProductosAdmin.clear()

        // 1. GRANOS
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 1,
                idInventario = 23,
                idIngrediente = 3,
                nombre = "Arroz",
                cantidad = 20.0,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Arroz blanco seleccionado",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 2,
                idInventario = 25,
                idIngrediente = 12,
                nombre = "Frijoles",
                cantidad = 20.0,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Frijol bolón",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 3,
                idInventario = 27,
                idIngrediente = 2,
                nombre = "Carne de res",
                cantidad = 0.02,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Carne de res magra",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 5,
                idInventario = 24,
                idIngrediente = 7,
                nombre = "Aceite vegetal",
                cantidad = 15.0,
                unidadMedida = "L",
                categoria = "Granos",
                descripcion = "Aceite vegetal para cocina",
                imagenRes = R.drawable.ic_food
            )
        )

        // 2. LÁCTEOS
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 6,
                idInventario = 22,
                idIngrediente = 1,
                nombre = "Leche entera",
                cantidad = 30.0,
                unidadMedida = "L",
                categoria = "Lácteos",
                descripcion = "Leche pasteurizada",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 8,
                idInventario = 21,
                idIngrediente = 8,
                nombre = "Huevos",
                cantidad = 50.0,
                unidadMedida = "u",
                categoria = "Lácteos",
                descripcion = "Huevos frescos tipo A",
                imagenRes = R.drawable.ic_food
            )
        )

        // 3. VERDURAS
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 9,
                idInventario = 26,
                idIngrediente = 4,
                nombre = "Tomate",
                cantidad = 11.2,
                unidadMedida = "kg",
                categoria = "Verduras",
                descripcion = "Tomate fresco",
                imagenRes = R.drawable.ic_food
            )
        )
    }

    private fun configurarRecyclerView() {
        // Adaptador exclusivo de la manipuladora: solo lectura, sin botón de lápiz ni código de edición
        adapter = ProductoManipuladoraAdapter(listaProductosAdmin)
        binding.rvProductosAdmin.layoutManager = LinearLayoutManager(this)
        binding.rvProductosAdmin.adapter = adapter
        actualizarTextoConteo()
    }

    /**
     * Configuración de los 3 botones horizontales (Granos, Lácteos, Verduras)
     */
    private fun configurarBotonesCategorias() {
        binding.btnCategoriaGranos.setOnClickListener {
            alternarFiltroCategoria("Granos", binding.btnCategoriaGranos)
        }

        binding.btnCategoriaLacteos.setOnClickListener {
            alternarFiltroCategoria("Lácteos", binding.btnCategoriaLacteos)
        }

        binding.btnCategoriaVerduras.setOnClickListener {
            alternarFiltroCategoria("Verduras", binding.btnCategoriaVerduras)
        }
    }

    private fun alternarFiltroCategoria(categoria: String, botonPulsado: MaterialButton) {
        if (categoriaFiltroActiva == categoria) {
            // Deseleccionar: mostrar todos
            categoriaFiltroActiva = null
            restablecerEstilosBotones()
            adapter.filtrarPorCategoria(null)
            binding.txtTituloSeccionProductos.text = "Inventario del Administrador"
        } else {
            // Seleccionar esta categoría
            categoriaFiltroActiva = categoria
            restablecerEstilosBotones()
            aplicarEstiloBotonActivo(botonPulsado)
            adapter.filtrarPorCategoria(categoria)
            binding.txtTituloSeccionProductos.text = "Inventario: $categoria"
        }
        actualizarTextoConteo()
    }

    private fun restablecerEstilosBotones() {
        val colorVerde = Color.parseColor("#1B3317")
        val colorBorde = Color.parseColor("#CBD5E1")
        val colorBlanco = Color.WHITE

        val botones = listOf(
            binding.btnCategoriaGranos,
            binding.btnCategoriaLacteos,
            binding.btnCategoriaVerduras
        )

        for (btn in botones) {
            btn.backgroundTintList = ColorStateList.valueOf(colorBlanco)
            btn.setTextColor(colorVerde)
            btn.strokeColor = ColorStateList.valueOf(colorBorde)
            btn.strokeWidth = dpToPx(1.5f)
        }
    }

    private fun aplicarEstiloBotonActivo(btn: MaterialButton) {
        val colorVerdeActivo = Color.parseColor("#1B3317")
        val colorBlanco = Color.WHITE

        btn.backgroundTintList = ColorStateList.valueOf(colorVerdeActivo)
        btn.setTextColor(colorBlanco)
        btn.strokeWidth = 0
    }

    private fun actualizarTextoConteo() {
        val total = adapter.itemCount
        val desc = if (categoriaFiltroActiva == null) "Todos los insumos" else "$total en $categoriaFiltroActiva"
        binding.txtConteoProductos.text = "$total $desc"
    }

    private fun configurarBarraNavegacion() {
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.INVENTARIO
        )
    }

    /**
     * Sincroniza en tiempo real los datos frescos del inventario del Admin desde el backend
     */
    private fun cargarDatosDesdeServidor() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SessionManager.getToken(this@InventarioActivity)

                // 1. Obtener catálogo de ingredientes para nombres, categorías y unidades
                val ingRes = RetrofitClient.apiService.obtenerIngredientes()
                val mapIngredientes = if (ingRes.isSuccessful && !ingRes.body().isNullOrEmpty()) {
                    ingRes.body()!!.associateBy { it.idIngrediente }
                } else emptyMap()

                // 2. Consultar el inventario real del Administrador
                val invRes = RetrofitClient.apiService.obtenerInventario()
                if (invRes.isSuccessful && !invRes.body().isNullOrEmpty()) {
                    val itemsInventario = invRes.body()!!
                    val listaServidor = mutableListOf<ProductoAdmin>()

                    for (inv in itemsInventario) {
                        val stockServidor = inv.cantidadActual?.toDoubleOrNull()
                            ?: inv.cantidad
                            ?: 0.0

                        val ing = inv.idIngrediente?.let { mapIngredientes[it] }
                        val nombre = ing?.nombreIngrediente ?: "Insumo #${inv.idIngrediente ?: inv.idInventario}"
                        val catNombre = when (ing?.idCategoriaInventario) {
                            2, 7 -> "Lácteos"
                            4 -> "Granos"
                            5 -> "Verduras"
                            else -> "Granos"
                        }
                        val unidadStr = when (inv.idUnidadMedida ?: ing?.idUnidadMedida) {
                            1 -> "kg"
                            2, 8 -> "L"
                            3 -> "u"
                            else -> "kg"
                        }

                        listaServidor.add(
                            ProductoAdmin(
                                id = inv.idInventario,
                                idInventario = inv.idInventario,
                                idIngrediente = inv.idIngrediente,
                                nombre = nombre,
                                cantidad = stockServidor,
                                unidadMedida = unidadStr,
                                categoria = catNombre,
                                descripcion = ing?.descripcion ?: "",
                                imagenRes = R.drawable.ic_food
                            )
                        )
                    }

                    if (listaServidor.isNotEmpty()) {
                        listaProductosAdmin.clear()
                        listaProductosAdmin.addAll(listaServidor)
                    }
                }

                // 3. Refrescar la interfaz en el hilo principal
                withContext(Dispatchers.Main) {
                    adapter.actualizarLista(listaProductosAdmin)
                    if (categoriaFiltroActiva != null) {
                        adapter.filtrarPorCategoria(categoriaFiltroActiva)
                    }
                    actualizarTextoConteo()
                }

            } catch (e: Exception) {
                android.util.Log.e("INVENTARIO_SYNC_MANI", "Error al sincronizar inventario: ${e.message}")
            }
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
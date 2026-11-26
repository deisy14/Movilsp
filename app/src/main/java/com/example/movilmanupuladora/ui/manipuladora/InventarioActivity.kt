package com.example.movilmanupuladora.ui.manipuladora

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityInventarioBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.ProductoAdmin
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventarioBinding
    private lateinit var adapter: ProductoManipuladoraAdapter

    private val listaProductosAdmin = mutableListOf<ProductoAdmin>()
    private var categoriaFiltroActiva: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityInventarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, 0)
            insets
        }

        NavigationHelper.setupStickyTopBar(this, binding.main, lifecycleScope)

        inicializarListaLocalConValoresPredeterminados()
        configurarRecyclerView()
        configurarBotonesCategorias()
        configurarFiltrosResumen()
        configurarBarraNavegacion()

        cargarDatosDesdeServidor()
    }

    private fun inicializarListaLocalConValoresPredeterminados() {
        listaProductosAdmin.clear()
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 1,
                idInventario = 20,
                idIngrediente = 1,
                nombre = "Arroz",
                cantidad = 20.0,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Arroz de primera calidad",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 2,
                idInventario = 21,
                idIngrediente = 2,
                nombre = "Frijoles",
                cantidad = 20.0,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Frijoles rojos",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 3,
                idInventario = 22,
                idIngrediente = 3,
                nombre = "Carne de res",
                cantidad = 0.0,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Carne magra",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 4,
                idInventario = 23,
                idIngrediente = 5,
                nombre = "Aceite vegetal",
                cantidad = 15.0,
                unidadMedida = "L",
                categoria = "Granos",
                descripcion = "Aceite de cocina",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 5,
                idInventario = 24,
                idIngrediente = 7,
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
                id = 6,
                idInventario = 25,
                idIngrediente = 8,
                nombre = "Huevos",
                cantidad = 50.0,
                unidadMedida = "u",
                categoria = "Lácteos",
                descripcion = "Huevos tipo A",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 7,
                idInventario = 26,
                idIngrediente = 4,
                nombre = "Tomate y cebolla",
                cantidad = 12.0,
                unidadMedida = "kg",
                categoria = "Verduras",
                descripcion = "Verduras frescas",
                imagenRes = R.drawable.ic_food
            )
        )
    }

    private fun configurarRecyclerView() {
        adapter = ProductoManipuladoraAdapter(listaProductosAdmin)
        binding.rvProductosAdmin.layoutManager = LinearLayoutManager(this)
        binding.rvProductosAdmin.adapter = adapter
        actualizarResumenYBotones()
        actualizarTextoConteo()
    }

    private fun actualizarResumenYBotones() {
        val total = listaProductosAdmin.size
        val disponibles = listaProductosAdmin.count { it.cantidad > 0.0 }
        val agotados = total - disponibles

        val granosCount = listaProductosAdmin.count { it.categoria.equals("Granos", true) }
        val lacteosCount = listaProductosAdmin.count { it.categoria.equals("Lácteos", true) || it.categoria.equals("Lacteos", true) }
        val verdurasCount = listaProductosAdmin.count { it.categoria.equals("Verduras", true) }

        binding.txtTotalInventario?.text = total.toString()
        binding.txtDisponiblesInventario?.text = disponibles.toString()
        binding.txtAgotadosInventario?.text = agotados.toString()

        binding.tvConteoGranos?.text = granosCount.toString()
        binding.tvConteoLacteos?.text = lacteosCount.toString()
        binding.tvConteoVerduras?.text = verdurasCount.toString()

        binding.txtInsumosRegistrados?.text = "$total insumos registrados por administración"
        binding.txtConteoProductos?.text = "$total registrados"
    }

    private fun configurarBotonesCategorias() {
        val btnGranos = findViewById<View>(R.id.btnCategoriaGranosCard) ?: findViewById<View>(R.id.btnCategoriaGranos)
        val btnLacteos = findViewById<View>(R.id.btnCategoriaLacteosCard) ?: findViewById<View>(R.id.btnCategoriaLacteos)
        val btnVerduras = findViewById<View>(R.id.btnCategoriaVerdurasCard) ?: findViewById<View>(R.id.btnCategoriaVerduras)

        btnGranos?.setOnClickListener {
            alternarFiltroCategoria("Granos", btnGranos)
        }

        btnLacteos?.setOnClickListener {
            alternarFiltroCategoria("Lácteos", btnLacteos)
        }

        btnVerduras?.setOnClickListener {
            alternarFiltroCategoria("Verduras", btnVerduras)
        }
    }

    private fun configurarFiltrosResumen() {
        findViewById<View>(R.id.cardTotalInventario)?.setOnClickListener {
            categoriaFiltroActiva = null
            restablecerEstilosBotones()
            adapter.actualizarLista(listaProductosAdmin)
            binding.txtTituloSeccionProductos.text = "Insumos en Bodega"
            actualizarTextoConteo()
        }
        findViewById<View>(R.id.cardDisponiblesInventario)?.setOnClickListener {
            categoriaFiltroActiva = null
            restablecerEstilosBotones()
            val disponibles = listaProductosAdmin.filter { it.cantidad > 0.0 }
            adapter.actualizarLista(disponibles)
            binding.txtTituloSeccionProductos.text = "Insumos Disponibles"
            actualizarTextoConteo()
        }
        findViewById<View>(R.id.cardAgotadosInventario)?.setOnClickListener {
            categoriaFiltroActiva = null
            restablecerEstilosBotones()
            val agotados = listaProductosAdmin.filter { it.cantidad <= 0.0 }
            adapter.actualizarLista(agotados)
            binding.txtTituloSeccionProductos.text = "Insumos Agotados"
            actualizarTextoConteo()
        }
    }

    private fun alternarFiltroCategoria(categoria: String, vistaPulsada: View) {
        if (categoriaFiltroActiva == categoria) {
            categoriaFiltroActiva = null
            restablecerEstilosBotones()
            adapter.filtrarPorCategoria(null)
            binding.txtTituloSeccionProductos.text = "Insumos en Bodega"
        } else {
            categoriaFiltroActiva = categoria
            restablecerEstilosBotones()
            aplicarEstiloBotonActivo(vistaPulsada)
            adapter.filtrarPorCategoria(categoria)
            binding.txtTituloSeccionProductos.text = "Inventario: $categoria"
        }
        actualizarTextoConteo()
    }

    private fun restablecerEstilosBotones() {
        val colorBorde = Color.parseColor("#CBD5E1")
        val colorBlanco = Color.WHITE

        val tarjetas = listOfNotNull(
            findViewById<MaterialCardView>(R.id.btnCategoriaGranosCard),
            findViewById<MaterialCardView>(R.id.btnCategoriaLacteosCard),
            findViewById<MaterialCardView>(R.id.btnCategoriaVerdurasCard)
        )

        for (card in tarjetas) {
            card.setCardBackgroundColor(colorBlanco)
            card.strokeColor = colorBorde
            card.strokeWidth = dpToPx(1.0f)
        }
    }

    private fun aplicarEstiloBotonActivo(vista: View) {
        val colorBordeActivo = Color.parseColor("#F4B41F")
        val colorFondoActivo = Color.parseColor("#FEF9E7")

        if (vista is MaterialCardView) {
            vista.setCardBackgroundColor(colorFondoActivo)
            vista.strokeColor = colorBordeActivo
            vista.strokeWidth = dpToPx(2.0f)
        }
    }

    private fun actualizarTextoConteo() {
        val total = adapter.itemCount
        binding.txtConteoProductos.text = "$total registrados"
    }

    private fun configurarBarraNavegacion() {
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.INVENTARIO
        )
    }

    private fun cargarDatosDesdeServidor() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SessionManager.getToken(this@InventarioActivity)

                val ingRes = RetrofitClient.apiService.obtenerIngredientes()
                val mapIngredientes = if (ingRes.isSuccessful && !ingRes.body().isNullOrEmpty()) {
                    ingRes.body()!!.associateBy { it.obtenerIdReal() }
                } else {
                    emptyMap()
                }

                val invRes = RetrofitClient.apiService.obtenerInventario()
                if (invRes.isSuccessful && !invRes.body().isNullOrEmpty()) {
                    val itemsInventario = invRes.body()!!
                    val listaServidor = mutableListOf<ProductoAdmin>()

                    for (inv in itemsInventario) {
                        val stockServidor = inv.cantidadActual?.toDoubleOrNull()
                            ?: inv.cantidad
                            ?: 0.0

                        val idReal = inv.obtenerIdReal()
                        val idIng = inv.obtenerIdIngredienteReal()
                        val ing = idIng?.let { mapIngredientes[it] }

                        val nombre = inv.nombreIngrediente
                            ?: ing?.nombre
                            ?: "Insumo #${idIng ?: idReal}"

                        val catId = ing?.obtenerCategoriaReal()
                        val catNombre = when (catId) {
                            2, 7 -> "Lácteos"
                            4 -> "Granos"
                            5 -> "Verduras"
                            else -> "Granos"
                        }
                        val unidId = inv.obtenerIdUnidadReal() ?: ing?.obtenerUnidadReal()
                        val unidadStr = when (unidId) {
                            1 -> "kg"
                            2, 8 -> "L"
                            3 -> "u"
                            else -> "kg"
                        }

                        listaServidor.add(
                            ProductoAdmin(
                                id = idReal,
                                idInventario = idReal,
                                idIngrediente = idIng,
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

                withContext(Dispatchers.Main) {
                    actualizarResumenYBotones()
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

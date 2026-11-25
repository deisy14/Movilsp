package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityInventarioJefaBinding
import com.example.movilmanupuladora.utils.NavigationHelperJefa
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventarioJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventarioJefaBinding
    private lateinit var adapter: ProductoAdminAdapter

    // Categoría activa actualmente: null = Todos, "Granos", "Lácteos", "Verduras"
    private var categoriaFiltroActiva: String? = null

    // Lista en memoria de productos del inventario del Admin
    private val listaProductosAdmin = mutableListOf<ProductoAdmin>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-Edge: fondo se dibuja detrás de la barra de estado
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        binding = ActivityInventarioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.headerInventario) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight + (10 * resources.displayMetrics.density).toInt(),
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        inicializarListaProductos()
        configurarRecyclerView()
        configurarBotonesCategorias()
        configurarNavegacionInferior()
        cargarDatosDesdeServidor()
    }

    override fun onResume() {
        super.onResume()
        // Cargar inventario local actualizado (con alimentos recibidos de entregas aceptadas)
        val productosLocales = InventarioManager.obtenerProductos(this)
        listaProductosAdmin.clear()
        listaProductosAdmin.addAll(productosLocales)
        adapter.actualizarLista(listaProductosAdmin)
        if (categoriaFiltroActiva != null) {
            adapter.filtrarPorCategoria(categoriaFiltroActiva)
        }
        actualizarTextoConteo()

        // Cada vez que la jefa abre o regresa a la pantalla, se consulta el stock actualizado del Admin
        cargarDatosDesdeServidor()
    }

    /**
     * Carga inicial con los productos del inventario
     */
    private fun inicializarListaProductos() {
        listaProductosAdmin.clear()
        listaProductosAdmin.addAll(InventarioManager.obtenerProductos(this))
    }

    private fun configurarRecyclerView() {
        adapter = ProductoAdminAdapter(listaProductosAdmin) { producto ->
            mostrarDialogoEditarProducto(producto)
        }
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

    /**
     * Diálogo emergente que se abre al tocar el lápiz para editar el stock
     */
    private fun mostrarDialogoEditarProducto(producto: ProductoAdmin) {
        val dialogBinding = com.example.movilmanupuladora.databinding.DialogModificarStockBinding.inflate(layoutInflater)

        val valorActualStr = if (producto.cantidad % 1.0 == 0.0) {
            producto.cantidad.toInt().toString()
        } else {
            producto.cantidad.toString()
        }

        dialogBinding.txtDetallesStock.text = "Producto: ${producto.nombre}\nCantidad actual: ${producto.cantidadConUnidad}\nUnidad: ${producto.unidadMedida}"
        dialogBinding.edtNuevaCantidadStock.setText(valorActualStr)
        dialogBinding.edtNuevaCantidadStock.setSelection(dialogBinding.edtNuevaCantidadStock.text.length)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnGuardarStock.setOnClickListener {
            val nuevoValorStr = dialogBinding.edtNuevaCantidadStock.text.toString().trim()
            val nuevoValor = nuevoValorStr.toDoubleOrNull()
            if (nuevoValor != null && nuevoValor >= 0) {
                producto.cantidad = nuevoValor
                InventarioManager.guardarProductos(this, listaProductosAdmin)
                adapter.notifyDataSetChanged()
                actualizarTextoConteo()

                Toast.makeText(
                    this,
                    "Stock de ${producto.nombre} actualizado a ${producto.cantidadConUnidad}",
                    Toast.LENGTH_SHORT
                ).show()

                // Sincronizar con la API del backend para reflejarlo al Administrador
                val idInv = producto.idInventario
                if (idInv != null && idInv > 0) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            SessionManager.getToken(this@InventarioJefaActivity)
                            val payload = com.example.movilmanupuladora.data.model.inventario(
                                idInventario = idInv,
                                idIngrediente = producto.idIngrediente,
                                cantidadActual = String.format(java.util.Locale.US, "%.2f", nuevoValor),
                                stockMinimo = "4.00",
                                idUnidadMedida = when (producto.unidadMedida.lowercase().trim()) {
                                    "l", "litro", "litros" -> 2
                                    "u", "unidad", "unidades" -> 3
                                    else -> 1
                                }
                            )
                            val res = RetrofitClient.apiService.actualizarInventario(idInv, payload)
                            withContext(Dispatchers.Main) {
                                if (!res.isSuccessful) {
                                    android.util.Log.e("INVENTARIO_SYNC", "Error ${res.code()}: ${res.errorBody()?.string()}")
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("INVENTARIO_SYNC", "Error al conectar con servidor: ${e.message}")
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Por favor ingresa una cantidad numérica válida", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialogBinding.btnCancelarStock.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Sincroniza en tiempo real los datos frescos del inventario del Admin desde el backend
     */
    private fun cargarDatosDesdeServidor() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SessionManager.getToken(this@InventarioJefaActivity)

                // 1. Obtener catálogo de ingredientes para nombres, categorías y unidades
                val ingRes = RetrofitClient.apiService.obtenerIngredientes()
                val mapIngredientes = if (ingRes.isSuccessful && !ingRes.body().isNullOrEmpty()) {
                    ingRes.body()!!.associateBy { it.obtenerIdReal() }
                } else {
                    android.util.Log.e("INVENTARIO_SYNC", "obtenerIngredientes: HTTP ${ingRes.code()}")
                    emptyMap()
                }

                // 2. Consultar el inventario real del Administrador
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

                        val nombre = ing?.obtenerNombreReal()
                            ?.takeIf { it.isNotBlank() }
                            ?: inv.nombreIngrediente
                            ?: inv.nombre
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
                        // Preservar productos e incrementos locales originados por remisiones recibidas
                        val locales = InventarioManager.obtenerProductos(this@InventarioJefaActivity)
                        for (pLocal in locales) {
                            val localLimpio = InventarioManager.limpiarNombre(pLocal.nombre)
                            val encontrado = listaServidor.find { 
                                InventarioManager.limpiarNombre(it.nombre).equals(localLimpio, ignoreCase = true) 
                            }
                            if (encontrado == null) {
                                listaServidor.add(pLocal.copy(nombre = localLimpio))
                            } else if (pLocal.cantidad > encontrado.cantidad) {
                                encontrado.cantidad = pLocal.cantidad
                            }
                        }
                        listaProductosAdmin.clear()
                        listaProductosAdmin.addAll(listaServidor)
                        InventarioManager.guardarProductos(this@InventarioJefaActivity, listaProductosAdmin)
                    }
                } else {
                    android.util.Log.e("INVENTARIO_SYNC", "obtenerInventario: HTTP ${invRes.code()}")
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
                android.util.Log.e("INVENTARIO_SYNC", "Error al sincronizar inventario del servidor: ${e.message}")
            }
        }
    }

    /**
     * Configuración de la barra de navegación exclusiva de la Jefa
     */
    private fun configurarNavegacionInferior() {
        NavigationHelperJefa.setupBarraNavegacionJefa(
            this,
            binding.barraNavegacionJefa,
            NavigationHelperJefa.TabJefa.INVENTARIO
        )
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
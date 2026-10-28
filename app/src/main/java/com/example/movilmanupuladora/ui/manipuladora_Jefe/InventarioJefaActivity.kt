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
        enableEdgeToEdge()

        binding = ActivityInventarioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        inicializarListaProductos()
        configurarRecyclerView()
        configurarBotonesCategorias()
        configurarNavegacionInferior()
        cargarDatosDesdeServidor()

        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Cada vez que la jefa abre o regresa a la pantalla, se consulta el stock actualizado del Admin
        cargarDatosDesdeServidor()
    }

    /**
     * Carga inicial con los productos reales del admin registrados en la base de datos
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
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val valorActualStr = if (producto.cantidad % 1.0 == 0.0) {
                producto.cantidad.toInt().toString()
            } else {
                producto.cantidad.toString()
            }
            setText(valorActualStr)
            setSelection(text.length)
            hint = "Nueva cantidad"
            textSize = 18f
            setTextColor(Color.parseColor("#1B3317"))
            setPadding(dpToPx(16f), dpToPx(12f), dpToPx(16f), dpToPx(12f))
        }

        val container = FrameLayout(this).apply {
            setPadding(dpToPx(24f), dpToPx(12f), dpToPx(24f), dpToPx(8f))
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Modificar Stock")
            .setMessage("Producto: ${producto.nombre}\nCantidad actual: ${producto.cantidadConUnidad}\nUnidad: ${producto.unidadMedida}\n\nIngresa la nueva cantidad:")
            .setView(container)
            .setPositiveButton("Guardar") { dialog, _ ->
                val nuevoValorStr = input.text.toString().trim()
                val nuevoValor = nuevoValorStr.toDoubleOrNull()
                if (nuevoValor != null && nuevoValor >= 0) {
                    producto.cantidad = nuevoValor
                    adapter.notifyDataSetChanged()
                    actualizarTextoConteo()

                    Toast.makeText(
                        this,
                        "¡Stock de ${producto.nombre} actualizado a ${producto.cantidadConUnidad}!",
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
                                        if (res.code() == 403) {
                                            Toast.makeText(
                                                this@InventarioJefaActivity,
                                                "Guardado en la app. El servidor requiere permiso de Supervisor/Admin para reflejarlo en la web.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
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
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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
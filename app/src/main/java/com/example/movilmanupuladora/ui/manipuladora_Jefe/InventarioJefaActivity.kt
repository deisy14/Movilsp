package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InventarioJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventarioJefaBinding
    private lateinit var adapter: ProductoAdminAdapter
    private lateinit var sessionManager: SessionManager

    // Categoría activa: null = Todas
    private var categoriaFiltroActiva: String? = null
    private var textoBusqueda: String = ""

    // Lista dinámica de productos del backend
    private val listaProductosAdmin = mutableListOf<ProductoAdmin>()
    // Lista dinámica de categorías del backend
    private val categoriasBackend = mutableListOf<String>()
    private val botonesCategorias = mutableListOf<Pair<String?, MaterialButton>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        binding = ActivityInventarioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.headerInventario) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(
                view.paddingLeft,
                statusBarHeight + dpToPx(10f),
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        configurarHeader()
        configurarRecyclerView()
        configurarBuscador()
        configurarCarruselFlechas()
        configurarNavegacionInferior()

        // Cargar caché local solo si existe mientras responde el servidor
        val locales = InventarioManager.obtenerProductos(this)
        if (locales.isNotEmpty()) {
            listaProductosAdmin.clear()
            listaProductosAdmin.addAll(locales)
            adapter.actualizarLista(listaProductosAdmin)
            actualizarCategoriasDesdeProductosSiVacio()
        }
        actualizarEstadoVacio()
    }

    override fun onResume() {
        super.onResume()
        actualizarBadgeNotificaciones()
        cargarDatosDesdeServidor()
    }

    private fun configurarHeader() {
        val sedeNombre = sessionManager.getSedeNombre()?.takeIf { it.isNotBlank() } ?: "Sede asignada"
        binding.txtJornadaTitulo.text = sedeNombre

        val nombreUsuario = sessionManager.getUserName()?.trim().orEmpty()
        val iniciales = nombreUsuario
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "PA" }
        binding.txtInicialesPerfilInv.text = iniciales

        binding.btnNotificacionesInv.setOnClickListener {
            startActivity(Intent(this, HistorialNotificacionesJefaActivity::class.java))
        }
        binding.btnPerfilInv.setOnClickListener {
            startActivity(Intent(this, PerfilJefaActivity::class.java))
        }
    }

    private fun actualizarBadgeNotificaciones() {
        val noLeidas = NotificacionesJefaManager.obtenerConteoNoLeidas(this)
        if (noLeidas > 0) {
            binding.badgeNotificacionesInv.visibility = View.VISIBLE
            binding.badgeNotificacionesInv.text = if (noLeidas > 99) "99+" else noLeidas.toString()
        } else {
            binding.badgeNotificacionesInv.visibility = View.GONE
        }
    }

    private fun configurarRecyclerView() {
        adapter = ProductoAdminAdapter(listaProductosAdmin, modoSoloLectura = true)
        binding.rvProductosAdmin.layoutManager = LinearLayoutManager(this)
        binding.rvProductosAdmin.adapter = adapter
    }

    private fun configurarBuscador() {
        binding.edtBuscarInsumo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textoBusqueda = s?.toString().orEmpty()
                adapter.aplicarFiltros(categoriaFiltroActiva, textoBusqueda)
                actualizarEstadoVacio()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun configurarCarruselFlechas() {
        binding.btnCatPrev.setOnClickListener {
            binding.scrollCategorias.smoothScrollBy(-dpToPx(160f), 0)
        }
        binding.btnCatNext.setOnClickListener {
            binding.scrollCategorias.smoothScrollBy(dpToPx(160f), 0)
        }
    }

    /**
     * Construye dinámicamente las píldoras del carrusel con "Todas" + todas las categorías del backend.
     * Botón activo: fondo amarillo #F4B41F y texto BLANCO #FFFFFF.
     */
    private fun renderizarCarruselCategorias(categorias: List<String>) {
        binding.contenedorCategoriasCarrusel.removeAllViews()
        botonesCategorias.clear()

        val opciones = mutableListOf<Pair<String?, String>>()
        opciones.add(null to "Todas")
        for (cat in categorias.distinct()) {
            if (cat.isNotBlank()) {
                opciones.add(cat to cat)
            }
        }

        for ((valorFiltro, etiqueta) in opciones) {
            val btn = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = etiqueta
                isAllCaps = false
                textSize = 13f
                cornerRadius = dpToPx(20f)
                insetTop = 0
                insetBottom = 0
                minHeight = dpToPx(38f)
                minimumHeight = dpToPx(38f)
                setPadding(dpToPx(18f), 0, dpToPx(18f), 0)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dpToPx(38f)
                )
                params.marginEnd = dpToPx(8f)
                layoutParams = params

                setOnClickListener {
                    categoriaFiltroActiva = valorFiltro
                    actualizarEstilosCarrusel()
                    adapter.aplicarFiltros(categoriaFiltroActiva, textoBusqueda)
                    actualizarEstadoVacio()
                }
            }
            botonesCategorias.add(valorFiltro to btn)
            binding.contenedorCategoriasCarrusel.addView(btn)
        }

        actualizarEstilosCarrusel()
    }

    private fun actualizarEstilosCarrusel() {
        for ((valorFiltro, btn) in botonesCategorias) {
            val activo = (categoriaFiltroActiva == null && valorFiltro == null) ||
                (categoriaFiltroActiva != null && valorFiltro != null &&
                    categoriaFiltroActiva.equals(valorFiltro, ignoreCase = true))
            if (activo) {
                btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F4B41F"))
                btn.setTextColor(Color.WHITE)
                btn.strokeWidth = 0
            } else {
                btn.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                btn.setTextColor(Color.parseColor("#0F172A"))
                btn.strokeColor = ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                btn.strokeWidth = dpToPx(1f)
            }
        }
    }

    private fun actualizarCategoriasDesdeProductosSiVacio() {
        if (categoriasBackend.isEmpty() && listaProductosAdmin.isNotEmpty()) {
            val cats = listaProductosAdmin.map { it.categoria }.filter { it.isNotBlank() }.distinct()
            renderizarCarruselCategorias(cats)
        }
    }

    private fun actualizarEstadoVacio() {
        val hayItems = adapter.itemCount > 0
        binding.rvProductosAdmin.visibility = if (hayItems) View.VISIBLE else View.GONE
        binding.layoutSinInsumos.visibility = if (hayItems) View.GONE else View.VISIBLE
    }

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
                adapter.actualizarLista(listaProductosAdmin)
                actualizarEstadoVacio()

                Toast.makeText(
                    this,
                    "Stock de ${producto.nombre} actualizado",
                    Toast.LENGTH_SHORT
                ).show()

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
                                    "u", "und", "unidad", "unidades" -> 3
                                    else -> 1
                                }
                            )
                            RetrofitClient.apiService.actualizarInventario(idInv, payload)
                        } catch (_: Exception) { }
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
     * Consulta en paralelo categorías, unidades de medida, ingredientes (con URLs de Cloudinary)
     * e inventario real del backend, sin datos quemados.
     */
    private fun cargarDatosDesdeServidor() {
        if (listaProductosAdmin.isEmpty()) {
            binding.progressInventario.visibility = View.VISIBLE
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SessionManager.getToken(this@InventarioJefaActivity)

                val catDef = async { runCatching { RetrofitClient.apiService.obtenerCategoriasInventario() }.getOrNull() }
                val unidDef = async { runCatching { RetrofitClient.apiService.obtenerUnidadesMedida() }.getOrNull() }
                val ingDef = async { runCatching { RetrofitClient.apiService.obtenerIngredientes() }.getOrNull() }
                val invDef = async { runCatching { RetrofitClient.apiService.obtenerInventario() }.getOrNull() }

                val catRes = catDef.await()
                val unidRes = unidDef.await()
                val ingRes = ingDef.await()
                val invRes = invDef.await()

                val mapCategorias = if (catRes?.isSuccessful == true && !catRes.body().isNullOrEmpty()) {
                    catRes.body()!!.associate { it.idCategoriaInventario to it.nombreCategoria.trim() }
                } else {
                    emptyMap()
                }

                val mapUnidades = if (unidRes?.isSuccessful == true && !unidRes.body().isNullOrEmpty()) {
                    unidRes.body()!!.associate {
                        it.idUnidadMedida to (it.abreviatura?.takeIf { a -> a.isNotBlank() } ?: it.nombreUnidad)
                    }
                } else {
                    emptyMap()
                }

                val mapIngredientes = if (ingRes?.isSuccessful == true && !ingRes.body().isNullOrEmpty()) {
                    ingRes.body()!!.associateBy { it.obtenerIdReal() }
                } else {
                    emptyMap()
                }

                val listaServidor = mutableListOf<ProductoAdmin>()
                if (invRes?.isSuccessful == true && !invRes.body().isNullOrEmpty()) {
                    val sedeIdActual = sessionManager.getSedeId().takeIf { it > 0 }
                    val itemsInventario = invRes.body()!!.filter { inv ->
                        sedeIdActual == null || inv.idSede == null || inv.idSede == sedeIdActual
                    }

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

                        val ingPorNombre = ing ?: mapIngredientes.values.firstOrNull {
                            InventarioManager.limpiarNombre(it.obtenerNombreReal())
                                .equals(InventarioManager.limpiarNombre(nombre), ignoreCase = true)
                        }

                        val catId = ingPorNombre?.obtenerCategoriaReal()
                        val catNombre = catId?.let { mapCategorias[it] } ?: "General"

                        val unidId = inv.obtenerIdUnidadReal() ?: ingPorNombre?.obtenerUnidadReal()
                        val unidadStr = unidId?.let { mapUnidades[it] } ?: "Kg"

                        val imagenCloudinary = ingPorNombre?.imagenIngrediente?.takeIf { it.isNotBlank() }

                        listaServidor.add(
                            ProductoAdmin(
                                id = idReal,
                                idInventario = idReal,
                                idIngrediente = idIng ?: ingPorNombre?.obtenerIdReal(),
                                nombre = nombre,
                                cantidad = stockServidor,
                                unidadMedida = unidadStr,
                                categoria = catNombre,
                                descripcion = ingPorNombre?.descripcion ?: "",
                                imagenRes = R.drawable.ic_food,
                                imagenUrl = imagenCloudinary
                            )
                        )
                    }
                }

                // Si el backend respondió con inventario real, usarlo directamente (sin arrastrar datos quemados viejos)
                val locales = InventarioManager.obtenerProductos(this@InventarioJefaActivity)
                val listaFinal = if (listaServidor.isNotEmpty()) {
                    listaServidor
                } else {
                    locales.map { p ->
                        val matchIng = mapIngredientes.values.firstOrNull {
                            InventarioManager.limpiarNombre(it.obtenerNombreReal())
                                .equals(InventarioManager.limpiarNombre(p.nombre), ignoreCase = true)
                        }
                        if (matchIng != null && !matchIng.imagenIngrediente.isNullOrBlank()) {
                            p.copy(imagenUrl = matchIng.imagenIngrediente)
                        } else {
                            p
                        }
                    }.toMutableList()
                }

                // Lista final de categorías reales del backend
                val catsOrdenadas = if (mapCategorias.isNotEmpty()) {
                    mapCategorias.values.filter { it.isNotBlank() }.distinct()
                } else {
                    listaFinal.map { it.categoria }.filter { it.isNotBlank() }.distinct()
                }

                withContext(Dispatchers.Main) {
                    binding.progressInventario.visibility = View.GONE
                    categoriasBackend.clear()
                    categoriasBackend.addAll(catsOrdenadas)
                    renderizarCarruselCategorias(categoriasBackend)

                    listaProductosAdmin.clear()
                    listaProductosAdmin.addAll(listaFinal)
                    InventarioManager.guardarProductos(this@InventarioJefaActivity, listaProductosAdmin)

                    adapter.actualizarLista(listaProductosAdmin)
                    actualizarEstadoVacio()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressInventario.visibility = View.GONE
                    actualizarEstadoVacio()
                }
            }
        }
    }

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

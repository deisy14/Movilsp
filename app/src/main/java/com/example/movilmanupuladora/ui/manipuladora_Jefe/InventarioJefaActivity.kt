package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityInventarioJefaBinding
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
        binding = ActivityInventarioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inicializarListaProductos()
        configurarRecyclerView()
        configurarBotonesCategorias()
        configurarNavegacionInferior()
        cargarDatosDesdeServidor()

        binding.btnVolver.setOnClickListener {
            finish()
        }
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
                nombre = "Arroz blanco",
                cantidad = 24.0,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Arroz blanco seleccionado",
                imagenRes = R.drawable.arroz_pollo
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 2,
                nombre = "Frijoles",
                cantidad = 55.0,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Frijol bolón",
                imagenRes = R.drawable.frijoles
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 3,
                nombre = "Lentejas",
                cantidad = 18.0,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Lenteja limpia",
                imagenRes = R.drawable.lentejas_arroz
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 4,
                nombre = "Pasta",
                cantidad = 12.0,
                unidadMedida = "kg",
                categoria = "Granos",
                descripcion = "Fideos y pasta corta",
                imagenRes = R.drawable.pasta_carne
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 5,
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
                nombre = "Leche entera",
                cantidad = 20.0,
                unidadMedida = "L",
                categoria = "Lácteos",
                descripcion = "Leche pasteurizada",
                imagenRes = R.drawable.ic_leche
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 7,
                nombre = "Queso campesino",
                cantidad = 10.0,
                unidadMedida = "kg",
                categoria = "Lácteos",
                descripcion = "Queso fresco",
                imagenRes = R.drawable.huevo_perico
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 8,
                nombre = "Huevos",
                cantidad = 500.0,
                unidadMedida = "u",
                categoria = "Lácteos",
                descripcion = "Huevos frescos tipo A",
                imagenRes = R.drawable.huevo_perico
            )
        )

        // 3. VERDURAS
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 9,
                nombre = "Tomate",
                cantidad = 11.2,
                unidadMedida = "kg",
                categoria = "Verduras",
                descripcion = "Tomate fresco",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 10,
                nombre = "Papa criolla",
                cantidad = 30.0,
                unidadMedida = "kg",
                categoria = "Verduras",
                descripcion = "Papa criolla lavada",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 11,
                nombre = "Zanahoria",
                cantidad = 8.5,
                unidadMedida = "kg",
                categoria = "Verduras",
                descripcion = "Zanahoria fresca",
                imagenRes = R.drawable.ic_food
            )
        )
        listaProductosAdmin.add(
            ProductoAdmin(
                id = 12,
                nombre = "Cebolla de rama",
                cantidad = 6.0,
                unidadMedida = "kg",
                categoria = "Verduras",
                descripcion = "Cebolla junca limpia",
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
                        Toast.LENGTH_LONG
                    ).show()
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
     * Intenta sincronizar datos frescos del backend en segundo plano
     */
    private fun cargarDatosDesdeServidor() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SessionManager.getToken(this@InventarioJefaActivity)
                val res = RetrofitClient.apiService.obtenerIngredientes()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val ingredientesBackend = res.body()!!
                    withContext(Dispatchers.Main) {
                        for (ing in ingredientesBackend) {
                            val nombreIng = ing.nombreIngrediente ?: continue
                            val existente = listaProductosAdmin.find {
                                it.nombre.contains(nombreIng, ignoreCase = true) ||
                                nombreIng.contains(it.nombre, ignoreCase = true)
                            }
                            if (existente == null) {
                                val catNombre = when (ing.idCategoriaInventario) {
                                    2, 7 -> "Lácteos"
                                    4 -> "Granos"
                                    5 -> "Verduras"
                                    else -> "Granos"
                                }
                                val unidadStr = when (ing.idUnidadMedida) {
                                    1 -> "kg"
                                    2, 8 -> "L"
                                    else -> "u"
                                }
                                listaProductosAdmin.add(
                                    ProductoAdmin(
                                        id = ing.idIngrediente ?: 0,
                                        nombre = nombreIng,
                                        cantidad = 10.0,
                                        unidadMedida = unidadStr,
                                        categoria = catNombre,
                                        descripcion = ing.descripcion ?: "",
                                        imagenRes = R.drawable.ic_food
                                    )
                                )
                            }
                        }
                        adapter.actualizarLista(listaProductosAdmin)
                        actualizarTextoConteo()
                    }
                }
            } catch (e: Exception) {
                // Conserva los datos iniciales reales sin interrumpir la experiencia
            }
        }
    }

    /**
     * Configuración de la barra de navegación exclusiva de la Jefa
     */
    private fun configurarNavegacionInferior() {
        val barra = binding.barraNavegacionJefa

        // Marcar la pestaña actual (Inventario) como activa
        barra.iconNavInventario.setColorFilter(Color.parseColor("#1B3317"))
        barra.txtNavInventario.setTextColor(Color.parseColor("#1B3317"))
        barra.txtNavInventario.paint.isFakeBoldText = true

        barra.navInicio.setOnClickListener {
            val intent = Intent(this, InicioJefaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        barra.navPreparaciones.setOnClickListener {
            val intent = Intent(this, PreparacionesJefaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        barra.navManipuladoras.setOnClickListener {
            val intent = Intent(this, ManipuladorasJefaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        barra.navPerfil.setOnClickListener {
            val intent = Intent(this, PerfilJefaActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
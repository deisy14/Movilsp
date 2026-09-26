package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.Ingrediente
import com.example.movilmanupuladora.data.model.inventario
import com.example.movilmanupuladora.data.repository.InventarioRepository
import com.example.movilmanupuladora.databinding.ActivityInventarioBinding
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class InventarioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventarioBinding

    private val inventarioRepository =
        InventarioRepository(RetrofitClient.apiService)

    private var listaInventario: List<inventario> = emptyList()

    // Mapa id_ingrediente -> Ingrediente
    private var mapaIngredientes: Map<Int, Ingrediente> = emptyMap()

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityInventarioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ==========================================
        // SESIÓN / TOKEN
        // ==========================================

        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        // ==========================================
        // BUSCADOR
        // ==========================================

        binding.edtBuscarIngrediente.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filtrarInventario(s?.toString() ?: "")
                }

                override fun afterTextChanged(s: Editable?) {
                }
            }
        )

        // ==========================================
        // CATEGORÍAS
        // ==========================================

        binding.btnLacteos.setOnClickListener {
            activarCategoria(binding.btnLacteos)
        }

        binding.btnProteina.setOnClickListener {
            activarCategoria(binding.btnProteina)
        }

        binding.btnFrutas.setOnClickListener {
            activarCategoria(binding.btnFrutas)
        }

        binding.btnVerduras.setOnClickListener {
            activarCategoria(binding.btnVerduras)
        }

        // ==========================================
        // REGISTRAR ENTRADA
        // ==========================================

        binding.btnRegistrarEntrada.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    RegistrarEntradaActivity::class.java
                )
            )
        }

        // ==========================================
        // BARRA DE NAVEGACIÓN
        // ==========================================

        configurarBarraNavegacion()

        // ==========================================
        // CONSUMO DEL BACKEND
        // ==========================================

        cargarDatosDeInventario()
    }

    // =========================================================
    // CONSUMO DE INVENTARIO
    // =========================================================

    private fun cargarDatosDeInventario() {

        lifecycleScope.launch {

            try {

                // A) Cargar ingredientes PRIMERO
                val resIngredientes =
                    inventarioRepository.obtenerIngredientes()

                if (
                    resIngredientes.isSuccessful &&
                    resIngredientes.body() != null
                ) {

                    mapaIngredientes =
                        resIngredientes.body()!!
                            .associateBy { it.idIngrediente }
                }

                // B) Cargar inventario
                val resInventario =
                    inventarioRepository.obtenerInventario()

                if (
                    resInventario.isSuccessful &&
                    resInventario.body() != null
                ) {

                    listaInventario = resInventario.body()!!

                    mostrarInventario(listaInventario)

                } else {

                    Toast.makeText(
                        this@InventarioActivity,
                        "Sin datos de inventario",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@InventarioActivity,
                    "Error al conectar con el servidor",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================================================
    // MOSTRAR INVENTARIO
    // =========================================================

    private fun mostrarInventario(lista: List<inventario>) {

        binding.contenedorInventario.removeAllViews()

        for (item in lista) {

            val itemView = LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_inventario,
                    binding.contenedorInventario,
                    false
                )

            val txtNombre =
                itemView.findViewById<TextView>(
                    R.id.txtNombreProducto
                )

            val txtDetalle =
                itemView.findViewById<TextView>(
                    R.id.txtDetalleProducto
                )

            val txtEstado =
                itemView.findViewById<TextView>(
                    R.id.txtEstadoProducto
                )

            // Buscar ingrediente relacionado
            val ingrediente =
                item.idIngrediente?.let {
                    mapaIngredientes[it]
                }

            val nombreItem =
                ingrediente?.nombreIngrediente
                    ?: item.nombre
                    ?: "Ingrediente #${item.idIngrediente ?: item.idInventario}"

            // Marca
            val marcaTexto =
                if (!ingrediente?.marcaIngrediente.isNullOrEmpty()) {
                    " · ${ingrediente?.marcaIngrediente}"
                } else {
                    ""
                }

            // Cantidad
            val cantidadItem =
                item.cantidadActual?.toDoubleOrNull()
                    ?: item.cantidad
                    ?: 0.0

            // Unidad
            val unidadItem =
                item.unidad_medida ?: "uds"

            txtNombre.text =
                "$nombreItem$marcaTexto"

            txtDetalle.text =
                "$cantidadItem $unidadItem disponibles"

            // ==========================================
            // ESTADO DEL STOCK
            // ==========================================

            val stockMin =
                item.stockMinimo?.toDoubleOrNull()
                    ?: 10.0

            if (cantidadItem <= stockMin) {

                txtEstado.text = "Stock bajo"

                txtEstado.setTextColor(
                    Color.parseColor("#8B5718")
                )

                txtEstado.setBackgroundResource(
                    R.drawable.bg_estado_bajo
                )

            } else {

                txtEstado.text = "Normal"

                txtEstado.setTextColor(
                    Color.parseColor("#477A26")
                )

                txtEstado.setBackgroundResource(
                    R.drawable.bg_estado_normal
                )
            }

            binding.contenedorInventario.addView(itemView)
        }
    }

    // =========================================================
    // BUSCAR INVENTARIO
    // =========================================================

    private fun filtrarInventario(texto: String) {

        val busqueda =
            texto.lowercase().trim()

        if (busqueda.isEmpty()) {

            mostrarInventario(listaInventario)
            return
        }

        val filtrados =
            listaInventario.filter { item ->

                val ingrediente =
                    item.idIngrediente?.let {
                        mapaIngredientes[it]
                    }

                val nombre =
                    ingrediente?.nombreIngrediente
                        ?: item.nombre
                        ?: "Ingrediente #${item.idIngrediente ?: item.idInventario}"

                nombre
                    .lowercase()
                    .contains(busqueda)
            }

        mostrarInventario(filtrados)
    }

    // =========================================================
    // CATEGORÍAS
    // =========================================================

    private fun activarCategoria(
        categoriaSeleccionada: TextView
    ) {

        val categorias = listOf(
            binding.btnLacteos,
            binding.btnProteina,
            binding.btnFrutas,
            binding.btnVerduras
        )

        for (categoria in categorias) {

            categoria.setBackgroundResource(
                R.drawable.bg_categoria
            )

            categoria.setTextColor(
                Color.parseColor("#6C6A63")
            )
        }

        categoriaSeleccionada.setBackgroundResource(
            R.drawable.bg_categoria_activa
        )

        categoriaSeleccionada.setTextColor(
            Color.parseColor("#3F3B28")
        )
    }

    // =========================================================
    // BARRA DE NAVEGACIÓN
    // =========================================================

    private fun configurarBarraNavegacion() {
        com.example.movilmanupuladora.utils.NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            com.example.movilmanupuladora.utils.NavigationHelper.Tab.INVENTARIO
        )
    }
}


package com.example.movilmanupuladora.ui.manipuladora

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

        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        binding.edtBuscarIngrediente.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filtrarInventario(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            }
        )

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

        configurarBarraNavegacion()
        cargarDatosDeInventario()
    }

    private fun cargarDatosDeInventario() {
        lifecycleScope.launch {
            try {
                val resIngredientes = inventarioRepository.obtenerIngredientes()
                if (resIngredientes.isSuccessful && resIngredientes.body() != null) {
                    mapaIngredientes = resIngredientes.body()!!.associateBy { it.idIngrediente }
                }

                val resInventario = inventarioRepository.obtenerInventario()
                if (resInventario.isSuccessful && resInventario.body() != null) {
                    listaInventario = resInventario.body()!!
                    mostrarInventario(listaInventario)
                } else {
                    Toast.makeText(this@InventarioActivity, "Sin datos de inventario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@InventarioActivity, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarInventario(lista: List<inventario>) {
        binding.contenedorInventario.removeAllViews()

        for (item in lista) {
            val itemView = LayoutInflater.from(this).inflate(
                R.layout.item_inventario,
                binding.contenedorInventario,
                false
            )

            val txtNombre = itemView.findViewById<TextView>(R.id.txtNombreProducto)
            val txtDetalle = itemView.findViewById<TextView>(R.id.txtDetalleProducto)
            val txtEstado = itemView.findViewById<TextView>(R.id.txtEstadoProducto)

            val ingrediente = item.idIngrediente?.let { mapaIngredientes[it] }
            val nombreItem = ingrediente?.nombreIngrediente ?: item.nombre ?: "Insumo #${item.idIngrediente ?: item.idInventario}"
            val marcaTexto = if (!ingrediente?.marcaIngrediente.isNullOrEmpty()) " · ${ingrediente?.marcaIngrediente}" else ""

            val cantidadItem = item.cantidadActual?.toDoubleOrNull() ?: item.cantidad ?: 0.0
            val unidadItem = item.unidad_medida ?: "kg"

            txtNombre.text = "$nombreItem$marcaTexto"
            txtDetalle.text = "$cantidadItem $unidadItem disponibles"

            val stockMin = item.stockMinimo?.toDoubleOrNull() ?: 10.0

            if (cantidadItem <= stockMin) {
                txtEstado.text = "Stock bajo"
                txtEstado.setTextColor(Color.parseColor("#7A5500"))
                txtEstado.setBackgroundResource(R.drawable.bg_pill_yellow)
            } else {
                txtEstado.text = "Normal"
                txtEstado.setTextColor(Color.parseColor("#2A5226"))
                txtEstado.setBackgroundResource(R.drawable.bg_pill_green)
            }

            binding.contenedorInventario.addView(itemView)
        }
    }

    private fun filtrarInventario(texto: String) {
        val busqueda = texto.lowercase().trim()
        if (busqueda.isEmpty()) {
            mostrarInventario(listaInventario)
            return
        }

        val filtrados = listaInventario.filter { item ->
            val ingrediente = item.idIngrediente?.let { mapaIngredientes[it] }
            val nombre = ingrediente?.nombreIngrediente ?: item.nombre ?: ""
            nombre.lowercase().contains(busqueda)
        }

        mostrarInventario(filtrados)
    }

    private fun activarCategoria(categoriaSeleccionada: TextView) {
        val categorias = listOf(binding.btnLacteos, binding.btnProteina, binding.btnFrutas, binding.btnVerduras)

        for (cat in categorias) {
            cat.setBackgroundResource(R.drawable.bg_tag_plato)
            cat.setTextColor(Color.parseColor("#64748B"))
        }

        categoriaSeleccionada.setBackgroundResource(R.drawable.bg_pill_yellow)
        categoriaSeleccionada.setTextColor(Color.parseColor("#7A5500"))
    }

    private fun configurarBarraNavegacion() {
        com.example.movilmanupuladora.utils.NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            com.example.movilmanupuladora.utils.NavigationHelper.Tab.INVENTARIO
        )
    }
}
package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.MainActivity
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.Ingrediente
import com.example.movilmanupuladora.data.model.inventario
import com.example.movilmanupuladora.data.repository.InventarioRepository
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch

class InventarioActivity : AppCompatActivity() {

    private lateinit var contenedorInventario: LinearLayout
    private val inventarioRepository = InventarioRepository(RetrofitClient.apiService)
    private var listaInventario: List<inventario> = emptyList()
    // Mapa id_ingrediente -> Ingrediente para cruzar datos
    private var mapaIngredientes: Map<Int, Ingrediente> = emptyMap()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restaurar token
        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        enableEdgeToEdge()

        setContentView(R.layout.activity_inventario)

        // CONTENEDOR DONDE SE DIBUJARÁN LOS PRODUCTOS
        contenedorInventario = findViewById(R.id.contenedorInventario)

        // BUSCADOR
        val edtBuscar = findViewById<EditText>(R.id.edtBuscarIngrediente)
        edtBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarInventario(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // CATEGORÍAS
        val btnLacteos = findViewById<TextView>(R.id.btnLacteos)
        val btnProteina = findViewById<TextView>(R.id.btnProteina)
        val btnFrutas = findViewById<TextView>(R.id.btnFrutas)
        val btnVerduras = findViewById<TextView>(R.id.btnVerduras)

        btnLacteos.setOnClickListener { activarCategoria(btnLacteos) }
        btnProteina.setOnClickListener { activarCategoria(btnProteina) }
        btnFrutas.setOnClickListener { activarCategoria(btnFrutas) }
        btnVerduras.setOnClickListener { activarCategoria(btnVerduras) }

        // REGISTRAR ENTRADA
        val btnRegistrarEntrada = findViewById<TextView>(R.id.btnRegistrarEntrada)
        btnRegistrarEntrada.setOnClickListener {
            // Aquí posteriormente se abrirá RegistrarEntradaActivity
        }

        // BARRA DE NAVEGACIÓN
        configurarBarraNavegacion()

        // CARGAR DATOS DEL BACKEND
        cargarDatosDeInventario()
    }

    private fun cargarDatosDeInventario() {
        lifecycleScope.launch {
            try {
                // A) Cargar ingredientes PRIMERO para tener el mapa de nombres listo
                val resIngredientes = inventarioRepository.obtenerIngredientes()
                if (resIngredientes.isSuccessful && resIngredientes.body() != null) {
                    mapaIngredientes = resIngredientes.body()!!.associateBy { it.idIngrediente }
                }

                // B) Cargar el inventario
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
        contenedorInventario.removeAllViews()

        for (item in lista) {
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_inventario, contenedorInventario, false)

            val txtNombre = itemView.findViewById<TextView>(R.id.txtNombreProducto)
            val txtDetalle = itemView.findViewById<TextView>(R.id.txtDetalleProducto)
            val txtEstado = itemView.findViewById<TextView>(R.id.txtEstadoProducto)

            // Buscar el nombre del ingrediente cruzando con la lista de ingredientes
            val ingrediente = item.idIngrediente?.let { mapaIngredientes[it] }
            val nombreItem = ingrediente?.nombreIngrediente
                ?: item.nombre
                ?: "Ingrediente #${item.idIngrediente ?: item.idInventario}"

            // Mostrar la marca si existe
            val marcaTexto = if (!ingrediente?.marcaIngrediente.isNullOrEmpty()) {
                " · ${ingrediente?.marcaIngrediente}"
            } else ""

            // Cantidad y unidad (la API devuelve "15.00" como texto, hay que convertirlo)
            val cantidadItem = item.cantidadActual?.toDoubleOrNull()
                ?: item.cantidad
                ?: 0.0
            val unidadItem = item.unidad_medida ?: "uds"

            txtNombre.text = "$nombreItem$marcaTexto"
            txtDetalle.text = "$cantidadItem $unidadItem disponibles"

            // Estado según stock_minimo
            val stockMin = item.stockMinimo?.toDoubleOrNull() ?: 10.0
            if (cantidadItem <= stockMin) {
                txtEstado.text = "Stock bajo"
                txtEstado.setTextColor(Color.parseColor("#8B5718"))
                txtEstado.setBackgroundResource(R.drawable.bg_estado_bajo)
            } else {
                txtEstado.text = "Normal"
                txtEstado.setTextColor(Color.parseColor("#477A26"))
                txtEstado.setBackgroundResource(R.drawable.bg_estado_normal)
            }

            contenedorInventario.addView(itemView)
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
            val nombre = ingrediente?.nombreIngrediente
                ?: item.nombre
                ?: "Ingrediente #${item.idIngrediente ?: item.idInventario}"
            nombre.lowercase().contains(busqueda)
        }

        mostrarInventario(filtrados)
    }

    private fun activarCategoria(categoriaSeleccionada: TextView) {
        val categorias = listOf(
            findViewById<TextView>(R.id.btnLacteos),
            findViewById<TextView>(R.id.btnProteina),
            findViewById<TextView>(R.id.btnFrutas),
            findViewById<TextView>(R.id.btnVerduras)
        )
        for (categoria in categorias) {
            categoria.setBackgroundResource(R.drawable.bg_categoria)
            categoria.setTextColor(Color.parseColor("#6C6A63"))
        }
        categoriaSeleccionada.setBackgroundResource(R.drawable.bg_categoria_activa)
        categoriaSeleccionada.setTextColor(Color.parseColor("#3F3B28"))
    }

    private fun configurarBarraNavegacion() {
        findViewById<LinearLayout>(R.id.navInicio).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navAvisos).setOnClickListener {
            startActivity(Intent(this, AvisosActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java)); finish()
        }
    }
}

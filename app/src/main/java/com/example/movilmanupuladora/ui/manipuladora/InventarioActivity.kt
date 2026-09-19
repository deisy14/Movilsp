package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.movilmanupuladora.data.model.inventario
import com.example.movilmanupuladora.data.repository.InventarioRepository
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch

class InventarioActivity : AppCompatActivity() {

    private lateinit var itemLeche: LinearLayout
    private lateinit var itemQueso: LinearLayout
    private lateinit var itemYogurt: LinearLayout
    private lateinit var itemMantequilla: LinearLayout
    private lateinit var itemChocolate: LinearLayout
    
    private val inventarioRepository = InventarioRepository(RetrofitClient.apiService)
    private var listaInventario: List<inventario> = emptyList()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restaurar token
        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        enableEdgeToEdge()

        setContentView(R.layout.activity_inventario)

        // PRODUCTOS
        itemLeche = findViewById(R.id.itemLeche)
        itemQueso = findViewById(R.id.itemQueso)
        itemYogurt = findViewById(R.id.itemYogurt)
        itemMantequilla = findViewById(R.id.itemMantequilla)
        itemChocolate = findViewById(R.id.itemChocolate)

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

        btnLacteos.setOnClickListener {
            activarCategoria(btnLacteos)
            mostrarLacteos()
        }
        btnProteina.setOnClickListener { activarCategoria(btnProteina); ocultarProductos() }
        btnFrutas.setOnClickListener { activarCategoria(btnFrutas); ocultarProductos() }
        btnVerduras.setOnClickListener { activarCategoria(btnVerduras); ocultarProductos() }

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
                val response = inventarioRepository.obtenerInventario()
                if (response.isSuccessful && response.body() != null) {
                    listaInventario = response.body()!!
                    Toast.makeText(this@InventarioActivity, "Inventario cargado: ${listaInventario.size} productos", Toast.LENGTH_SHORT).show()
                    // Aquí se podría mapear a una lista dinámica
                }
            } catch (e: Exception) {
                Toast.makeText(this@InventarioActivity, "Error al cargar inventario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filtrarInventario(texto: String) {
        val busqueda = texto.lowercase().trim()
        if (busqueda.isEmpty()) {
            mostrarLacteos()
            return
        }
        itemLeche.visibility = if ("leche".contains(busqueda)) View.VISIBLE else View.GONE
        itemQueso.visibility = if ("queso".contains(busqueda)) View.VISIBLE else View.GONE
        itemYogurt.visibility = if ("yogurt".contains(busqueda)) View.VISIBLE else View.GONE
        itemMantequilla.visibility = if ("mantequilla".contains(busqueda)) View.VISIBLE else View.GONE
        itemChocolate.visibility = if ("chocolate".contains(busqueda)) View.VISIBLE else View.GONE
    }

    private fun mostrarLacteos() {
        itemLeche.visibility = View.VISIBLE
        itemQueso.visibility = View.VISIBLE
        itemYogurt.visibility = View.VISIBLE
        itemMantequilla.visibility = View.VISIBLE
        itemChocolate.visibility = View.VISIBLE
    }

    private fun ocultarProductos() {
        itemLeche.visibility = View.GONE
        itemQueso.visibility = View.GONE
        itemYogurt.visibility = View.GONE
        itemMantequilla.visibility = View.GONE
        itemChocolate.visibility = View.GONE
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

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
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.MainActivity

class inventario_activity : AppCompatActivity() {

    private lateinit var itemLeche: LinearLayout
    private lateinit var itemQueso: LinearLayout
    private lateinit var itemYogurt: LinearLayout
    private lateinit var itemMantequilla: LinearLayout
    private lateinit var itemChocolate: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_inventario)

        // PRODUCTOS

        itemLeche = findViewById(R.id.itemLeche)
        itemQueso = findViewById(R.id.itemQueso)
        itemYogurt = findViewById(R.id.itemYogurt)
        itemMantequilla = findViewById(R.id.itemMantequilla)
        itemChocolate = findViewById(R.id.itemChocolate)

        // BUSCADOR

        val edtBuscar =
            findViewById<EditText>(R.id.edtBuscarIngrediente)

        edtBuscar.addTextChangedListener(
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
                    filtrarInventario(
                        s?.toString() ?: ""
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )

        // CATEGORÍAS

        val btnLacteos =
            findViewById<TextView>(R.id.btnLacteos)

        val btnProteina =
            findViewById<TextView>(R.id.btnProteina)

        val btnFrutas =
            findViewById<TextView>(R.id.btnFrutas)

        val btnVerduras =
            findViewById<TextView>(R.id.btnVerduras)

        // LÁCTEOS

        btnLacteos.setOnClickListener {
            activarCategoria(btnLacteos)
            mostrarLacteos()
        }

        // PROTEÍNA

        btnProteina.setOnClickListener {
            activarCategoria(btnProteina)
            ocultarProductos()
        }

        // FRUTAS

        btnFrutas.setOnClickListener {
            activarCategoria(btnFrutas)
            ocultarProductos()
        }

        // VERDURAS

        btnVerduras.setOnClickListener {
            activarCategoria(btnVerduras)
            ocultarProductos()
        }

        // REGISTRAR ENTRADA

        val btnRegistrarEntrada =
            findViewById<TextView>(R.id.btnRegistrarEntrada)

        btnRegistrarEntrada.setOnClickListener {
            // Aquí posteriormente se abrirá
            // RegistrarEntradaActivity
        }

        // BARRA DE NAVEGACIÓN

        configurarBarraNavegacion()
    }

    // ==================================================
    // FILTRAR INVENTARIO
    // ==================================================

    private fun filtrarInventario(texto: String) {

        val busqueda = texto.lowercase().trim()

        if (busqueda.isEmpty()) {
            mostrarLacteos()
            return
        }

        itemLeche.visibility =
            if ("leche".contains(busqueda))
                View.VISIBLE
            else
                View.GONE

        itemQueso.visibility =
            if ("queso".contains(busqueda))
                View.VISIBLE
            else
                View.GONE

        itemYogurt.visibility =
            if ("yogurt".contains(busqueda))
                View.VISIBLE
            else
                View.GONE

        itemMantequilla.visibility =
            if ("mantequilla".contains(busqueda))
                View.VISIBLE
            else
                View.GONE

        itemChocolate.visibility =
            if ("chocolate".contains(busqueda))
                View.VISIBLE
            else
                View.GONE
    }

    // ==================================================
    // MOSTRAR LÁCTEOS
    // ==================================================

    private fun mostrarLacteos() {

        itemLeche.visibility = View.VISIBLE
        itemQueso.visibility = View.VISIBLE
        itemYogurt.visibility = View.VISIBLE
        itemMantequilla.visibility = View.VISIBLE
        itemChocolate.visibility = View.VISIBLE
    }

    // ==================================================
    // OCULTAR PRODUCTOS
    // ==================================================

    private fun ocultarProductos() {

        itemLeche.visibility = View.GONE
        itemQueso.visibility = View.GONE
        itemYogurt.visibility = View.GONE
        itemMantequilla.visibility = View.GONE
        itemChocolate.visibility = View.GONE
    }

    // ==================================================
    // ACTIVAR CATEGORÍA
    // ==================================================

    private fun activarCategoria(
        categoriaSeleccionada: TextView
    ) {

        val categorias = listOf(

            findViewById<TextView>(R.id.btnLacteos),
            findViewById<TextView>(R.id.btnProteina),
            findViewById<TextView>(R.id.btnFrutas),
            findViewById<TextView>(R.id.btnVerduras)
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

    // ==================================================
    // BARRA DE NAVEGACIÓN
    // ==================================================

    private fun configurarBarraNavegacion() {

        val navInicio =
            findViewById<LinearLayout>(R.id.navInicio)

        val navAsignadas =
            findViewById<LinearLayout>(R.id.navAsignadas)

        val navInventario =
            findViewById<LinearLayout>(R.id.navInventario)

        val navAvisos =
            findViewById<LinearLayout>(R.id.navAvisos)

        val navPerfil =
            findViewById<LinearLayout>(R.id.navPerfil)

        // INICIO

        navInicio.setOnClickListener {

            val intent =
                Intent(
                    this,
                    MainActivity::class.java
                )

            startActivity(intent)
            finish()
        }

        // ASIGNADAS

        navAsignadas.setOnClickListener {

            // Aquí se colocará la Activity
            // de Asignadas.
        }

        // INVENTARIO

        navInventario.setOnClickListener {

            // Ya estamos en Inventario.
        }

        // AVISOS

        navAvisos.setOnClickListener {

            val intent =
                Intent(
                    this,
                    avisos_Activity::class.java
                )

            startActivity(intent)
            finish()
        }

        // PERFIL

        navPerfil.setOnClickListener {

            val intent =
                Intent(
                    this,
                    activity_perfil::class.java
                )

            startActivity(intent)
            finish()
        }
    }
}

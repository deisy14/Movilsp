package com.example.psirae

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.text.Editable
import android.text.TextWatcher

class inventario_activity : AppCompatActivity() {

    // Productos
    private lateinit var itemLeche: LinearLayout
    private lateinit var itemQueso: LinearLayout
    private lateinit var itemYogurt: LinearLayout
    private lateinit var itemMantequilla: LinearLayout
    private lateinit var itemChocolate: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_inventario)

        // ==========================================
        // INSETS
        // ==========================================

        val root = findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }


        // ==========================================
        // PRODUCTOS
        // ==========================================

        itemLeche = findViewById(R.id.itemLeche)
        itemQueso = findViewById(R.id.itemQueso)
        itemYogurt = findViewById(R.id.itemYogurt)
        itemMantequilla = findViewById(R.id.itemMantequilla)
        itemChocolate = findViewById(R.id.itemChocolate)


        // ==========================================
        // BUSCADOR
        // ==========================================

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
                        s.toString()
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )


        // ==========================================
        // CATEGORÍAS
        // ==========================================

        val btnLacteos =
            findViewById<TextView>(R.id.btnLacteos)

        val btnProteina =
            findViewById<TextView>(R.id.btnProteina)

        val btnFrutas =
            findViewById<TextView>(R.id.btnFrutas)

        val btnVerduras =
            findViewById<TextView>(R.id.btnVerduras)


        // ==========================================
        // LÁCTEOS
        // ==========================================

        btnLacteos.setOnClickListener {

            activarCategoria(btnLacteos)

            mostrarLacteos()
        }


        // ==========================================
        // PROTEÍNA
        // ==========================================

        btnProteina.setOnClickListener {

            activarCategoria(btnProteina)

            ocultarProductos()
        }


        // ==========================================
        // FRUTAS
        // ==========================================

        btnFrutas.setOnClickListener {

            activarCategoria(btnFrutas)

            ocultarProductos()
        }


        // ==========================================
        // VERDURAS
        // ==========================================

        btnVerduras.setOnClickListener {

            activarCategoria(btnVerduras)

            ocultarProductos()
        }


        // ==========================================
        // REGISTRAR ENTRADA
        // ==========================================

        val btnRegistrarEntrada =
            findViewById<TextView>(R.id.btnRegistrarEntrada)

        btnRegistrarEntrada.setOnClickListener {

            // Cuando tengas creada la pantalla:
            //
            // val intent = Intent(
            //     this,
            //     RegistrarEntradaActivity::class.java
            // )
            //
            // startActivity(intent)

        }


        // ==========================================
        // BARRA DE NAVEGACIÓN
        // ==========================================

        configurarBarraNavegacion()
    }


    // ==================================================
    // FILTRAR PRODUCTOS
    // ==================================================

    private fun filtrarInventario(texto: String) {

        val busqueda =
            texto.lowercase().trim()


        // Si no escribió nada,
        // mostramos nuevamente los lácteos.

        if (busqueda.isEmpty()) {

            mostrarLacteos()

            return
        }


        // LECHE

        itemLeche.visibility =
            if ("leche".contains(busqueda))
                View.VISIBLE
            else
                View.GONE


        // QUESO

        itemQueso.visibility =
            if ("queso".contains(busqueda))
                View.VISIBLE
            else
                View.GONE


        // YOGURT

        itemYogurt.visibility =
            if ("yogurt".contains(busqueda))
                View.VISIBLE
            else
                View.GONE


        // MANTEQUILLA

        itemMantequilla.visibility =
            if ("mantequilla".contains(busqueda))
                View.VISIBLE
            else
                View.GONE


        // CHOCOLATE

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

            findViewById<TextView>(
                R.id.btnLacteos
            ),

            findViewById<TextView>(
                R.id.btnProteina
            ),

            findViewById<TextView>(
                R.id.btnFrutas
            ),

            findViewById<TextView>(
                R.id.btnVerduras
            )
        )


        // Todas vuelven a estado normal

        for (categoria in categorias) {

            categoria.setBackgroundResource(
                R.drawable.bg_categoria
            )

            categoria.setTextColor(
                Color.parseColor("#6C6A63")
            )
        }


        // Categoría seleccionada

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


        // ==========================================
        // INICIO
        // ==========================================

        navInicio.setOnClickListener {

            val intent =
                Intent(
                    this,
                    MainActivity::class.java
                )

            startActivity(intent)

            finish()
        }


        // ==========================================
        // ASIGNADAS
        // ==========================================

        navAsignadas.setOnClickListener {

            val intent =
                Intent(
                    this,
                    navAsignadas::class.java
                )

            startActivity(intent)

            finish()
        }


        // ==========================================
        // INVENTARIO
        // ==========================================

        navInventario.setOnClickListener {

            // Ya estamos en Inventario.
            // No hacemos nada.
        }


        // ==========================================
        // AVISOS
        // ==========================================

        navAvisos.setOnClickListener {

            val intent =
                Intent(
                    this,
                    avisos_Activity::class.java
                )

            startActivity(intent)

            finish()
        }


        // ==========================================
        // PERFIL
        // ==========================================

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

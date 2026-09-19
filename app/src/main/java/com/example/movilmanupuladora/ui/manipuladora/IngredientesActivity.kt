package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.Ingrediente
import kotlinx.coroutines.launch

class IngredientesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_ingredientes)

        // Barras del sistema
        val main = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Mostrar nombre del plato si viene del intent
        val nombrePlato = intent.getStringExtra("nombre_plato") ?: "Plato"
        val tvNombrePlato = findViewById<TextView>(R.id.tvNombrePlatoIngredientes)
        tvNombrePlato?.text = nombrePlato

        // Botón continuar a preparación
        val btnContinuarPreparacion = findViewById<Button>(R.id.btnContinuarPreparacion)
        btnContinuarPreparacion.setOnClickListener {
            startActivity(Intent(this, PreparacionActivity::class.java))
        }

        // Cargar ingredientes desde el backend
        cargarIngredientes()
    }

    private fun cargarIngredientes() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerIngredientes()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val lista = response.body()!!
                    mostrarIngredientes(lista)
                } else {
                    Toast.makeText(
                        this@IngredientesActivity,
                        "Sin ingredientes registrados",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@IngredientesActivity,
                    "Error al conectar: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarIngredientes(ingredientes: List<Ingrediente>) {
        val contenedor = findViewById<LinearLayout>(R.id.contenedorIngredientes) ?: return
        contenedor.removeAllViews()

        // Título "Lista de ingredientes"
        val tvTitulo = TextView(this).apply {
            text = "Lista de ingredientes"
            setTextColor(0xFF173F2A.toInt())
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 56, 0, 0)
        }
        contenedor.addView(tvTitulo)

        for (ingrediente in ingredientes) {
            // Fila: punto + nombre + marca
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 30, 0, 0)
            }

            // Punto verde
            val punto = View(this).apply {
                setBackgroundResource(R.drawable.bg_punto_lista)
                layoutParams = LinearLayout.LayoutParams(16, 16).also {
                    it.marginEnd = 26
                }
            }

            // Nombre del ingrediente
            val tvNombre = TextView(this).apply {
                text = ingrediente.nombreIngrediente ?: "Sin nombre"
                setTextColor(0xFF34443A.toInt())
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Marca del ingrediente
            val tvMarca = TextView(this).apply {
                text = ingrediente.marcaIngrediente ?: ""
                setTextColor(0xFF65736A.toInt())
                textSize = 14f
            }

            fila.addView(punto)
            fila.addView(tvNombre)
            fila.addView(tvMarca)
            contenedor.addView(fila)

            // Línea separadora
            val linea = View(this).apply {
                setBackgroundColor(0xFFEDEDED.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 2
                ).also { it.topMargin = 26 }
            }
            contenedor.addView(linea)
        }
    }
}
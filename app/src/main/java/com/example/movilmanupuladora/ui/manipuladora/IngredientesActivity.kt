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

        // Botón volver
        findViewById<View>(R.id.btnVolver)?.setOnClickListener {
            finish()
        }

        // Mostrar nombre del plato si viene del intent
        val nombrePlato = intent.getStringExtra("nombre_plato") ?: "Bandeja Paisa"
        val tvNombrePlato = findViewById<TextView>(R.id.tvNombrePlatoIngredientes)
        val nombreFormateado = nombrePlato.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        tvNombrePlato?.text = nombreFormateado

        // Asignar imagen adecuada según el plato
        val imgPlato = findViewById<android.widget.ImageView>(R.id.imgPlatoIngredientes)
        val imgRes = when {
            nombrePlato.contains("bandeja", ignoreCase = true) -> R.drawable.bandeja_paisa
            nombrePlato.contains("frijol", ignoreCase = true) -> R.drawable.frijoles
            nombrePlato.contains("chocolate", ignoreCase = true) -> R.drawable.chocolate
            nombrePlato.contains("huevo", ignoreCase = true) -> R.drawable.huevo_perico
            nombrePlato.contains("pollo", ignoreCase = true) -> R.drawable.apanado
            nombrePlato.contains("arroz", ignoreCase = true) -> R.drawable.arroz_de_leche
            else -> R.drawable.frijoles
        }
        imgPlato?.setImageResource(imgRes)

        // Botón continuar a preparación
        val btnContinuarPreparacion = findViewById<Button>(R.id.btnContinuarPreparacion)
        btnContinuarPreparacion.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                putExtra("nombre_plato", nombrePlato)
                putExtra("id_plato", intent.getIntExtra("id_plato", -1))
            }
            startActivity(intent)
        }

        // Cargar ingredientes desde el backend
        cargarIngredientes()
    }

    private val ingredientesFallback = listOf(
        Ingrediente(idIngrediente = 1, nombreIngrediente = "Arroz blanco de grano largo", marcaIngrediente = "Diana / 500g"),
        Ingrediente(idIngrediente = 2, nombreIngrediente = "Presa o pechuga seleccionada", marcaIngrediente = "Campesino / 1kg"),
        Ingrediente(idIngrediente = 3, nombreIngrediente = "Fríjol rojo / cargamanto", marcaIngrediente = "Grano Oro / 500g"),
        Ingrediente(idIngrediente = 4, nombreIngrediente = "Cebolla cabezona y tomate", marcaIngrediente = "Finca Fresca / 250g"),
        Ingrediente(idIngrediente = 5, nombreIngrediente = "Aceite vegetal institucional", marcaIngrediente = "Premier / 250ml"),
        Ingrediente(idIngrediente = 6, nombreIngrediente = "Sal yodada y especias", marcaIngrediente = "Refisal / 100g")
    )

    private fun cargarIngredientes() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerIngredientes()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val lista = response.body()!!
                    mostrarIngredientes(lista)
                } else {
                    mostrarIngredientes(ingredientesFallback)
                }
            } catch (e: Exception) {
                mostrarIngredientes(ingredientesFallback)
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
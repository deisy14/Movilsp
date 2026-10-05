package com.example.movilmanupuladora.ui.manipuladora

import android.os.Bundle
import android.view.View
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
import com.example.movilmanupuladora.data.model.pasos_preparacion
import com.example.movilmanupuladora.databinding.ActivityPreparacionBinding
import com.example.movilmanupuladora.utils.NavigationHelper
import kotlinx.coroutines.launch

class PreparacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreparacionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPreparacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón volver
        binding.btnVolver.setOnClickListener { finish() }

        // Botón marcar como preparado
        val preferencias = getSharedPreferences("SIRAE", MODE_PRIVATE)
        val preparado = preferencias.getBoolean("plato_preparado", false)

        if (preparado) {
            binding.btnMarcarPreparado.text = "✓   Preparado"
            binding.btnMarcarPreparado.isEnabled = false
            binding.btnMarcarPreparado.alpha = 0.6f
        }

        binding.btnMarcarPreparado.setOnClickListener {
            preferencias.edit().putBoolean("plato_preparado", true).apply()
            binding.btnMarcarPreparado.text = "✓   Preparado"
            binding.btnMarcarPreparado.isEnabled = false
            binding.btnMarcarPreparado.alpha = 0.6f
            Toast.makeText(this, "¡Plato marcado como preparado!", Toast.LENGTH_SHORT).show()
        }

        // Barra de navegación
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.ASIGNADAS
        )

        // Mostrar nombre e imagen del plato si viene del intent
        val nombrePlato = intent.getStringExtra("nombre_plato") ?: "Arroz con Pollo"
        val componente = intent.getStringExtra("componente_seleccionado")

        val nombreFormateado = nombrePlato.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        binding.tvNombrePlatoPreparacion.text = nombreFormateado

        if (!componente.isNullOrBlank()) {
            binding.tvComponenteSubtituloPreparacion.text = "Componente: $componente"
            binding.tvComponenteSubtituloPreparacion.visibility = View.VISIBLE
        } else {
            binding.tvComponenteSubtituloPreparacion.visibility = View.GONE
        }

        val imgRes = when {
            nombrePlato.contains("bandeja", ignoreCase = true) -> R.drawable.bandeja_paisa
            nombrePlato.contains("frijol", ignoreCase = true) -> R.drawable.frijoles
            nombrePlato.contains("chocolate", ignoreCase = true) -> R.drawable.chocolate
            nombrePlato.contains("huevo", ignoreCase = true) -> R.drawable.huevo_perico
            nombrePlato.contains("pollo", ignoreCase = true) -> R.drawable.arroz_pollo
            nombrePlato.contains("arroz", ignoreCase = true) -> R.drawable.arroz_de_leche
            else -> R.drawable.comida_almuerzo
        }
        binding.imgPlatoPreparacion.setImageResource(imgRes)

        // Cargar pasos de la receta
        cargarPasos()
    }

    private val pasosFallback = listOf(
        pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Lavar y desinfectar los ingredientes, utensilios y mesas de trabajo siguiendo el protocolo de inocuidad PAE."),
        pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Cocinar y sellar la proteína y los principios a temperatura controlada (mínimo 75°C)."),
        pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Verificar sazón, textura, cocción completa y temperatura antes del porcionado."),
        pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Porcionar y servir según las tablas de gramaje institucional del programa de alimentación escolar.")
    )

    private fun cargarPasos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerPasosPreparacion()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val listaPasos: List<pasos_preparacion> = response.body()!!
                    mostrarPasos(listaPasos)
                } else {
                    mostrarPasos(pasosFallback)
                }
            } catch (e: Exception) {
                mostrarPasos(pasosFallback)
            }
        }
    }

    private fun mostrarPasos(pasos: List<pasos_preparacion>) {
        val contenedor = binding.contenedorPasos
        contenedor.removeAllViews()

        for (paso in pasos) {
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 16, 0, 16)
            }

            val tvNumero = TextView(this).apply {
                text = paso.numeroPaso
                setTextColor(0xFF7A5500.toInt())
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.bg_pill_yellow)
                layoutParams = LinearLayout.LayoutParams(56, 56)
            }

            val tvDescripcion = TextView(this).apply {
                text = paso.descripcionPaso
                setTextColor(0xFF1B3317.toInt())
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).also { it.marginStart = 20 }
            }

            fila.addView(tvNumero)
            fila.addView(tvDescripcion)
            contenedor.addView(fila)
        }
    }
}
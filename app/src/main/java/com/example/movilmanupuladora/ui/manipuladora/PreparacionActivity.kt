package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.pasos_preparacion
import com.example.movilmanupuladora.databinding.ActivityPreparacionBinding
import com.example.movilmanupuladora.utils.NavigationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PreparacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreparacionBinding
    private var nombrePlatoActual: String = "Pollo Guisado Criollo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPreparacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolver.setOnClickListener { finish() }

        val preferencias = getSharedPreferences("SIRAE", MODE_PRIVATE)
        val estadoActual = preferencias.getString("estado_plato", "PENDIENTE")

        if (estadoActual == "FINALIZADO") {
            binding.btnMarcarPreparado.text = "✅   Finalizado"
            binding.btnMarcarPreparado.isEnabled = false
            binding.btnMarcarPreparado.alpha = 0.7f
            binding.badgeTurnoPreparacion.text = "Estado: ✅ Finalizado"
            binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_green)
        } else if (estadoActual == "EN_PREPARACION") {
            binding.btnMarcarPreparado.text = "🍳 En preparación"
            binding.badgeTurnoPreparacion.text = "Estado: 🍳 En preparación"
            binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_yellow)
        }

        binding.btnMarcarPreparado.setOnClickListener {
            mostrarDialogoEstadoPreparacion()
        }

        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.ASIGNADAS,
        )

        nombrePlatoActual = intent.getStringExtra("nombre_plato") ?: "Pollo Guisado Criollo"
        val nombreFormateado = nombrePlatoActual.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        binding.tvNombrePlatoPreparacion.text = nombreFormateado

        val imgRes = when {
            nombrePlatoActual.contains("bandeja", ignoreCase = true) -> R.drawable.bandeja_paisa
            nombrePlatoActual.contains("frijol", ignoreCase = true) -> R.drawable.frijoles
            nombrePlatoActual.contains("chocolate", ignoreCase = true) -> R.drawable.chocolate
            nombrePlatoActual.contains("huevo", ignoreCase = true) -> R.drawable.huevo_perico
            nombrePlatoActual.contains("pollo", ignoreCase = true) -> R.drawable.pollo_guisado
            nombrePlatoActual.contains("arroz", ignoreCase = true) -> R.drawable.arroz_de_leche
            else -> R.drawable.pollo_guisado
        }
        binding.imgPlatoPreparacion.setImageResource(imgRes)

        cargarPasos()
    }

    private fun mostrarDialogoEstadoPreparacion() {
        val opciones = arrayOf("📋 Por hacer / Pendiente", "🍳 En proceso / En preparación", "✅ Completado / Finalizado")

        MaterialAlertDialogBuilder(this)
            .setTitle("Actualizar Estado de Preparación")
            .setItems(opciones) { dialog, which ->
                val preferencias = getSharedPreferences("SIRAE", MODE_PRIVATE)
                when (which) {
                    0 -> { // Por hacer
                        preferencias.edit().putString("estado_plato", "PENDIENTE").apply()
                        binding.badgeTurnoPreparacion.text = "Estado: 📋 Por hacer"
                        binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_tag_plato)
                        binding.btnMarcarPreparado.text = "Marcar Estado"
                        Toast.makeText(this, "Estado cambiado a: Por hacer", Toast.LENGTH_SHORT).show()
                    }
                    1 -> { // En proceso
                        preferencias.edit().putString("estado_plato", "EN_PREPARACION").apply()
                        binding.badgeTurnoPreparacion.text = "Estado: 🍳 En proceso"
                        binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_yellow)
                        binding.btnMarcarPreparado.text = "🍳 En proceso"
                        Toast.makeText(this, "Estado cambiado a: En proceso", Toast.LENGTH_SHORT).show()
                    }
                    2 -> { // Completado -> Abre ConfirmacionActivity
                        preferencias.edit().putBoolean("plato_preparado", true).apply()
                        preferencias.edit().putString("estado_plato", "FINALIZADO").apply()
                        binding.badgeTurnoPreparacion.text = "Estado: ✅ Completado"
                        binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_green)
                        binding.btnMarcarPreparado.text = "✅   Completado"
                        binding.btnMarcarPreparado.isEnabled = false
                        binding.btnMarcarPreparado.alpha = 0.7f

                        val intentConf = Intent(this, ConfirmacionActivity::class.java).apply {
                            putExtra("nombre_plato", nombrePlatoActual)
                        }
                        startActivity(intentConf)
                        finish()
                    }
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private val pasosFallback = listOf(
        pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Lavar y desinfectar los ingredientes, utensilios y mesas de trabajo siguiendo el protocolo de bioseguridad."),
        pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Cocinar y sellar la proteína y los principios a temperatura controlada (mínimo 75°C)."),
        pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Verificar sazón, textura, cocción completa y temperatura antes del ensamble del plato."),
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
        binding.contenedorPasos.removeAllViews()

        for (paso in pasos) {
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 24, 0, 0)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val tvNumero = TextView(this).apply {
                text = paso.numeroPaso
                setTextColor(0xFF1B3317.toInt())
                textSize = 13f
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.bg_pill_yellow)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(54, 54)
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
            binding.contenedorPasos.addView(fila)
        }
    }
}
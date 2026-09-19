package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.MainActivity
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.pasos_preparacion
import kotlinx.coroutines.launch

class PreparacionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_preparacion)

        // Botón volver
        val btnVolver = findViewById<ImageView>(R.id.btnVolver)
        btnVolver?.setOnClickListener { finish() }

        // Botón marcar como preparado
        val btnMarcarPreparado = findViewById<Button>(R.id.btnMarcarPreparado)
        val preferencias = getSharedPreferences("SIRAE", MODE_PRIVATE)
        val preparado = preferencias.getBoolean("plato_preparado", false)

        if (preparado && btnMarcarPreparado != null) {
            btnMarcarPreparado.text = "✓   Preparado"
            btnMarcarPreparado.isEnabled = false
            btnMarcarPreparado.alpha = 0.6f
        }

        btnMarcarPreparado?.setOnClickListener {
            preferencias.edit().putBoolean("plato_preparado", true).apply()
            btnMarcarPreparado.text = "✓   Preparado"
            btnMarcarPreparado.isEnabled = false
            btnMarcarPreparado.alpha = 0.6f
            Toast.makeText(this, "¡Plato marcado como preparado!", Toast.LENGTH_SHORT).show()
        }

        // Barra de navegación
        findViewById<LinearLayout>(R.id.navInicio)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navAsignadas)?.setOnClickListener {
            startActivity(Intent(this, ComponentesActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navInventario)?.setOnClickListener {
            startActivity(Intent(this, InventarioActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navAvisos)?.setOnClickListener {
            startActivity(Intent(this, AvisosActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navPerfil)?.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java)); finish()
        }

        // Mostrar nombre e imagen del plato si viene del intent
        val nombrePlato = intent.getStringExtra("nombre_plato") ?: "Bandeja Paisa"
        val tvNombre = findViewById<TextView>(R.id.tvNombrePlatoPreparacion)
        val nombreFormateado = nombrePlato.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        tvNombre?.text = nombreFormateado

        val imgPlato = findViewById<ImageView>(R.id.imgPlatoPreparacion)
        val imgRes = when {
            nombrePlato.contains("bandeja", ignoreCase = true) || nombrePlato.contains("frijol", ignoreCase = true) -> R.drawable.frijoles
            nombrePlato.contains("chocolate", ignoreCase = true) -> R.drawable.chocolate
            nombrePlato.contains("huevo", ignoreCase = true) -> R.drawable.huevo_perico
            nombrePlato.contains("pollo", ignoreCase = true) -> R.drawable.apanado
            nombrePlato.contains("arroz", ignoreCase = true) -> R.drawable.arroz_de_leche
            else -> R.drawable.frijoles
        }
        imgPlato?.setImageResource(imgRes)

        // Cargar pasos desde el backend
        cargarPasos()
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
        val contenedor = findViewById<LinearLayout>(R.id.contenedorPasos) ?: return
        contenedor.removeAllViews()

        for (paso in pasos) {
            // Fila horizontal: número + descripción
            val fila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 36, 0, 0)
            }

            // Círculo con el número del paso
            val tvNumero = TextView(this).apply {
                text = paso.numeroPaso
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.bg_numero_paso)
                layoutParams = LinearLayout.LayoutParams(72, 72)
            }

            // Descripción del paso
            val tvDescripcion = TextView(this).apply {
                text = paso.descripcionPaso
                setTextColor(0xFF34443A.toInt())
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                ).also { it.marginStart = 30 }
            }

            fila.addView(tvNumero)
            fila.addView(tvDescripcion)
            contenedor.addView(fila)
        }
    }
}
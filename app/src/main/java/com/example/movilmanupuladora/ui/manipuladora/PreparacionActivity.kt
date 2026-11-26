package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
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
import com.example.movilmanupuladora.ui.manipuladora_Jefe.PreparacionEstadoManager
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PreparacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreparacionBinding
    private var nombrePlatoActual: String = "Pollo Guisado Criollo"
    private var componenteAsignado: String? = null
    private var idPlatoLocal: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPreparacionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVolver.setOnClickListener { finish() }

        val sessionMgr = SessionManager(this)
        componenteAsignado = intent.getStringExtra("componente_seleccionado")
            ?: sessionMgr.getComponenteAsignado(this)

        val platoIntent = intent.getStringExtra("nombre_plato")
        val platoJefa = sessionMgr.getPlatoJefa(this).takeIf { it.isNotBlank() }
        val platoResolver = (platoIntent ?: platoJefa)?.takeIf { it.isNotBlank() }
        idPlatoLocal = intent.getIntExtra("id_plato", -1)

        if (platoResolver.isNullOrBlank()) {
            nombrePlatoActual = "Menú no seleccionado"
            binding.tvNombrePlatoPreparacion.text = "La jefa aún no ha seleccionado el menú de hoy"
        } else {
            nombrePlatoActual = platoResolver
            val nombreFormateado = nombrePlatoActual.split(" ").joinToString(" ") { palabra ->
                palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            binding.tvNombrePlatoPreparacion.text = nombreFormateado
        }

        val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
        val miCorreo = sessionMgr.getUserEmail() ?: ""

        if (!componenteAsignado.isNullOrBlank()) {
            val estadoActual = PreparacionEstadoManager.obtenerEstado(
                this,
                miNombre,
                componenteAsignado,
                miCorreo
            )
            actualizarVistaEstado(estadoActual)
            binding.tvComponentePreparacion.text = "Estación Proteica y Huevos"
            binding.btnMarcarPreparado.visibility = View.VISIBLE
        } else {
            binding.badgeTurnoPreparacion.text = "Estado: Sin asignación"
            binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_tag_plato)
            binding.badgeTurnoPreparacion.setTextColor(0xFF64748B.toInt())
            binding.tvComponentePreparacion.text = "Sin componente asignado"
            binding.btnMarcarPreparado.visibility = View.GONE
        }

        binding.btnMarcarPreparado.setOnClickListener {
            mostrarDialogoEstadoPreparacion()
        }

        configurarPestañasEstado()

        val imgIntent = intent.getIntExtra("id_imagen", -1)
        val imgRes = if (imgIntent > 0) imgIntent else com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity.obtenerImagenPlatoPorNombre(nombrePlatoActual)
        binding.imgPlatoPreparacion.setImageResource(imgRes)

        cargarPasosDesdeBackend()
    }

    private fun configurarPestañasEstado() {
        val tabPorHacer = findViewById<TextView>(R.id.tabPorHacer)
        val tabEnProceso = findViewById<TextView>(R.id.tabEnProceso)
        val tabListo = findViewById<TextView>(R.id.tabListo)

        val sessionMgr = SessionManager(this)
        val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
        val miCorreo = sessionMgr.getUserEmail() ?: ""
        val comp = componenteAsignado ?: "Preparación general"
        val estadoActual = PreparacionEstadoManager.obtenerEstado(this, miNombre, comp, miCorreo)

        fun destacarTab(activo: TextView?) {
            val tabs = listOf(tabPorHacer, tabEnProceso, tabListo)
            for (t in tabs) {
                if (t == activo) {
                    t?.setBackgroundResource(R.drawable.bg_pill_green)
                    t?.setTextColor(Color.WHITE)
                } else {
                    t?.background = null
                    t?.setTextColor(Color.parseColor("#64748B"))
                }
            }
        }

        when (estadoActual) {
            PreparacionEstadoManager.ESTADO_EN_PROCESO -> destacarTab(tabEnProceso)
            PreparacionEstadoManager.ESTADO_FINALIZADA -> destacarTab(tabListo)
            else -> destacarTab(tabPorHacer)
        }

        tabPorHacer?.setOnClickListener {
            destacarTab(tabPorHacer)
            ejecutarCambioEstadoDirecto(PreparacionEstadoManager.ESTADO_POR_HACER)
        }
        tabEnProceso?.setOnClickListener {
            destacarTab(tabEnProceso)
            ejecutarCambioEstadoDirecto(PreparacionEstadoManager.ESTADO_EN_PROCESO)
        }
        tabListo?.setOnClickListener {
            destacarTab(tabListo)
            ejecutarCambioEstadoDirecto(PreparacionEstadoManager.ESTADO_FINALIZADA)
        }
    }

    private fun actualizarVistaEstado(estado: String) {
        val tabPorHacer = findViewById<TextView>(R.id.tabPorHacer)
        val tabEnProceso = findViewById<TextView>(R.id.tabEnProceso)
        val tabListo = findViewById<TextView>(R.id.tabListo)

        fun destacarTab(activo: TextView?) {
            val tabs = listOf(tabPorHacer, tabEnProceso, tabListo)
            for (t in tabs) {
                if (t == activo) {
                    t?.setBackgroundResource(R.drawable.bg_pill_green)
                    t?.setTextColor(Color.WHITE)
                } else {
                    t?.background = null
                    t?.setTextColor(Color.parseColor("#64748B"))
                }
            }
        }

        when (estado) {
            PreparacionEstadoManager.ESTADO_POR_HACER -> {
                binding.badgeTurnoPreparacion.text = "Estado: Por hacer"
                binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_yellow)
                binding.badgeTurnoPreparacion.setTextColor(0xFF7A5500.toInt())
                binding.btnMarcarPreparado.text = "Actualizar Estado de Preparación"
                destacarTab(tabPorHacer)
            }
            PreparacionEstadoManager.ESTADO_EN_PROCESO -> {
                binding.badgeTurnoPreparacion.text = "Estado: En preparación"
                binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_yellow)
                binding.badgeTurnoPreparacion.setTextColor(0xFF855B00.toInt())
                binding.btnMarcarPreparado.text = "Actualizar Estado de Preparación"
                destacarTab(tabEnProceso)
            }
            PreparacionEstadoManager.ESTADO_FINALIZADA -> {
                binding.badgeTurnoPreparacion.text = "Estado: Listo"
                binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_green)
                binding.badgeTurnoPreparacion.setTextColor(0xFF176B35.toInt())
                binding.btnMarcarPreparado.text = "Actualizar Estado de Preparación"
                destacarTab(tabListo)
            }
            else -> {
                binding.badgeTurnoPreparacion.text = "Estado: $estado"
                binding.badgeTurnoPreparacion.setBackgroundResource(R.drawable.bg_pill_yellow)
                binding.btnMarcarPreparado.text = "Actualizar Estado de Preparación"
                destacarTab(tabPorHacer)
            }
        }
    }

    private fun cargarPasosDesdeBackend() {
        val pasosDinamicos = obtenerPasosComponente(componenteAsignado ?: nombrePlatoActual)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerPasosPreparacion()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val todosPasos = response.body()!!
                    val pasosBackend = if (idPlatoLocal > 0) {
                        todosPasos.filter { it.idPlato == idPlatoLocal }
                    } else {
                        emptyList()
                    }
                    val pasosFinales = if (pasosBackend.isNotEmpty()) pasosBackend else pasosDinamicos
                    mostrarPasos(pasosFinales)
                } else {
                    mostrarPasos(pasosDinamicos)
                }
            } catch (e: Exception) {
                mostrarPasos(pasosDinamicos)
            }
        }
    }

    private fun obtenerPasosComponente(componente: String): List<pasos_preparacion> {
        val c = componente.lowercase().trim()
        return when {
            c.contains("huevo") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Lavado y desinfección de vegetales y selección de huevos frescos."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Preparación del sofrito de tomate y cebolla con sal y condimentos."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Incorporación de los huevos batidos y cocción a fuego medio hasta punto perfecto."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Porcionamiento en bandeja térmica acompañado de arroz blanco.")
            )
            c.contains("pollo") -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Descongelación y lavado de piezas de pollo bajo estrictas normas de inocuidad."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Sazón con ajo, comino, sal y especias de la receta institucional."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Cocción a fuego moderado hasta alcanzar temperatura interna superior a 75°C."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Servido de porciones estandarizadas según gramaje escolar.")
            )
            else -> listOf(
                pasos_preparacion(idPlato = 1, numeroPaso = "1", descripcionPaso = "Recepción, lavado y desinfección de materia prima e insumos de cocina."),
                pasos_preparacion(idPlato = 1, numeroPaso = "2", descripcionPaso = "Alistamiento de utensilios y preparación previa (Mise en place)."),
                pasos_preparacion(idPlato = 1, numeroPaso = "3", descripcionPaso = "Cocción controlada verificando temperatura interna y estándar PAE."),
                pasos_preparacion(idPlato = 1, numeroPaso = "4", descripcionPaso = "Porcionamiento final y control de gramaje para entrega a estudiantes.")
            )
        }
    }

    private fun mostrarPasos(pasos: List<pasos_preparacion>) {
        binding.contenedorPasos.removeAllViews()

        for ((index, paso) in pasos.withIndex()) {
            val itemBinding = com.example.movilmanupuladora.databinding.ItemPasoCocinaBinding.inflate(
                layoutInflater,
                binding.contenedorPasos,
                false
            )

            itemBinding.tvNumeroPaso.text = paso.numeroPaso.ifBlank { "${index + 1}" }

            val descOriginal = paso.descripcionPaso.replaceFirst(Regex("^\\[[^\\]]+\\]\\s*"), "")
            val partes = descOriginal.split(":", limit = 2)
            if (partes.size > 1) {
                itemBinding.tvTituloPaso.text = partes[0].trim()
                itemBinding.tvDescripcionPaso.text = partes[1].trim()
            } else {
                itemBinding.tvTituloPaso.text = "Paso ${index + 1} de cocción"
                itemBinding.tvDescripcionPaso.text = descOriginal
            }

            val tiempoEst = when (index) {
                0 -> "10 min"
                1 -> "22 min"
                2 -> "14 min"
                else -> "15 min"
            }
            itemBinding.tvTiempoPaso.text = tiempoEst

            binding.contenedorPasos.addView(itemBinding.root)
        }
    }

    private fun mostrarDialogoEstadoPreparacion() {
        val opciones = listOf(
            "Por hacer / Pendiente",
            "En proceso / En preparación",
            "Finalizada / Completada"
        )

        val dialogBinding = com.example.movilmanupuladora.databinding.DialogOpcionesJefaBinding.inflate(layoutInflater)
        dialogBinding.txtTituloOpciones.text = "Actualizar Estado de Preparación"
        dialogBinding.contenedorBotonesOpciones.removeAllViews()

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        for ((index, opcion) in opciones.withIndex()) {
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = opcion
                isAllCaps = false
                setTextColor(Color.parseColor("#1E293B"))
                textSize = 14f
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                strokeWidth = (1 * resources.displayMetrics.density).toInt()
                cornerRadius = (12 * resources.displayMetrics.density).toInt()
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                setPadding((14 * resources.displayMetrics.density).toInt(), (12 * resources.displayMetrics.density).toInt(), (14 * resources.displayMetrics.density).toInt(), (12 * resources.displayMetrics.density).toInt())

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * resources.displayMetrics.density).toInt()
                }

                setOnClickListener {
                    dialog.dismiss()
                    ejecutarCambioEstadoPreparacion(index)
                }
            }
            dialogBinding.contenedorBotonesOpciones.addView(btn)
        }

        dialogBinding.btnCancelarOpciones.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun ejecutarCambioEstadoPreparacion(index: Int) {
        val nuevoEstado = when (index) {
            0 -> PreparacionEstadoManager.ESTADO_POR_HACER
            1 -> PreparacionEstadoManager.ESTADO_EN_PROCESO
            2 -> PreparacionEstadoManager.ESTADO_FINALIZADA
            else -> PreparacionEstadoManager.ESTADO_POR_HACER
        }

        val sessionMgr = SessionManager(this)
        val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
        val miCorreo = sessionMgr.getUserEmail() ?: ""
        val comp = componenteAsignado ?: "Preparación general"

        PreparacionEstadoManager.guardarEstado(this, miNombre, comp, miCorreo, nuevoEstado)
        actualizarVistaEstado(nuevoEstado)
        Toast.makeText(this, "Estado actualizado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
    }

    private fun ejecutarCambioEstadoDirecto(nuevoEstado: String) {
        val sessionMgr = SessionManager(this)
        val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
        val miCorreo = sessionMgr.getUserEmail() ?: ""
        val comp = componenteAsignado ?: "Preparación general"

        PreparacionEstadoManager.guardarEstado(this, miNombre, comp, miCorreo, nuevoEstado)
        actualizarVistaEstado(nuevoEstado)
        Toast.makeText(this, "Estado actualizado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
    }
}

package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityAsignadasBinding
import com.example.movilmanupuladora.databinding.DialogDetalleAsignacionBinding
import com.example.movilmanupuladora.utils.NavigationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsignadasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsignadasBinding

    private var idPlato: Int = -1
    private var nombrePlato: String = "Pollo Guisado Criollo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAsignadasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra("nombre_plato")) {
            nombrePlato = intent.getStringExtra("nombre_plato") ?: "Pollo Guisado Criollo"
        }
        idPlato = intent.getIntExtra("id_plato", -1)

        configurarEncabezadoYPlato()
        configurarAcciones()
        configurarListaOperarias()
        configurarNavegacion()
    }

    private fun configurarEncabezadoYPlato() {
        val fechaHoy = SimpleDateFormat("EEEE / d / MMM / yyyy", Locale("es", "CO"))
            .format(Date())
            .uppercase()
        binding.tvFechaAsignadas.text = fechaHoy

        val nombreFormateado = nombrePlato.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        binding.tvNombreMiPlato.text = nombreFormateado

        val imgRes = when {
            nombrePlato.contains("bandeja", ignoreCase = true) -> R.drawable.bandeja_paisa
            nombrePlato.contains("frijol", ignoreCase = true) -> R.drawable.frijoles
            nombrePlato.contains("chocolate", ignoreCase = true) -> R.drawable.chocolate
            nombrePlato.contains("huevo", ignoreCase = true) -> R.drawable.huevo_perico
            nombrePlato.contains("pollo", ignoreCase = true) -> R.drawable.pollo_guisado
            nombrePlato.contains("arroz", ignoreCase = true) -> R.drawable.arroz_de_leche
            else -> R.drawable.comida_almuerzo
        }
        binding.imgMiPlato.setImageResource(imgRes)
    }

    private fun configurarAcciones() {
        binding.btnVerInsumos.setOnClickListener {
            val intent = Intent(this, IngredientesActivity::class.java).apply {
                putExtra("id_plato", idPlato)
                putExtra("nombre_plato", nombrePlato)
            }
            startActivity(intent)
        }

        binding.btnIniciarPreparacionTask.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                putExtra("id_plato", idPlato)
                putExtra("nombre_plato", nombrePlato)
            }
            startActivity(intent)
        }

        binding.cardMiPlatoAsignado.setOnClickListener {
            mostrarDialogoDetalleAsignacion("Preparación del Componente Sazón y Proteína", "María López")
        }

        binding.cardVerEquipo.setOnClickListener {
            if (binding.layoutEquipoOperarias.visibility == View.VISIBLE) {
                binding.layoutEquipoOperarias.visibility = View.GONE
            } else {
                binding.layoutEquipoOperarias.visibility = View.VISIBLE
            }
        }

        binding.cardPasoAsignado1.setOnClickListener {
            mostrarDialogoDetalleAsignacion("Pesado y verificación de insumos", "Ana López")
        }

        binding.cardPasoAsignado2.setOnClickListener {
            mostrarDialogoDetalleAsignacion("Alistamiento e inocuidad (Mise en place)", "María Rodríguez")
        }

        binding.cardPasoAsignado3.setOnClickListener {
            mostrarDialogoDetalleAsignacion("Cocción y empaque/entrega", "María López")
        }
    }

    private fun mostrarDialogoDetalleAsignacion(nombrePaso: String, operaria: String) {
        val dialogBinding = DialogDetalleAsignacionBinding.inflate(layoutInflater)
        dialogBinding.tvAsignacionTitulo.text = "DETALLE DE ASIGNACIÓN PAE"
        dialogBinding.tvNombrePlatoAsignacion.text = nombrePlato
        dialogBinding.tvEstadoAsignacion.text = "Responsable: $operaria · Turno Mañana"
        dialogBinding.tvPaso1.text = "1. Recepción y desinfección de materia prima"
        dialogBinding.tvPaso2.text = "2. $nombrePaso"
        dialogBinding.tvPaso3.text = "3. Servido y control de gramaje escolar"
        dialogBinding.tvIngredientesAsignacion.text = "• Pollo fresco (18 kg)\n• Verduras y Guiso (8 kg)\n• Sazón y Sal Yodada (2 kg)"

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnMarcarCompletado.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(this, "¡Tarea '$nombrePaso' marcada como completada!", Toast.LENGTH_SHORT).show()
        }

        dialogBinding.btnCerrarDetalle.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun configurarListaOperarias() {
        val operarias = listOf(
            Pair("Sofía Rojas", "Ensaladas y Frutas • Ingredientes listos"),
            Pair("Daniela Gómez", "Ceviche y Principios • Ingredientes listos"),
            Pair("Elena Pérez", "Postres y Bebidas • En preparación"),
            Pair("Mara Silva", "Asados y Proteína • Ingredientes listos"),
            Pair("Vanesa Castro", "Acompañamientos • En preparación"),
            Pair("Dayana López", "Sopas y Sazón • Finalizado")
        )

        binding.contenedorListaOperarias.removeAllViews()

        for (op in operarias) {
            val itemCard = com.google.android.material.card.MaterialCardView(this).apply {
                cardElevation = 2f
                radius = 24f
                strokeWidth = 1
                setStrokeColor(0xFFE2E8F0.toInt())
                setCardBackgroundColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 8, 0, 8) }
            }

            val layoutFila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
            }

            val imgAvatar = android.widget.ImageView(this).apply {
                setImageResource(R.drawable.ic_person)
                layoutParams = LinearLayout.LayoutParams(64, 64)
            }

            val layoutInfo = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).also { it.marginStart = 20 }
            }

            val tvNombre = TextView(this).apply {
                text = op.first
                setTextColor(0xFF1E293B.toInt())
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvCargo = TextView(this).apply {
                text = op.second
                setTextColor(0xFF64748B.toInt())
                textSize = 11f
            }

            layoutInfo.addView(tvNombre)
            layoutInfo.addView(tvCargo)

            layoutFila.addView(imgAvatar)
            layoutFila.addView(layoutInfo)

            itemCard.addView(layoutFila)

            itemCard.setOnClickListener {
                mostrarDialogoDetalleAsignacion(op.second, op.first)
            }

            binding.contenedorListaOperarias.addView(itemCard)
        }
    }

    private fun configurarNavegacion() {
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.ASIGNADAS
        )
    }
}
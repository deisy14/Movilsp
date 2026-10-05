package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityAsignadasBinding
import com.example.movilmanupuladora.utils.NavigationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsignadasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsignadasBinding

    private var idPlato: Int = -1
    private var nombrePlato: String = "Arroz con Pollo"
    private var componenteAsignado: String = "Sopas, Sazón y Proteína"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAsignadasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtener extras enviadas desde MainActivity o Turno
        if (intent.hasExtra("nombre_plato")) {
            nombrePlato = intent.getStringExtra("nombre_plato") ?: "Arroz con Pollo"
        }
        if (intent.hasExtra("componente_seleccionado")) {
            componenteAsignado = intent.getStringExtra("componente_seleccionado") ?: "Sopas, Sazón y Proteína"
        }
        idPlato = intent.getIntExtra("id_plato", -1)

        configurarEncabezadoYPlato()
        configurarAcciones()
        configurarListaOperarias()
        configurarNavegacion()
    }

    private fun configurarEncabezadoYPlato() {
        val fechaHoy = SimpleDateFormat("EEEE / d / MMM / yyyy", Locale.forLanguageTag("es-CO"))
            .format(Date())
            .uppercase()
        binding.tvFechaAsignadas.text = fechaHoy

        val nombreFormateado = nombrePlato.split(" ").joinToString(" ") { palabra ->
            palabra.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        binding.tvNombreMiPlato.text = nombreFormateado
        binding.tvComponenteMiPlato.text = "Te toca: $componenteAsignado"

        val imgRes = when {
            nombrePlato.contains("bandeja", ignoreCase = true) -> R.drawable.bandeja_paisa
            nombrePlato.contains("frijol", ignoreCase = true) -> R.drawable.frijoles
            nombrePlato.contains("chocolate", ignoreCase = true) -> R.drawable.chocolate
            nombrePlato.contains("huevo", ignoreCase = true) -> R.drawable.huevo_perico
            nombrePlato.contains("pollo", ignoreCase = true) -> R.drawable.arroz_pollo
            nombrePlato.contains("arroz", ignoreCase = true) -> R.drawable.arroz_de_leche
            else -> R.drawable.comida_almuerzo
        }
        binding.imgMiPlato.setImageResource(imgRes)
    }

    private fun configurarAcciones() {
        // Ir a ver insumos e ingredientes específicos de este plato
        binding.btnVerInsumos.setOnClickListener {
            val intent = Intent(this, IngredientesActivity::class.java).apply {
                putExtra("id_plato", idPlato)
                putExtra("nombre_plato", nombrePlato)
                putExtra("componente_seleccionado", componenteAsignado)
            }
            startActivity(intent)
        }

        // Iniciar preparación de la parte que le tocó a la manipuladora
        binding.btnIniciarPreparacionTask.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                putExtra("id_plato", idPlato)
                putExtra("nombre_plato", nombrePlato)
                putExtra("componente_seleccionado", componenteAsignado)
            }
            startActivity(intent)
        }

        // Mostrar / Ocultar distribución del equipo de cocineras
        binding.cardVerEquipo.setOnClickListener {
            if (binding.layoutEquipoOperarias.visibility == View.VISIBLE) {
                binding.layoutEquipoOperarias.visibility = View.GONE
            } else {
                binding.layoutEquipoOperarias.visibility = View.VISIBLE
            }
        }
    }

    private fun configurarListaOperarias() {
        val operarias = listOf(
            Pair("Sofía Rojas", "Ensaladas y Frutas • Ingredientes listos"),
            Pair("Daniela Gómez", "Principio y Granos • En alistamiento"),
            Pair("Elena Pérez", "Postres y Bebidas • Listo"),
            Pair("Mara Silva", "Sopas, Sazón y Proteína • Tu parte asignada"),
            Pair("Vanesa Castro", "Acompañamientos • En alistamiento")
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
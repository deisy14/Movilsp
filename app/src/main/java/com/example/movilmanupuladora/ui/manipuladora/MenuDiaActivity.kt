package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.SugerenciaIAItem
import com.example.movilmanupuladora.data.model.SugerenciasRecetasRequest
import com.example.movilmanupuladora.databinding.ActivityMenuDiaBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MenuDiaActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityMenuDiaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        enableEdgeToEdge()

        binding = ActivityMenuDiaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnVolverMenu.setOnClickListener { finish() }

        binding.btnVerComponentes.setOnClickListener {
            val intent = Intent(this, PreparacionActivity::class.java)
            startActivity(intent)
        }

        binding.btnIrInventarioMenu.setOnClickListener {
            val intent = Intent(this, InventarioActivity::class.java)
            startActivity(intent)
        }

        cargarPlatos()
    }

    private fun cargarPlatos() {
        lifecycleScope.launch {
            var resumenInvTexto = "Sin insumos registrados en tu sede"
            try {
                SessionManager.getToken(this@MenuDiaActivity)
                com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@MenuDiaActivity)

                // 1. Raciones reales
                val racionesLocal = AsistenciaManager.obtenerManana(this@MenuDiaActivity)
                var totalRaciones = racionesLocal
                runCatching {
                    val asistRes = RetrofitClient.apiService.obtenerAsistenciaDiaria()
                    if (asistRes.isSuccessful && asistRes.body() != null) {
                        val hoy = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        val lista = asistRes.body()!!
                        val deHoy = lista.filter { it.fecha.startsWith(hoy) }.ifEmpty { lista }
                        val sum = deHoy.sumOf { it.ninosPresentes }
                        if (sum > 0) totalRaciones = sum
                    }
                }
                binding.tvRacionesMenuDia.text = if (totalRaciones > 0) "$totalRaciones raciones" else "Sin registro"

                // 2. Inventario real filtrado por la sede de la manipuladora
                val sedeIdActual = sessionManager.getSedeId().takeIf { it > 0 }
                runCatching {
                    val invRes = RetrofitClient.apiService.obtenerInventario()
                    val ingRes = RetrofitClient.apiService.obtenerIngredientes()
                    val mapIng = ingRes.body().orEmpty().associateBy { it.obtenerIdReal() }
                    val todosInv = invRes.body().orEmpty()
                    val haySede = todosInv.any { (it.idSede ?: it.sede) != null }
                    val itemsSede = todosInv.filter { inv ->
                        val invSede = inv.idSede ?: inv.sede
                        sedeIdActual == null || !haySede || invSede == sedeIdActual
                    }
                    val disponibles = itemsSede.count { (it.cantidadActual?.toDoubleOrNull() ?: it.cantidad ?: 0.0) > 0.0 }
                    val agotados = itemsSede.size - disponibles
                    binding.tvResumenInventarioMenu.text = if (itemsSede.isEmpty()) {
                        "Sin insumos en tu sede"
                    } else {
                        "$disponibles insumos disponibles · $agotados agotados"
                    }
                    val sedeNombre = sessionManager.getSedeNombre()?.takeIf { it.isNotBlank() } ?: "tu sede"
                    binding.tvSubtituloInventarioMenu.text = "Stock verificado en $sedeNombre"

                    val topInsumos = itemsSede.take(3).mapNotNull { inv ->
                        val cant = inv.cantidadActual?.toDoubleOrNull() ?: inv.cantidad ?: 0.0
                        val ing = inv.obtenerIdIngredienteReal()?.let { mapIng[it] }
                        val nom = ing?.obtenerNombreReal()?.takeIf { it.isNotBlank() }
                            ?: inv.nombreIngrediente?.takeIf { it.isNotBlank() }
                            ?: inv.nombre?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        "$nom (${if (cant % 1.0 == 0.0) cant.toInt() else cant})"
                    }
                    if (topInsumos.isNotEmpty()) {
                        resumenInvTexto = "En bodega: ${topInsumos.joinToString(" · ")}"
                    }
                }
            } catch (_: Exception) { }

            val platoJefa = sessionManager.getPlatoJefa(this@MenuDiaActivity)
            val componenteAsignado = sessionManager.getComponenteAsignado(this@MenuDiaActivity)
            val platoId = sessionManager.getPlatoJefaId(this@MenuDiaActivity)

            if (platoJefa.isNotBlank()) {
                mostrarPlatos(
                    listOf(
                        PlatoResponse(
                            idPlato = if (platoId > 0) platoId else 1,
                            nombrePlato = platoJefa,
                            componente = componenteAsignado?.takeIf { it.isNotBlank() } ?: "Asignación de tu sede"
                        )
                    ),
                    resumenInvTexto
                )
            } else {
                mostrarPlatos(emptyList(), resumenInvTexto)
            }
        }
    }

    private fun mostrarPlatos(platos: List<PlatoResponse>, resumenInvTexto: String = "Stock de tu sede") {
        val contenedorPrincipal = findViewById<LinearLayout>(R.id.contenedorPlatoPrincipal)
        val contenedorOtros = findViewById<LinearLayout>(R.id.contenedorOtrosPlatos)

        contenedorPrincipal?.removeAllViews()
        contenedorOtros?.removeAllViews()

        if (platos.isEmpty()) {
            val tvVacio = TextView(this).apply {
                text = "Aún no tienes un menú asignado por la manipuladora jefa en tu sede."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 13.5f
                setPadding(16, 24, 16, 24)
            }
            contenedorPrincipal?.addView(tvVacio)
            return
        }

        for (plato in platos) {
            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 16 }
                radius = (20 * resources.displayMetrics.density)
                cardElevation = (2 * resources.displayMetrics.density)
                setCardBackgroundColor(Color.WHITE)
                strokeColor = Color.parseColor("#F5B81F")
                strokeWidth = (1.5f * resources.displayMetrics.density).toInt()
            }

            val cardLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Header verde institucional
            val headerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#2B5424"))
                setPadding(
                    (16 * resources.displayMetrics.density).toInt(),
                    (10 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (10 * resources.displayMetrics.density).toInt()
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val tvMealTitle = TextView(this).apply {
                text = "MENÚ ASIGNADO EN TU SEDE"
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvMealTime = TextView(this).apply {
                text = sessionManager.getSedeNombre()?.takeIf { it.isNotBlank() } ?: "Sede actual"
                setTextColor(Color.parseColor("#F5B81F"))
                textSize = 11.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            headerLayout.addView(tvMealTitle)
            headerLayout.addView(tvMealTime)
            cardLayout.addView(headerLayout)

            // Cuerpo de la tarjeta
            val bodyLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt()
                )
            }

            val rowContent = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val imgCard = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (80 * resources.displayMetrics.density).toInt(),
                    (80 * resources.displayMetrics.density).toInt()
                )
                radius = (16 * resources.displayMetrics.density)
                cardElevation = 0f
                setCardBackgroundColor(Color.WHITE)
                strokeColor = Color.parseColor("#F4B41F")
                strokeWidth = (1.5f * resources.displayMetrics.density).toInt()
            }

            val imageView = android.widget.ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setImageResource(R.drawable.plato1)
            }
            imgCard.addView(imageView)

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = (14 * resources.displayMetrics.density).toInt()
                }
            }

            val tvNombre = TextView(this).apply {
                text = plato.nombrePlato ?: "Plato PAE"
                setTextColor(Color.parseColor("#1E293B"))
                textSize = 16f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvComp = TextView(this).apply {
                text = plato.componente ?: "Estación Proteica"
                setTextColor(Color.parseColor("#64748B"))
                textSize = 12f
            }

            val tvDesc = TextView(this).apply {
                text = "Asignación activa de tu sede"
                setTextColor(Color.parseColor("#64748B"))
                textSize = 11.5f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = 2 }
            }

            textLayout.addView(tvNombre)
            textLayout.addView(tvComp)
            textLayout.addView(tvDesc)

            rowContent.addView(imgCard)
            rowContent.addView(textLayout)
            bodyLayout.addView(rowContent)

            // Pastilla de inventario interna
            val pillInventario = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = (12 * resources.displayMetrics.density).toInt() }
                radius = (12 * resources.displayMetrics.density)
                cardElevation = 0f
                setCardBackgroundColor(Color.parseColor("#E6EDE2"))
                strokeColor = Color.parseColor("#2B5424")
                strokeWidth = (1f * resources.displayMetrics.density).toInt()
            }

            val tvPill = TextView(this).apply {
                text = resumenInvTexto
                setTextColor(Color.parseColor("#2B5424"))
                textSize = 11.5f
                setPadding(
                    (12 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt()
                )
            }
            pillInventario.addView(tvPill)
            bodyLayout.addView(pillInventario)

            // Botón inferior dentro de la tarjeta
            val btnVerPrep = com.google.android.material.button.MaterialButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (44 * resources.displayMetrics.density).toInt()
                ).also { it.topMargin = (12 * resources.displayMetrics.density).toInt() }
                text = "Ver preparación e ingredientes  ➔"
                isAllCaps = false
                setTextColor(Color.parseColor("#1E293B"))
                textSize = 12.5f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                strokeColor = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                strokeWidth = (1f * resources.displayMetrics.density).toInt()
                cornerRadius = (12 * resources.displayMetrics.density).toInt()

                setOnClickListener {
                    val intent = Intent(this@MenuDiaActivity, PreparacionActivity::class.java).apply {
                        putExtra("nombre_plato", plato.nombrePlato)
                        putExtra("componente_seleccionado", plato.componente)
                    }
                    startActivity(intent)
                }
            }
            bodyLayout.addView(btnVerPrep)

            cardLayout.addView(bodyLayout)
            card.addView(cardLayout)
            contenedorPrincipal?.addView(card)
        }
    }
}


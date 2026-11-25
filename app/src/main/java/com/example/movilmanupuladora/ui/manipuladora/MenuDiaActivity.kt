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
            try {
                val response = RetrofitClient.apiService.obtenerPlatos()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val platos = response.body()!!
                    mostrarPlatos(platos)
                } else {
                    val platosPae = MenuPaeCatalog.menus.map {
                        PlatoResponse(idPlato = it.id, nombrePlato = it.nombre, componente = it.componente)
                    }
                    mostrarPlatos(platosPae)
                }
            } catch (_: Exception) {
                val platosPae = MenuPaeCatalog.menus.map {
                    PlatoResponse(idPlato = it.id, nombrePlato = it.nombre, componente = it.componente)
                }
                mostrarPlatos(platosPae)
            }
        }
    }

    private fun mostrarPlatos(platos: List<PlatoResponse>) {
        val contenedorPrincipal = findViewById<LinearLayout>(R.id.contenedorPlatoPrincipal)
        val contenedorOtros = findViewById<LinearLayout>(R.id.contenedorOtrosPlatos)
        val tvOtrosPlatos = findViewById<TextView>(R.id.tvOtrosPlatos)

        contenedorPrincipal?.removeAllViews()
        contenedorOtros?.removeAllViews()

        if (platos.isEmpty()) return

        val tiemposComida = listOf(
            Triple("DESAYUNO", "07:00 – 08:15", R.drawable.comida_desayuno),
            Triple("ALMUERZO", "11:30 – 13:00", R.drawable.plato4),
            Triple("MERIENDA", "09:30 – 10:15", R.drawable.comida_merienda)
        )

        for ((index, tiempo) in tiemposComida.withIndex()) {
            val plato = platos.getOrNull(index) ?: platos.first()

            val card = MaterialCardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 16 }
                radius = (20 * resources.displayMetrics.density)
                cardElevation = (2 * resources.displayMetrics.density)
                setCardBackgroundColor(Color.WHITE)
                strokeColor = Color.parseColor("#F4B41F")
                strokeWidth = (1.5f * resources.displayMetrics.density).toInt()
            }

            val cardLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Header verde oscuro
            val headerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor("#1B3317"))
                setPadding(
                    (16 * resources.displayMetrics.density).toInt(),
                    (10 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (10 * resources.displayMetrics.density).toInt()
                )
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val tvMealTitle = TextView(this).apply {
                text = tiempo.first
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvMealTime = TextView(this).apply {
                text = "🕒 ${tiempo.second}"
                setTextColor(Color.parseColor("#F4B41F"))
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
                setImageResource(tiempo.third)
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
                text = "Bebida láctea tibia (avena) · 8 Ingredientes"
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
                setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                strokeColor = Color.parseColor("#15803D")
                strokeWidth = (1f * resources.displayMetrics.density).toInt()
            }

            val tvPill = TextView(this).apply {
                text = "En bodega: Arroz (20K) · Aceite vegetal (15L) · Leche entera (30L)  ✔"
                setTextColor(Color.parseColor("#15803D"))
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

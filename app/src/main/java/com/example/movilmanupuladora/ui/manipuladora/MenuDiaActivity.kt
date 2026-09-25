package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
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
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.databinding.ActivityMenuDiaBinding
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch

class MenuDiaActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityMenuDiaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restaurar token
        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        enableEdgeToEdge()

        // Inicialización con ViewBinding
        binding = ActivityMenuDiaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajustar la pantalla a las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón "Ver componentes" -> Navega a ComponentesActivity
        binding.btnVerComponentes.setOnClickListener {
            val intent = Intent(this, AsignadasActivity::class.java)
            startActivity(intent)
        }

        // Cargar platos desde el backend
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
                    Toast.makeText(
                        this@MenuDiaActivity,
                        "Sin platos registrados",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MenuDiaActivity,
                    "Error al conectar: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
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

        // PRIMER PLATO = destacado
        val principal = platos.first()
        val cardPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_seccion_seleccionada)
            setPadding(36, 36, 36, 36)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvSeccion = TextView(this).apply {
            text = principal.componente ?: "Plato del día"
            setTextColor(0xFF173F2A.toInt())
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val tvAsignado = TextView(this).apply {
            text = "Plato asignado"
            setTextColor(0xFF65736A.toInt())
            textSize = 12f
            setPadding(0, 8, 0, 0)
        }

        val tvNombrePrincipal = TextView(this).apply {
            text = principal.nombrePlato ?: "Sin nombre"
            setTextColor(0xFF173F2A.toInt())
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 6, 0, 0)
        }

        infoLayout.addView(tvSeccion)
        infoLayout.addView(tvAsignado)
        infoLayout.addView(tvNombrePrincipal)
        cardPrincipal.addView(infoLayout)
        contenedorPrincipal?.addView(cardPrincipal)

        // OTROS PLATOS (del segundo en adelante)
        if (platos.size > 1) {
            tvOtrosPlatos?.visibility = View.VISIBLE
            tvOtrosPlatos?.text = "Otros platos del menú"

            for (i in 1 until platos.size) {
                val plato = platos[i]

                val fila = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(20, 26, 20, 26)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = 20 }
                }

                val tvNombre = TextView(this).apply {
                    text = plato.nombrePlato ?: "Sin nombre"
                    setTextColor(0xFF34443A.toInt())
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    ).also { it.marginStart = 30 }
                }

                val tvFlecha = TextView(this).apply {
                    text = "›"
                    setTextColor(0xFF173F2A.toInt())
                    textSize = 28f
                }

                fila.addView(tvNombre)
                fila.addView(tvFlecha)

                // Al tocar un plato secundario se puede navegar a ingredientes
                fila.setOnClickListener {
                    val intent = Intent(this@MenuDiaActivity, IngredientesActivity::class.java)
                    intent.putExtra("nombre_plato", plato.nombrePlato)
                    startActivity(intent)
                }

                contenedorOtros?.addView(fila)

                // Línea separadora
                val linea = View(this).apply {
                    setBackgroundColor(0xFFEDEDED.toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 2
                    )
                }
                contenedorOtros?.addView(linea)
            }
        }
    }
}

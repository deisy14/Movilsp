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
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.model.SugerenciaIAItem
import com.example.movilmanupuladora.data.model.SugerenciasRecetasRequest
import com.example.movilmanupuladora.databinding.ActivityMenuDiaBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MenuDiaActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var binding: ActivityMenuDiaBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restaurar token de sesión
        sessionManager = SessionManager(this)
        RetrofitClient.authToken = sessionManager.fetchAuthToken()

        enableEdgeToEdge()

        // Inicializar la vista
        binding = ActivityMenuDiaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ajustar la pantalla a las barras del sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Botón para ver componentes
        binding.btnVerComponentes.setOnClickListener {
            val intent = Intent(
                this@MenuDiaActivity,
                PreparacionActivity::class.java
            )
            startActivity(intent)
        }

        findViewById<View>(R.id.navAvisos)?.setOnClickListener {
            startActivity(
                Intent(
                    this@MenuDiaActivity,
                    AvisosActivity::class.java
                )
            )
        }

        // Cargar platos desde el backend
        cargarPlatos()
    }

    private fun cargarPlatos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerPlatos()

                if (response.isSuccessful && !response.body().isNullOrEmpty() && response.body()!!.size >= 15) {
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
        val contenedorPrincipal =
            findViewById<LinearLayout>(R.id.contenedorPlatoPrincipal)

        val contenedorOtros =
            findViewById<LinearLayout>(R.id.contenedorOtrosPlatos)

        val tvOtrosPlatos =
            findViewById<TextView>(R.id.tvOtrosPlatos)

        contenedorPrincipal?.removeAllViews()
        contenedorOtros?.removeAllViews()

        if (platos.isEmpty()) return

        // Primer plato destacado
        val principal = platos.first()

        val cardPrincipal = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_seccion_seleccionada)
            setPadding(36, 36, 36, 36)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val infoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
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

        // Evento de clic en el plato principal para invocar la IA adaptada
        cardPrincipal.setOnClickListener {
            solicitarSugerenciaIA(
                nombrePlato = principal.nombrePlato ?: "Plato del día",
                componente = principal.componente
            )
        }

        contenedorPrincipal?.addView(cardPrincipal)

        // Otros platos
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
                    ).also {
                        it.topMargin = 20
                    }
                }

                val tvNombre = TextView(this).apply {
                    text = plato.nombrePlato ?: "Sin nombre"
                    setTextColor(0xFF34443A.toInt())
                    textSize = 14f

                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).also {
                        it.marginStart = 30
                    }
                }

                val tvFlecha = TextView(this).apply {
                    text = "›"
                    setTextColor(0xFF173F2A.toInt())
                    textSize = 28f
                }

                fila.addView(tvNombre)
                fila.addView(tvFlecha)

                // Evento de clic en los otros platos para abrir ingredientes o solicitar IA
                fila.setOnClickListener {
                    solicitarSugerenciaIA(
                        nombrePlato = plato.nombrePlato ?: "Plato del día",
                        componente = plato.componente
                    )
                }

                contenedorOtros?.addView(fila)

                // Línea separadora
                val linea = View(this).apply {
                    setBackgroundColor(0xFFEDEDED.toInt())

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        2
                    )
                }

                contenedorOtros?.addView(linea)
            }
        } else {
            tvOtrosPlatos?.visibility = View.GONE
        }
    }

    // --- Integración de Sugerencia de Receta con IA adaptada al Inventario (Corrutina) ---
    private fun inferirSeccion(nombrePlato: String, componente: String?): Int {
        val texto = "$nombrePlato ${componente.orEmpty()}".lowercase()
        return when {
            texto.contains("merienda") || texto.contains("refrigerio") || texto.contains("yogurt") || texto.contains("colada") -> 3
            texto.contains("desayuno") || texto.contains("huevo") || texto.contains("arepa") || texto.contains("chocolate") || texto.contains("avena") -> 1
            else -> 2
        }
    }

    private fun solicitarSugerenciaIA(nombrePlato: String, componente: String?) {
        val idSede = sessionManager.getSedeId().takeIf { it > 0 } ?: 1
        val ninos = AsistenciaManager.obtenerTotal(this).takeIf { it > 0 } ?: 100
        val idSeccion = inferirSeccion(nombrePlato, componente)

        Toast.makeText(this, "Consultando sugerencia IA...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val request = SugerenciasRecetasRequest(
                    idSede = idSede,
                    ninosPresentes = ninos,
                    idSeccion = idSeccion,
                    preferencia = nombrePlato
                )
                val response = RetrofitClient.apiService.sugerirRecetas(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val lista = body.sugerenciasIaEstructuradas.orEmpty().ifEmpty {
                        body.platosEstipuladosPae.orEmpty()
                    }
                    val sugerencia = lista.firstOrNull()
                    if (sugerencia != null) {
                        mostrarIngredientesAdaptados(sugerencia, body.ninosPresentes ?: ninos)
                    } else {
                        Toast.makeText(
                            this@MenuDiaActivity,
                            "No se recibieron sugerencias IA para este plato.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@MenuDiaActivity,
                        "Error al consultar IA (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (t: Throwable) {
                Toast.makeText(this@MenuDiaActivity, "Fallo de red: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarIngredientesAdaptados(sugerencia: SugerenciaIAItem, ninos: Int) {
        val sb = StringBuilder()
        sb.append("Calculado para $ninos niños\n")
        if (sugerencia.componente.isNotBlank()) {
            sb.append("${sugerencia.componente}\n")
        }
        sb.append("\nIngredientes y bodega:\n")

        for (ing in sugerencia.ingredientes.orEmpty()) {
            val icono = if (ing.alcanza) "✓" else "⚠️"
            if (ing.cantidadTotal.isNotBlank()) {
                sb.append("$icono ${ing.nombreIngrediente}: ${ing.cantidadTotal} (Stock: ${ing.stockActual} ${ing.unidad})\n")
            } else {
                sb.append("• ${ing.nombreIngrediente}\n")
            }
        }

        val pasos = sugerencia.pasos.orEmpty()
        if (pasos.isNotEmpty()) {
            sb.append("\nPreparación sugerida:\n")
            pasos.forEachIndexed { idx, paso ->
                sb.append("${idx + 1}. $paso\n")
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("🤖 ${sugerencia.nombrePlato}")
            .setMessage(sb.toString().trim())
            .setPositiveButton("Entendido", null)
            .show()
    }
}
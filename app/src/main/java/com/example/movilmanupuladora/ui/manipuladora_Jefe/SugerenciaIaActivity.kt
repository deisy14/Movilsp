package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.DescontarInventarioRequest
import com.example.movilmanupuladora.data.model.IngredienteCalculadoIA
import com.example.movilmanupuladora.data.model.SugerenciaIAItem
import com.example.movilmanupuladora.data.model.SugerenciasRecetasRequest
import com.example.movilmanupuladora.databinding.ActivityAiRecetaBinding
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class SugerenciaIaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiRecetaBinding
    private lateinit var sessionManager: SessionManager

    private var seccionSeleccionadaId: Int = 1 // 1: Desayuno, 2: Almuerzo, 3: Merienda
    private var sugerenciaActual: SugerenciaIAItem? = null
    private var idSedeUsuario: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAiRecetaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        idSedeUsuario = sessionManager.getSedeId().takeIf { it > 0 } ?: 1

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarSelectorMomentos()
        configurarEventos()

        // Consultar automáticamente la sugerencia inicial con IA
        consultarSugerenciasConIA()
    }

    private fun configurarSelectorMomentos() {
        actualizarEstiloBotonMomento(binding.btnMomentoDesayuno, seccionSeleccionadaId == 1)
        actualizarEstiloBotonMomento(binding.btnMomentoAlmuerzo, seccionSeleccionadaId == 2)
        actualizarEstiloBotonMomento(binding.btnMomentoMerienda, seccionSeleccionadaId == 3)

        binding.btnMomentoDesayuno.setOnClickListener {
            seccionSeleccionadaId = 1
            actualizarEstilosMomentos()
            consultarSugerenciasConIA()
        }

        binding.btnMomentoAlmuerzo.setOnClickListener {
            seccionSeleccionadaId = 2
            actualizarEstilosMomentos()
            consultarSugerenciasConIA()
        }

        binding.btnMomentoMerienda.setOnClickListener {
            seccionSeleccionadaId = 3
            actualizarEstilosMomentos()
            consultarSugerenciasConIA()
        }
    }

    private fun actualizarEstilosMomentos() {
        actualizarEstiloBotonMomento(binding.btnMomentoDesayuno, seccionSeleccionadaId == 1)
        actualizarEstiloBotonMomento(binding.btnMomentoAlmuerzo, seccionSeleccionadaId == 2)
        actualizarEstiloBotonMomento(binding.btnMomentoMerienda, seccionSeleccionadaId == 3)
    }

    private fun actualizarEstiloBotonMomento(btn: MaterialButton, activo: Boolean) {
        if (activo) {
            btn.setBackgroundColor(Color.parseColor("#F4B41F"))
            btn.setTextColor(Color.parseColor("#1E293B"))
            btn.strokeWidth = 0
        } else {
            btn.setBackgroundColor(Color.TRANSPARENT)
            btn.setTextColor(Color.parseColor("#64748B"))
            btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
            btn.strokeWidth = 2
        }
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnGenerarIA.setOnClickListener {
            consultarSugerenciasConIA()
        }

        binding.btnConfirmarDescuento.setOnClickListener {
            confirmarYDescontarDeInventario()
        }

        binding.btnIrARedactar.setOnClickListener {
            try {
                val intent = Intent(this, RedactarRecetaActivity::class.java)
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Redacción manual de recetas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun consultarSugerenciasConIA() {
        val ninosTexto = binding.etNinosPresentes.text.toString().trim()
        val ninos = ninosTexto.toIntOrNull() ?: 200

        binding.layoutCargandoIA.visibility = View.VISIBLE
        binding.layoutResultadoIA.visibility = View.GONE
        binding.btnGenerarIA.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = SugerenciasRecetasRequest(
                    idSede = idSedeUsuario,
                    ninosPresentes = ninos,
                    idSeccion = seccionSeleccionadaId,
                    preferencia = ""
                )

                val response = RetrofitClient.apiService.sugerirRecetas(request)

                binding.layoutCargandoIA.visibility = View.GONE
                binding.btnGenerarIA.isEnabled = true

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val sugerencias = body.sugerenciasIaEstructuradas
                    if (sugerencias.isNotEmpty()) {
                        mostrarSugerenciaEnPantalla(sugerencias[0], ninos)
                    } else if (body.platosEstipuladosPae.isNotEmpty()) {
                        mostrarSugerenciaEnPantalla(body.platosEstipuladosPae[0], ninos)
                    } else {
                        Toast.makeText(
                            this@SugerenciaIaActivity,
                            "No se recibieron propuestas para este momento.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@SugerenciaIaActivity,
                        "Error del servidor al consultar IA: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                binding.layoutCargandoIA.visibility = View.GONE
                binding.btnGenerarIA.isEnabled = true
                Toast.makeText(
                    this@SugerenciaIaActivity,
                    "Error de conexión con IA: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarSugerenciaEnPantalla(sug: SugerenciaIAItem, ninos: Int) {
        sugerenciaActual = sug
        binding.layoutResultadoIA.visibility = View.VISIBLE

        binding.tvNombrePlatoIA.text = sug.nombrePlato
        binding.tvComponentesIA.text = sug.componente

        val momentoNombre = when (seccionSeleccionadaId) {
            1 -> "Desayuno • Coherencia PAE"
            3 -> "Merienda • Coherencia PAE"
            else -> "Almuerzo • Coherencia PAE"
        }
        binding.tvChipMomento.text = momentoNombre

        if (sug.viableConInventario) {
            binding.tvBadgeStock.text = "✓ Stock Disponible"
            binding.tvBadgeStock.setTextColor(Color.parseColor("#15803D"))
        } else {
            binding.tvBadgeStock.text = "⚠️ Stock Limitado"
            binding.tvBadgeStock.setTextColor(Color.parseColor("#D97706"))
        }

        binding.tvTituloIngredientes.text = "Ingredientes & Disminución de Inventario ($ninos Niños)"

        // Llenar lista dinámica de ingredientes
        binding.layoutListaIngredientes.removeAllViews()

        for (ing in sug.ingredientes) {
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 10, 0, 10)
            }

            // Fila 1: Nombre y Estado
            val fila1 = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val tvNombre = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = ing.nombreIngrediente
                setTextColor(Color.parseColor("#1E293B"))
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val tvEstado = TextView(this).apply {
                text = if (ing.alcanza) "✓ Alcanza" else "⚠️ Insuficiente"
                setTextColor(if (ing.alcanza) Color.parseColor("#15803D") else Color.parseColor("#E11D48"))
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            fila1.addView(tvNombre)
            fila1.addView(tvEstado)
            itemLayout.addView(fila1)

            // Fila 2: Porción por niño y Total para N niños
            val tvPorcion = TextView(this).apply {
                text = "Porción niño: ${ing.cantidadPorNino} | Total para $ninos niños: ${ing.cantidadTotal}"
                setTextColor(Color.parseColor("#475569"))
                textSize = 11f
            }
            itemLayout.addView(tvPorcion)

            // Fila 3: Disminución de inventario (Stock actual vs restante proyectado)
            val tvStock = TextView(this).apply {
                text = "Stock bodega: ${ing.stockActual} ${ing.unidad}  ➔  Quedarán: ${ing.stockRestanteProyectado} ${ing.unidad} (-${ing.cantidadTotalValor} ${ing.unidad})"
                setTextColor(Color.parseColor("#0369A1"))
                textSize = 11f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            itemLayout.addView(tvStock)

            // Separador tenue
            val separador = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply { setMargins(0, 8, 0, 0) }
                setBackgroundColor(Color.parseColor("#F1F5F9"))
            }
            itemLayout.addView(separador)

            binding.layoutListaIngredientes.addView(itemLayout)
        }
    }

    private fun confirmarYDescontarDeInventario() {
        val sug = sugerenciaActual ?: return
        val ninosTexto = binding.etNinosPresentes.text.toString().trim()
        val ninos = ninosTexto.toIntOrNull() ?: 200

        AlertDialog.Builder(this)
            .setTitle("Confirmar Preparación")
            .setMessage("¿Deseas registrar este menú para $ninos niños y descontar automáticamente los ingredientes de la bodega?")
            .setPositiveButton("Sí, Descontar") { _, _ ->
                ejecutarDescuentoEnBackend(sug, ninos)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarDescuentoEnBackend(sug: SugerenciaIAItem, ninos: Int) {
        binding.btnConfirmarDescuento.isEnabled = false
        binding.btnConfirmarDescuento.text = "Descontando de bodega..."

        lifecycleScope.launch {
            try {
                val req = DescontarInventarioRequest(
                    idSede = idSedeUsuario,
                    idPlato = sug.idPlato,
                    nombrePlato = sug.nombrePlato,
                    ninosPresentes = ninos,
                    ingredientes = sug.ingredientes
                )

                val res = RetrofitClient.apiService.descontarInventario(req)

                binding.btnConfirmarDescuento.isEnabled = true
                binding.btnConfirmarDescuento.text = "✓ Confirmar Menú y Descontar de Bodega"

                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    val detalle = body.descuentos.joinToString("\n") { d ->
                        "• ${d.ingrediente}: se descontaron ${d.cantidadDescontada} ${d.unidad} (Quedan: ${d.stockRestante} ${d.unidad})"
                    }

                    AlertDialog.Builder(this@SugerenciaIaActivity)
                        .setTitle("¡Inventario Descontado con Éxito!")
                        .setMessage("${body.mensaje}\n\n$detalle")
                        .setPositiveButton("Aceptar") { _, _ ->
                            setResult(RESULT_OK)
                            finish()
                        }
                        .show()
                } else {
                    Toast.makeText(
                        this@SugerenciaIaActivity,
                        "Error al descontar inventario: ${res.code()}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                binding.btnConfirmarDescuento.isEnabled = true
                binding.btnConfirmarDescuento.text = "✓ Confirmar Menú y Descontar de Bodega"
                Toast.makeText(
                    this@SugerenciaIaActivity,
                    "Error de conexión: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

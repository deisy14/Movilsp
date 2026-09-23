package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.Notificacion
import com.example.movilmanupuladora.databinding.ActivityAvisosBinding
import kotlinx.coroutines.launch

class AvisosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvisosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAvisosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarBarraNavegacion()
        cargarNotificaciones()
    }

    private val notificacionesFallback = listOf(
        Notificacion(
            idNotificacion = 1,
            titulo = "Revisa el inventario de entrega",
            mensaje = "El supervisor programó la entrega de insumos quincenal. Recuerda verificar las cantidades recibidas.",
            fecha = "Hoy · 7:30 a. m.",
            tipo = "importante"
        ),
        Notificacion(
            idNotificacion = 2,
            titulo = "Confirmación de menú escolar",
            mensaje = "El plato asignado para el almuerzo de hoy es Bandeja Paisa. Consulta las porciones y componentes.",
            fecha = "Hoy · 6:45 a. m.",
            tipo = "menu"
        ),
        Notificacion(
            idNotificacion = 3,
            titulo = "Recordatorio de bioseguridad",
            mensaje = "Lavar y desinfectar superficies y utensilios antes de iniciar la cocción según protocolo PAE.",
            fecha = "Ayer",
            tipo = "info"
        )
    )

    private fun cargarNotificaciones() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerNotificaciones()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    mostrarNotificaciones(response.body()!!)
                } else {
                    mostrarNotificaciones(notificacionesFallback)
                }
            } catch (e: Exception) {
                mostrarNotificaciones(notificacionesFallback)
            }
        }
    }

    private fun mostrarNotificaciones(lista: List<Notificacion>) {
        val contenedor = binding.contenedorAvisos
        contenedor.removeAllViews()

        for (notif in lista) {
            val card = CardView(this).apply {
                radius = 16f * resources.displayMetrics.density
                cardElevation = 2f * resources.displayMetrics.density
                setCardBackgroundColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.bottomMargin = (10 * resources.displayMetrics.density).toInt()
                }
            }

            val layoutFila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt()
                )
            }

            // Ícono visual
            val tvIcono = TextView(this).apply {
                val size = (42 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size)
                gravity = android.view.Gravity.CENTER
                text = "!"
                setTextColor(0xFF765B00.toInt())
                textSize = 20f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setBackgroundColor(0xFFFFF1B8.toInt())
            }

            val layoutTexto = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).also {
                    it.marginStart = (12 * resources.displayMetrics.density).toInt()
                }
            }

            val tvTitulo = TextView(this).apply {
                text = notif.titulo ?: "Aviso del sistema"
                setTextColor(0xFF151515.toInt())
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvMensaje = TextView(this).apply {
                text = notif.mensaje ?: ""
                setTextColor(0xFF5F5F5F.toInt())
                textSize = 13f
                setPadding(0, 4, 0, 0)
            }

            val tvFecha = TextView(this).apply {
                text = notif.fecha ?: "Reciente"
                setTextColor(0xFF929292.toInt())
                textSize = 11f
                setPadding(0, 6, 0, 0)
            }

            layoutTexto.addView(tvTitulo)
            layoutTexto.addView(tvMensaje)
            layoutTexto.addView(tvFecha)

            layoutFila.addView(tvIcono)
            layoutFila.addView(layoutTexto)
            card.addView(layoutFila)
            contenedor.addView(card)
        }
    }

    private fun configurarBarraNavegacion() {
        binding.barraNavegacion.navInicio.setOnClickListener {

            startActivity(
                Intent(this, MainActivity::class.java)
            )

            finish()
        }


        // ASIGNADAS
        binding.barraNavegacion.navAsignadas.setOnClickListener {

            startActivity(
                Intent(this, ComponentesActivity::class.java)
            )

            finish()
        }


        // INVENTARIO
        binding.barraNavegacion.navInventario.setOnClickListener {

            startActivity(
                Intent(this, InventarioActivity::class.java)
            )

            finish()
        }


        // AVISOS
        binding.barraNavegacion.navAvisos.setOnClickListener {
            // Ya estamos en Avisos.
        }


        // PERFIL
        binding.barraNavegacion.navPerfil.setOnClickListener {

            startActivity(
                Intent(this, PerfilActivity::class.java)
            )

            finish()
        }
    }
}


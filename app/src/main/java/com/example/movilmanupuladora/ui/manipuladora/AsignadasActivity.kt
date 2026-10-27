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
import kotlinx.coroutines.launch
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityAsignadasBinding
import com.example.movilmanupuladora.databinding.DialogDetalleAsignacionBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.ManipuladorasManager
import com.example.movilmanupuladora.ui.manipuladora_Jefe.PreparacionEstadoManager
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsignadasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsignadasBinding
    private lateinit var sessionMgr: SessionManager

    private var idPlato: Int = -1
    private var nombrePlato: String = "Pollo Guisado Criollo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAsignadasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionMgr = SessionManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val platoJefa = sessionMgr.getPlatoJefa(this)
        nombrePlato = if (intent.hasExtra("nombre_plato")) {
            intent.getStringExtra("nombre_plato") ?: platoJefa
        } else {
            platoJefa
        }
        idPlato = intent.getIntExtra("id_plato", sessionMgr.getPlatoJefaId(this))

        configurarEncabezadoYPlato()
        configurarAcciones()
        configurarListaOperarias()
        configurarNavegacion()

        sincronizarConBackend()
    }

    override fun onResume() {
        super.onResume()
        sincronizarConBackend()
    }

    private fun sincronizarConBackend() {
        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@AsignadasActivity)
                val platoActualizado = sessionMgr.getPlatoJefa(this@AsignadasActivity)
                if (!intent.hasExtra("nombre_plato") || intent.getStringExtra("nombre_plato").isNullOrBlank()) {
                    nombrePlato = platoActualizado
                }
                idPlato = sessionMgr.getPlatoJefaId(this@AsignadasActivity)
                configurarEncabezadoYPlato()
                configurarListaOperarias()
            } catch (_: Exception) { }
        }
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

        val compAsignado = sessionMgr.getComponenteAsignado(this)
        if (!compAsignado.isNullOrBlank()) {
            val miNombre = sessionMgr.getUserName() ?: ""
            val miCorreo = sessionMgr.getUserEmail() ?: ""
            val miEstado = PreparacionEstadoManager.obtenerEstado(this, miNombre, compAsignado, miCorreo)
            binding.tvComponenteMiPlato.text = "🥣 Tu componente: $compAsignado • $miEstado"
        } else {
            binding.tvComponenteMiPlato.text = "Componente pendiente de asignación"
        }

        val imgRes = InicioJefaActivity.obtenerImagenPlatoPorNombre(nombrePlato)
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
            val compAsignado = sessionMgr.getComponenteAsignado(this)
            val intent = Intent(this, PreparacionActivity::class.java).apply {
                putExtra("id_plato", idPlato)
                putExtra("nombre_plato", nombrePlato)
                putExtra("componente_seleccionado", compAsignado)
                putExtra("id_imagen", InicioJefaActivity.obtenerImagenPlatoPorNombre(nombrePlato))
            }
            startActivity(intent)
        }

        binding.cardMiPlatoAsignado.setOnClickListener {
            val compAsignado = sessionMgr.getComponenteAsignado(this) ?: "Preparación general"
            val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
            mostrarDialogoDetalleAsignacion("Preparación de $compAsignado", miNombre)
        }

        binding.cardVerEquipo.setOnClickListener {
            if (binding.layoutEquipoOperarias.visibility == View.VISIBLE) {
                binding.layoutEquipoOperarias.visibility = View.GONE
            } else {
                binding.layoutEquipoOperarias.visibility = View.VISIBLE
            }
        }

        binding.cardPasoAsignado1.setOnClickListener {
            val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
            mostrarDialogoDetalleAsignacion("Pesado y verificación de insumos", miNombre)
        }

        binding.cardPasoAsignado2.setOnClickListener {
            val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
            mostrarDialogoDetalleAsignacion("Alistamiento e inocuidad (Mise en place)", miNombre)
        }

        binding.cardPasoAsignado3.setOnClickListener {
            val miNombre = sessionMgr.getUserName() ?: "Manipuladora"
            mostrarDialogoDetalleAsignacion("Cocción y servido escolar", miNombre)
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
        dialogBinding.tvIngredientesAsignacion.text = "• Insumos del componente asignado según receta PAE\n• Sazón y control de inocuidad\n• Raciones completas para estudiantes"

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
        val prefs = getSharedPreferences("ManipuladoraJefe", MODE_PRIVATE)
        val listaReal = ManipuladorasManager.obtenerManipuladoras().filter { it.activa }
        val miNombre = sessionMgr.getUserName()?.trim()?.lowercase() ?: ""
        val miCorreo = sessionMgr.getUserEmail()?.trim()?.lowercase() ?: ""

        binding.contenedorListaOperarias.removeAllViews()

        val compJefa = prefs.getString("comp_jefa", null)
        if (!compJefa.isNullOrBlank()) {
            val nombreJefa = "Isa Pérez (Jefa de Cocina)"
            val estadoJefa = PreparacionEstadoManager.obtenerEstado(this, "Isa Pérez", compJefa)
            val esJefaActual = miNombre.contains("isa") || sessionMgr.getUserRole()?.lowercase()?.contains("jef") == true

            val itemJefaCard = com.google.android.material.card.MaterialCardView(this).apply {
                cardElevation = if (esJefaActual) 4f else 2f
                radius = 24f
                strokeWidth = if (esJefaActual) 2 else 1
                setStrokeColor(if (esJefaActual) 0xFFF4B41F.toInt() else 0xFFE2E8F0.toInt())
                setCardBackgroundColor(if (esJefaActual) 0xFFFFFDF5.toInt() else 0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 8, 0, 8) }
            }

            val layoutJefaFila = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(24, 16, 24, 16)
            }

            val imgJefaAvatar = android.widget.ImageView(this).apply {
                setImageResource(R.drawable.ic_nav_perfil)
                setBackgroundResource(R.drawable.bg_icono_amarillo)
                setPadding(12, 12, 12, 12)
                layoutParams = LinearLayout.LayoutParams(64, 64)
            }

            val layoutJefaInfo = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                ).also { it.marginStart = 20 }
            }

            val tvNombreJ = TextView(this).apply {
                text = if (esJefaActual) "$nombreJefa (Tú)" else nombreJefa
                setTextColor(if (esJefaActual) 0xFF7A5500.toInt() else 0xFF1E293B.toInt())
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvCargoJ = TextView(this).apply {
                text = "🥣 Componente: $compJefa • $estadoJefa"
                val color = when (estadoJefa) {
                    PreparacionEstadoManager.ESTADO_FINALIZADA -> 0xFF176B35.toInt()
                    PreparacionEstadoManager.ESTADO_EN_PROCESO -> 0xFF855B00.toInt()
                    else -> 0xFF1B3317.toInt()
                }
                setTextColor(color)
                textSize = 11f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            layoutJefaInfo.addView(tvNombreJ)
            layoutJefaInfo.addView(tvCargoJ)

            layoutJefaFila.addView(imgJefaAvatar)
            layoutJefaFila.addView(layoutJefaInfo)

            itemJefaCard.addView(layoutJefaFila)
            itemJefaCard.setOnClickListener {
                mostrarDialogoDetalleAsignacion("Preparación de $compJefa", nombreJefa)
            }

            binding.contenedorListaOperarias.addView(itemJefaCard)
        }

        for (m in listaReal) {
            val keyCorreo = "comp_correo_" + m.correo.trim().replace("@", "_").replace(".", "_").lowercase()
            val keyNombre = "comp_" + m.nombre.trim().replace(" ", "_").lowercase()
            val comp = prefs.getString(keyCorreo, null) ?: prefs.getString(keyNombre, null)

            val esUsuarioActual = (miCorreo.isNotBlank() && m.correo.trim().equals(miCorreo, ignoreCase = true)) ||
                    (miNombre.isNotBlank() && (m.nombre.trim().equals(miNombre, ignoreCase = true) || m.nombre.lowercase().contains(miNombre) || miNombre.contains(m.nombre.lowercase())))

            val estadoOperaria = if (!comp.isNullOrBlank()) {
                PreparacionEstadoManager.obtenerEstado(this, m.nombre, comp, m.correo)
            } else {
                "Sin asignar"
            }

            val itemCard = com.google.android.material.card.MaterialCardView(this).apply {
                cardElevation = if (esUsuarioActual) 4f else 2f
                radius = 24f
                strokeWidth = if (esUsuarioActual) 2 else 1
                setStrokeColor(if (esUsuarioActual) 0xFFF4B41F.toInt() else 0xFFE2E8F0.toInt())
                setCardBackgroundColor(if (esUsuarioActual) 0xFFFFFDF5.toInt() else 0xFFFFFFFF.toInt())
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
                text = if (esUsuarioActual) "${m.nombre} (Tú)" else m.nombre
                setTextColor(if (esUsuarioActual) 0xFF7A5500.toInt() else 0xFF1E293B.toInt())
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }

            val tvCargo = TextView(this).apply {
                text = if (!comp.isNullOrBlank()) {
                    "🥣 Componente: $comp • $estadoOperaria"
                } else {
                    "${m.funcion} • Sin asignar"
                }
                val color = when (estadoOperaria) {
                    PreparacionEstadoManager.ESTADO_FINALIZADA -> 0xFF176B35.toInt()
                    PreparacionEstadoManager.ESTADO_EN_PROCESO -> 0xFF855B00.toInt()
                    PreparacionEstadoManager.ESTADO_POR_HACER -> 0xFF1B3317.toInt()
                    else -> 0xFF64748B.toInt()
                }
                setTextColor(color)
                textSize = 11f
                setTypeface(typeface, if (!comp.isNullOrBlank()) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            }

            layoutInfo.addView(tvNombre)
            layoutInfo.addView(tvCargo)

            layoutFila.addView(imgAvatar)
            layoutFila.addView(layoutInfo)

            itemCard.addView(layoutFila)

            itemCard.setOnClickListener {
                val detalle = if (!comp.isNullOrBlank()) "Preparación de $comp ($estadoOperaria)" else m.funcion
                mostrarDialogoDetalleAsignacion(detalle, m.nombre)
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
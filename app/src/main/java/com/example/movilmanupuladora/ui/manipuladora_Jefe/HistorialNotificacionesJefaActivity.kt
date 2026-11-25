package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityHistorialNotificacionesJefaBinding
import com.example.movilmanupuladora.databinding.ItemNotificacionJefaBinding

class HistorialNotificacionesJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorialNotificacionesJefaBinding
    private var filtroSeleccionado: String = "Todas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHistorialNotificacionesJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        binding.btnVolver.setOnClickListener {
            finish()
        }

        configurarFiltros()
        cargarNotificaciones()

        lifecycleScope.launch {
            NotificacionesJefaManager.sincronizarNotificacionesDelBackend(this@HistorialNotificacionesJefaActivity)
            cargarNotificaciones()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarNotificaciones()
    }

    private fun cargarNotificaciones() {
        val notifsSede = NotificacionesJefaManager.obtenerNotificacionesSede(this)
        binding.btnFiltroTodas.text = "Todas (${notifsSede.size})"

        val filtradas = when (filtroSeleccionado) {
            "Equipo" -> notifsSede.filter { it.categoria.equals("Equipo", ignoreCase = true) }
            "Inventario" -> notifsSede.filter { it.categoria.equals("Inventario", ignoreCase = true) }
            "Preparaciones" -> notifsSede.filter { it.categoria.contains("Prep", ignoreCase = true) }
            "Asistencia" -> notifsSede.filter { it.categoria.equals("Asistencia", ignoreCase = true) }
            else -> notifsSede
        }

        binding.contenedorHistorialNotificaciones.removeAllViews()

        if (filtradas.isEmpty()) {
            binding.contenedorHistorialNotificaciones.visibility = View.GONE
            binding.layoutSinNotificaciones.visibility = View.VISIBLE
            binding.txtMensajeVacio.text = if (filtroSeleccionado == "Todas") {
                "No tienes notificaciones pendientes para tu sede."
            } else {
                "No hay notificaciones de $filtroSeleccionado para tu sede."
            }
        } else {
            binding.contenedorHistorialNotificaciones.visibility = View.VISIBLE
            binding.layoutSinNotificaciones.visibility = View.GONE

            val inflater = LayoutInflater.from(this)
            for (notif in filtradas) {
                val itemBinding = ItemNotificacionJefaBinding.inflate(inflater, binding.contenedorHistorialNotificaciones, false)

                itemBinding.txtTituloNotif.text = notif.titulo
                itemBinding.txtMensajeNotif.text = notif.mensaje
                itemBinding.txtFechaNotif.text = notif.fecha
                itemBinding.badgeCategoriaNotif.text = notif.categoria

                val esLeida = NotificacionesJefaManager.esLeida(this, notif.id)
                val density = resources.displayMetrics.density

                if (esLeida) {
                    itemBinding.cardItemNotificacion.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                    itemBinding.cardItemNotificacion.strokeColor = Color.parseColor("#E2E8F0")
                    itemBinding.cardItemNotificacion.strokeWidth = (1 * density).toInt()
                    itemBinding.viewDotNoLeida.visibility = View.GONE
                } else {
                    itemBinding.cardItemNotificacion.setCardBackgroundColor(Color.parseColor("#FEFCE8"))
                    itemBinding.cardItemNotificacion.strokeColor = Color.parseColor("#FDE68A")
                    itemBinding.cardItemNotificacion.strokeWidth = (1.2f * density).toInt()
                    itemBinding.viewDotNoLeida.visibility = View.VISIBLE
                }

                val textoCompleto = "${notif.titulo} ${notif.mensaje}"
                val esEntrega = textoCompleto.contains("entrega", ignoreCase = true) ||
                        textoCompleto.contains("remisi", ignoreCase = true) ||
                        textoCompleto.contains("despach", ignoreCase = true) ||
                        textoCompleto.contains("factura", ignoreCase = true) ||
                        textoCompleto.contains("FAC-", ignoreCase = true) ||
                        textoCompleto.contains("#REM", ignoreCase = true)

                if (esEntrega) {
                    itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_nav_inventory)
                    itemBinding.imgIconoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    itemBinding.imgIconoNotif.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
                } else {
                    when (notif.categoria.lowercase()) {
                        "equipo" -> {
                            itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_people)
                            itemBinding.imgIconoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E7FF"))
                            itemBinding.imgIconoNotif.imageTintList = ColorStateList.valueOf(Color.parseColor("#4338CA"))
                        }
                        "asistencia" -> {
                            itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_check_circle)
                            itemBinding.imgIconoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                            itemBinding.imgIconoNotif.imageTintList = ColorStateList.valueOf(Color.parseColor("#166534"))
                        }
                        "preparaciones", "preparacion" -> {
                            itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_nav_preparations)
                            itemBinding.imgIconoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                            itemBinding.imgIconoNotif.imageTintList = ColorStateList.valueOf(Color.parseColor("#854D0E"))
                        }
                        else -> {
                            itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_nav_inventory)
                            itemBinding.imgIconoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                            itemBinding.imgIconoNotif.imageTintList = ColorStateList.valueOf(Color.parseColor("#166534"))
                        }
                    }
                }

                itemBinding.cardItemNotificacion.setOnClickListener {
                    NotificacionesJefaManager.marcarLeida(this, notif.id)
                    itemBinding.cardItemNotificacion.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                    itemBinding.cardItemNotificacion.strokeColor = Color.parseColor("#E2E8F0")
                    itemBinding.viewDotNoLeida.visibility = View.GONE

                    if (esEntrega) {
                        val intent = Intent(this, RegistrarEntregaActivity::class.java)
                        val match = notif.numeroRemision
                            ?: Regex("(?:#REM-|FAC-)[0-9A-Za-z-]+", RegexOption.IGNORE_CASE).find(textoCompleto)?.value
                        if (match != null) {
                            intent.putExtra("numero_remision", match)
                        }
                        val matchSupervisor = Regex("El\\s+([A-Za-zÁÉÍÓÚáéíóúñÑ]+(?:\\s+[A-Za-zÁÉÍÓÚáéíóúñÑ]+)?)\\s+ha\\s+despachado", RegexOption.IGNORE_CASE).find(textoCompleto)
                            ?: Regex("Responsable:\\s*([^|;\\n]+)", RegexOption.IGNORE_CASE).find(textoCompleto)
                        matchSupervisor?.groupValues?.get(1)?.trim()?.let { sup ->
                            if (!sup.equals("supervisor", ignoreCase = true) && !sup.equals("supervisor pae", ignoreCase = true)) {
                                intent.putExtra("supervisor_nombre", sup)
                            }
                        }
                        startActivity(intent)
                    } else {
                        when (notif.categoria.lowercase()) {
                            "equipo" -> startActivity(Intent(this, ManipuladorasJefaActivity::class.java))
                            "asistencia" -> startActivity(Intent(this, AsistenciaJefaActivity::class.java))
                            "preparaciones", "preparacion" -> startActivity(Intent(this, HistorialPreparacionesActivity::class.java))
                            else -> startActivity(Intent(this, InventarioJefaActivity::class.java))
                        }
                    }
                }

                binding.contenedorHistorialNotificaciones.addView(itemBinding.root)
            }
        }
    }

    private fun configurarFiltros() {
        fun resetBoton(btn: com.google.android.material.button.MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            btn.strokeColor = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
            btn.strokeWidth = (1 * resources.displayMetrics.density).toInt()
        }

        fun seleccionarBoton(btn: com.google.android.material.button.MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
            btn.strokeColor = ColorStateList.valueOf(Color.parseColor("#F4B41F"))
            btn.strokeWidth = (1.5f * resources.displayMetrics.density).toInt()
        }

        val botones = listOf(
            Pair(binding.btnFiltroTodas, "Todas"),
            Pair(binding.btnFiltroEquipo, "Equipo"),
            Pair(binding.btnFiltroInventario, "Inventario"),
            Pair(binding.btnFiltroPreparaciones, "Preparaciones"),
            Pair(binding.btnFiltroAsistencia, "Asistencia")
        )

        botones.forEach { (btn, categoria) ->
            btn.setOnClickListener {
                botones.forEach { resetBoton(it.first) }
                seleccionarBoton(btn)
                filtroSeleccionado = categoria
                cargarNotificaciones()
            }
        }
    }
}

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

        binding.contenedorHistorialNotificaciones.removeAllViews()

        if (notifsSede.isEmpty()) {
            binding.contenedorHistorialNotificaciones.visibility = View.GONE
            binding.layoutSinNotificaciones.visibility = View.VISIBLE
            binding.txtMensajeVacio.text = "No tienes notificaciones pendientes para tu sede."
        } else {
            binding.contenedorHistorialNotificaciones.visibility = View.VISIBLE
            binding.layoutSinNotificaciones.visibility = View.GONE

            val inflater = LayoutInflater.from(this)
            for (notif in notifsSede) {
                val itemBinding = ItemNotificacionJefaBinding.inflate(inflater, binding.contenedorHistorialNotificaciones, false)

                val tituloLimpio = limpiarEmojis(notif.titulo)
                val mensajeLimpio = limpiarEmojis(notif.mensaje)
                itemBinding.txtTituloNotif.text = tituloLimpio
                itemBinding.txtMensajeNotif.text = mensajeLimpio
                itemBinding.txtFechaNotif.text = notif.fecha
                itemBinding.badgeCategoriaNotif.text = notif.categoria

                val entregaYaRecibida = !notif.numeroRemision.isNullOrBlank() &&
                        getSharedPreferences("ENTREGAS_RECIBIDAS_PREFS", MODE_PRIVATE)
                            .getBoolean("recibida_${notif.numeroRemision}", false)
                val esLeida = notif.leida || entregaYaRecibida || NotificacionesJefaManager.esLeida(this, notif.id)

                val textoCompleto = "${notif.titulo} ${notif.mensaje}"
                val esEntrega = textoCompleto.contains("entrega", ignoreCase = true) ||
                        textoCompleto.contains("remisi", ignoreCase = true) ||
                        textoCompleto.contains("despach", ignoreCase = true) ||
                        textoCompleto.contains("factura", ignoreCase = true) ||
                        textoCompleto.contains("FAC", ignoreCase = true) ||
                        textoCompleto.contains("#REM", ignoreCase = true)

                val colorBarra = when {
                    esLeida -> "#F4B41F"
                    esEntrega -> "#F4B41F"
                    notif.categoria.equals("equipo", ignoreCase = true) -> "#3B82F6"
                    notif.categoria.equals("asistencia", ignoreCase = true) -> "#10B981"
                    else -> "#F4B41F"
                }
                itemBinding.viewBarraAcento.setBackgroundColor(Color.parseColor(colorBarra))

                // Pill de estado (Nueva / Leída / Importante / Informativa)
                when {
                    esLeida -> {
                        itemBinding.badgeEstadoNotif.text = "Leída"
                        itemBinding.badgeEstadoNotif.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                        itemBinding.badgeEstadoNotif.setTextColor(Color.parseColor("#166534"))
                    }
                    textoCompleto.contains("bajo", ignoreCase = true) || textoCompleto.contains("alerta", ignoreCase = true) -> {
                        itemBinding.badgeEstadoNotif.text = "Importante"
                        itemBinding.badgeEstadoNotif.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                        itemBinding.badgeEstadoNotif.setTextColor(Color.parseColor("#92400E"))
                    }
                    esEntrega -> {
                        itemBinding.badgeEstadoNotif.text = "Nueva"
                        itemBinding.badgeEstadoNotif.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                        itemBinding.badgeEstadoNotif.setTextColor(Color.parseColor("#92400E"))
                    }
                    else -> {
                        itemBinding.badgeEstadoNotif.text = "Informativa"
                        itemBinding.badgeEstadoNotif.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                        itemBinding.badgeEstadoNotif.setTextColor(Color.parseColor("#475569"))
                    }
                }

                // Detalles desplegables: inician colapsados; la pestañita lateral despliega/contrae Remisión / Productos / Destino
                val remisionCode = notif.numeroRemision
                    ?: Regex("(?:#REM-|FAC-?)[0-9A-Za-z-]+", RegexOption.IGNORE_CASE).find(textoCompleto)?.value
                itemBinding.layoutDetallesEntrega.visibility = View.GONE
                itemBinding.imgFlechaDerecha.visibility = View.VISIBLE
                itemBinding.imgFlechaDerecha.rotation = if (esEntrega && !remisionCode.isNullOrBlank()) 90f else 0f

                if (esEntrega && !remisionCode.isNullOrBlank()) {
                    itemBinding.txtDetalleRemision.text = remisionCode
                    val prodMatch = Regex("(\\d+\\s+alimentos?|\\d+\\s+productos?)", RegexOption.IGNORE_CASE).find(mensajeLimpio)?.value
                    itemBinding.txtDetalleProductos.text = prodMatch ?: "Ver en remisión"
                    val sedeActual = com.example.movilmanupuladora.utils.SessionManager(this).getSedeNombre()
                    itemBinding.txtDetalleDestino.text = if (!sedeActual.isNullOrBlank()) sedeActual else "Sede Central"

                    itemBinding.imgFlechaDerecha.setOnClickListener {
                        val abierto = itemBinding.layoutDetallesEntrega.visibility == View.VISIBLE
                        itemBinding.layoutDetallesEntrega.visibility = if (abierto) View.GONE else View.VISIBLE
                        itemBinding.imgFlechaDerecha.rotation = if (abierto) 90f else 270f
                    }
                } else {
                    itemBinding.imgFlechaDerecha.setOnClickListener(null)
                }

                itemBinding.cardItemNotificacion.setOnClickListener {
                    NotificacionesJefaManager.marcarLeida(this, notif.id)
                    itemBinding.badgeEstadoNotif.text = "Leída"
                    itemBinding.badgeEstadoNotif.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                    itemBinding.badgeEstadoNotif.setTextColor(Color.parseColor("#166534"))

                    // Marcar también como leída en el backend si proviene de allí
                    if (notif.id.startsWith("backend_")) {
                        val idBackend = notif.id.removePrefix("backend_").toIntOrNull()
                        if (idBackend != null) {
                            lifecycleScope.launch {
                                try {
                                    com.example.movilmanupuladora.data.api.RetrofitClient.apiService
                                        .marcarNotificacionLeida(idBackend)
                                } catch (_: Exception) { }
                            }
                        }
                    }

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

    private fun limpiarEmojis(texto: String): String {
        return texto
            .replace(Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+"), "")
            .replace(Regex("[\\u2600-\\u27BF\\uFE0F]"), "")
            .trim()
    }
}

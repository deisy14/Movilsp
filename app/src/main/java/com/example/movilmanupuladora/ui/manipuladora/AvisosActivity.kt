package com.example.movilmanupuladora.ui.manipuladora

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.Notificacion
import com.example.movilmanupuladora.databinding.ActivityAvisosBinding
import com.example.movilmanupuladora.databinding.ItemNotificacionJefaBinding
import com.example.movilmanupuladora.utils.NavigationHelper
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AvisosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAvisosBinding
    private var todasLasNotificaciones: List<Notificacion> = emptyList()
    private var filtroActual: String = "TODAS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAvisosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, 0)
            insets
        }

        NavigationHelper.setupStickyTopBar(this, binding.root, lifecycleScope)
        configurarBarraNavegacion()
        configurarFiltros()
        configurarMarcarLeidas()
    }

    override fun onResume() {
        super.onResume()
        cargarNotificacionesDesdeBackend()
    }

    private fun configurarBarraNavegacion() {
        NavigationHelper.setupBarraNavegacion(
            this,
            binding.barraNavegacion,
            NavigationHelper.Tab.AVISOS
        )
    }

    private fun configurarMarcarLeidas() {
        binding.btnMarcarLeidas.setOnClickListener {
            val noLeidas = todasLasNotificaciones.filter { it.leido != true }
            if (noLeidas.isEmpty()) return@setOnClickListener

            lifecycleScope.launch(Dispatchers.IO) {
                for (n in noLeidas) {
                    val id = n.idNotificacion ?: continue
                    runCatching { RetrofitClient.apiService.marcarNotificacionLeida(id) }
                }
                withContext(Dispatchers.Main) {
                    cargarNotificacionesDesdeBackend()
                }
            }
        }
    }

    private fun configurarFiltros() {
        fun actualizarEstilosBotones(activo: MaterialButton) {
            val botones = listOf(
                binding.btnFiltroTodas,
                binding.btnFiltroInventario,
                binding.btnFiltroPreparaciones,
                binding.btnFiltroAsistencia
            )
            val strokePx = (1 * resources.displayMetrics.density).toInt()
            for (btn in botones) {
                if (btn == activo) {
                    btn.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2B5424"))
                    btn.setTextColor(Color.WHITE)
                    btn.strokeColor = ColorStateList.valueOf(Color.parseColor("#2B5424"))
                    btn.strokeWidth = strokePx
                } else {
                    btn.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
                    btn.setTextColor(Color.parseColor("#2B5424"))
                    btn.strokeColor = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                    btn.strokeWidth = strokePx
                }
            }
        }

        binding.btnFiltroTodas.setOnClickListener {
            filtroActual = "TODAS"
            actualizarEstilosBotones(binding.btnFiltroTodas)
            renderizarLista()
        }
        binding.btnFiltroInventario.setOnClickListener {
            filtroActual = "INVENTARIO"
            actualizarEstilosBotones(binding.btnFiltroInventario)
            renderizarLista()
        }
        binding.btnFiltroPreparaciones.setOnClickListener {
            filtroActual = "PREPARACIONES"
            actualizarEstilosBotones(binding.btnFiltroPreparaciones)
            renderizarLista()
        }
        binding.btnFiltroAsistencia.setOnClickListener {
            filtroActual = "ASISTENCIA"
            actualizarEstilosBotones(binding.btnFiltroAsistencia)
            renderizarLista()
        }
    }

    private fun cargarNotificacionesDesdeBackend() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SessionManager.getToken(this@AvisosActivity)
                val response = RetrofitClient.apiService.obtenerNotificaciones()
                val lista = if (response.isSuccessful && response.body() != null) {
                    response.body()!!.sortedByDescending { it.idNotificacion ?: 0 }
                } else {
                    emptyList()
                }
                withContext(Dispatchers.Main) {
                    todasLasNotificaciones = lista
                    actualizarResumen()
                    renderizarLista()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    actualizarResumen()
                    renderizarLista()
                }
            }
        }
    }

    private fun actualizarResumen() {
        val total = todasLasNotificaciones.size
        val sinLeer = todasLasNotificaciones.count { it.leido != true }
        val leidas = total - sinLeer
        val sede = SessionManager(this).getSedeNombre()?.takeIf { it.isNotBlank() } ?: "tu sede"

        binding.txtTotalAvisos.text = total.toString()
        binding.txtSinLeerAvisos.text = sinLeer.toString()
        binding.txtLeidasAvisos.text = leidas.toString()
        binding.txtResumenAvisos.text = "$total avisos registrados en $sede"
        binding.btnFiltroTodas.text = "Todas  $total"
    }

    private fun inferirCategoria(n: Notificacion): String {
        val txt = "${n.titulo.orEmpty()} ${n.mensaje.orEmpty()} ${n.tipo.orEmpty()}".lowercase()
        return when {
            txt.contains("inventario") || txt.contains("stock") || txt.contains("insumo") || txt.contains("entrega") -> "Inventario"
            txt.contains("asistencia") || txt.contains("racion") || txt.contains("estudiante") -> "Asistencia"
            txt.contains("menu") || txt.contains("menú") || txt.contains("plato") || txt.contains("prepar") || txt.contains("componente") -> "Preparaciones"
            else -> "General"
        }
    }

    private fun renderizarLista() {
        val filtradas = when (filtroActual) {
            "INVENTARIO" -> todasLasNotificaciones.filter { inferirCategoria(it) == "Inventario" }
            "PREPARACIONES" -> todasLasNotificaciones.filter { inferirCategoria(it) == "Preparaciones" }
            "ASISTENCIA" -> todasLasNotificaciones.filter { inferirCategoria(it) == "Asistencia" }
            else -> todasLasNotificaciones
        }

        binding.contenedorNotificacionesReal.removeAllViews()

        if (filtradas.isEmpty()) {
            binding.contenedorNotificacionesReal.visibility = View.GONE
            binding.layoutSinAvisos.visibility = View.VISIBLE
            return
        }

        binding.contenedorNotificacionesReal.visibility = View.VISIBLE
        binding.layoutSinAvisos.visibility = View.GONE

        val inflater = LayoutInflater.from(this)
        val density = resources.displayMetrics.density

        for (notif in filtradas) {
            val itemBinding = ItemNotificacionJefaBinding.inflate(
                inflater,
                binding.contenedorNotificacionesReal,
                false
            )
            val cat = inferirCategoria(notif)
            itemBinding.txtTituloNotif.text = notif.titulo.orEmpty().ifBlank { "Aviso del sistema" }
            itemBinding.txtMensajeNotif.text = notif.mensaje.orEmpty()
            itemBinding.txtFechaNotif.text = notif.fecha.orEmpty().take(10)
            itemBinding.badgeCategoriaNotif.text = cat
            itemBinding.imgFlechaDerecha.visibility = View.GONE
            itemBinding.layoutDetallesEntrega.visibility = View.GONE

            val estaLeida = notif.leido == true
            if (estaLeida) {
                itemBinding.cardItemNotificacion.setCardBackgroundColor(Color.WHITE)
                itemBinding.cardItemNotificacion.strokeColor = Color.parseColor("#E2E8F0")
                itemBinding.cardItemNotificacion.strokeWidth = (1 * density).toInt()
                itemBinding.viewBarraAcento.setBackgroundColor(Color.parseColor("#94A3B8"))
                itemBinding.badgeEstadoNotif.text = "Leída"
                itemBinding.badgeEstadoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                itemBinding.badgeEstadoNotif.setTextColor(Color.parseColor("#475569"))
            } else {
                itemBinding.cardItemNotificacion.setCardBackgroundColor(Color.parseColor("#F3F6F0"))
                itemBinding.cardItemNotificacion.strokeColor = Color.parseColor("#B9CCB2")
                itemBinding.cardItemNotificacion.strokeWidth = (1.5f * density).toInt()
                itemBinding.viewBarraAcento.setBackgroundColor(Color.parseColor("#2B5424"))
                itemBinding.badgeEstadoNotif.text = "Nueva"
                itemBinding.badgeEstadoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E6EDE2"))
                itemBinding.badgeEstadoNotif.setTextColor(Color.parseColor("#2B5424"))
            }

            itemBinding.cardItemNotificacion.setOnClickListener {
                val id = notif.idNotificacion
                if (!estaLeida && id != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        runCatching { RetrofitClient.apiService.marcarNotificacionLeida(id) }
                        withContext(Dispatchers.Main) {
                            cargarNotificacionesDesdeBackend()
                        }
                    }
                }
            }

            binding.contenedorNotificacionesReal.addView(itemBinding.root)
        }
    }
}

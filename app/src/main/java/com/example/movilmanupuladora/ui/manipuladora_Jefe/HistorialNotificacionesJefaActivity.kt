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

                when (notif.categoria.lowercase()) {
                    "equipo" -> {
                        itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_people)
                        itemBinding.imgIconoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                        itemBinding.badgeCategoriaNotif.background = getDrawable(R.drawable.bg_pill_yellow)
                        itemBinding.badgeCategoriaNotif.setTextColor(Color.parseColor("#854D0E"))
                        itemBinding.cardItemNotificacion.setOnClickListener {
                            startActivity(Intent(this, ManipuladorasJefaActivity::class.java))
                        }
                    }
                    "inventario" -> {
                        itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_nav_inventory)
                        itemBinding.imgIconoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                        itemBinding.badgeCategoriaNotif.background = getDrawable(R.drawable.bg_pill_green)
                        itemBinding.badgeCategoriaNotif.setTextColor(Color.parseColor("#166534"))
                        itemBinding.cardItemNotificacion.setOnClickListener {
                            val textoCompleto = "${notif.titulo} ${notif.mensaje}"
                            if (textoCompleto.contains("entrega", ignoreCase = true) ||
                                textoCompleto.contains("remisi", ignoreCase = true) ||
                                textoCompleto.contains("despach", ignoreCase = true)) {
                                val intent = Intent(this, RegistrarEntregaActivity::class.java)
                                val match = Regex("#REM-[0-9A-Za-z-]+", RegexOption.IGNORE_CASE).find(textoCompleto)
                                if (match != null) {
                                    intent.putExtra("numero_remision", match.value)
                                }
                                startActivity(intent)
                            } else {
                                startActivity(Intent(this, InventarioJefaActivity::class.java))
                            }
                        }
                    }
                    "preparaciones", "preparacion" -> {
                        itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_nav_preparations)
                        itemBinding.imgIconoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                        itemBinding.badgeCategoriaNotif.background = getDrawable(R.drawable.bg_pill_yellow)
                        itemBinding.badgeCategoriaNotif.setTextColor(Color.parseColor("#854D0E"))
                        itemBinding.cardItemNotificacion.setOnClickListener {
                            startActivity(Intent(this, HistorialPreparacionesActivity::class.java))
                        }
                    }
                    "asistencia" -> {
                        itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_people)
                        itemBinding.imgIconoNotif.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                        itemBinding.badgeCategoriaNotif.background = getDrawable(R.drawable.bg_pill_green)
                        itemBinding.badgeCategoriaNotif.setTextColor(Color.parseColor("#166534"))
                        itemBinding.cardItemNotificacion.setOnClickListener {
                            startActivity(Intent(this, AsistenciaJefaActivity::class.java))
                        }
                    }
                    else -> {
                        itemBinding.imgIconoNotif.setImageResource(R.drawable.ic_notificaciones)
                        itemBinding.cardItemNotificacion.setOnClickListener(null)
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

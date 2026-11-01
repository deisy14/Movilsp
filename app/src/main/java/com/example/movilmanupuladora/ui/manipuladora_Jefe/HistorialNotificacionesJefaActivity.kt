package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityHistorialNotificacionesJefaBinding

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

        configurarFiltros()
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

        binding.btnFiltroTodas.setOnClickListener {
            resetBoton(binding.btnFiltroTodas)
            resetBoton(binding.btnFiltroInventario)
            resetBoton(binding.btnFiltroPreparaciones)
            resetBoton(binding.btnFiltroAsistencia)
            seleccionarBoton(binding.btnFiltroTodas)

            binding.cardNotif1.visibility = View.VISIBLE
            binding.cardNotif2.visibility = View.VISIBLE
            binding.cardNotif3.visibility = View.VISIBLE
            binding.cardNotif4.visibility = View.VISIBLE
        }

        binding.btnFiltroInventario.setOnClickListener {
            resetBoton(binding.btnFiltroTodas)
            resetBoton(binding.btnFiltroInventario)
            resetBoton(binding.btnFiltroPreparaciones)
            resetBoton(binding.btnFiltroAsistencia)
            seleccionarBoton(binding.btnFiltroInventario)

            binding.cardNotif1.visibility = View.VISIBLE
            binding.cardNotif2.visibility = View.GONE
            binding.cardNotif3.visibility = View.GONE
            binding.cardNotif4.visibility = View.GONE
        }

        binding.btnFiltroPreparaciones.setOnClickListener {
            resetBoton(binding.btnFiltroTodas)
            resetBoton(binding.btnFiltroInventario)
            resetBoton(binding.btnFiltroPreparaciones)
            resetBoton(binding.btnFiltroAsistencia)
            seleccionarBoton(binding.btnFiltroPreparaciones)

            binding.cardNotif1.visibility = View.GONE
            binding.cardNotif2.visibility = View.VISIBLE
            binding.cardNotif3.visibility = View.GONE
            binding.cardNotif4.visibility = View.GONE
        }

        binding.btnFiltroAsistencia.setOnClickListener {
            resetBoton(binding.btnFiltroTodas)
            resetBoton(binding.btnFiltroInventario)
            resetBoton(binding.btnFiltroPreparaciones)
            resetBoton(binding.btnFiltroAsistencia)
            seleccionarBoton(binding.btnFiltroAsistencia)

            binding.cardNotif1.visibility = View.GONE
            binding.cardNotif2.visibility = View.GONE
            binding.cardNotif3.visibility = View.VISIBLE
            binding.cardNotif4.visibility = View.GONE
        }
    }
}
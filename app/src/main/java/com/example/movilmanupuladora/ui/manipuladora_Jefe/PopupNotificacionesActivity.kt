package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.PopupNotificacionesBinding

class PopupNotificacionesActivity : AppCompatActivity() {

    private lateinit var binding: PopupNotificacionesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.setDimAmount(0.15f)

        // Posicionar bien arriba en la esquina superior derecha
        val wlp = window?.attributes
        wlp?.gravity = Gravity.TOP or Gravity.END
        wlp?.x = 16
        wlp?.y = 36
        window?.attributes = wlp

        binding = PopupNotificacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.popupNotificaciones) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }

        binding.txtCantidadNotificaciones.text = "3"

        binding.notificacionPreparacion.setOnClickListener {
            Toast.makeText(this, "Abriendo estado de preparaciones...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HistorialPreparacionesActivity::class.java))
            finish()
        }

        binding.notificacionInventario.setOnClickListener {
            Toast.makeText(this, "Abriendo inventario de insumos...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, InventarioJefaActivity::class.java))
            finish()
        }

        binding.notificacionAsistencia.setOnClickListener {
            Toast.makeText(this, "Abriendo registro de asistencia...", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AsistenciaJefaActivity::class.java))
            finish()
        }

        binding.btnVerTodasNotificaciones.setOnClickListener {
            Toast.makeText(this, "Todas las notificaciones al día", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.os.Bundle
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

        binding = PopupNotificacionesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.popupNotificaciones) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.notificacionPreparacion.setOnClickListener {
            Toast.makeText(this, "Preparación seleccionada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
package com.example.manipuladorajefe

import android.app.AlertDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.manipuladorajefe.databinding.ActivityProgramacionSemanalBinding

class ProgramacionSemanalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgramacionSemanalBinding

    private val platosDisponibles = arrayOf(
        "Pollo guisado",
        "Arroz con pollo",
        "Lentejas con arroz",
        "Pasta con pollo",
        "Pasta con verduras"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityProgramacionSemanalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarEventos()
    }

    private fun configurarEventos() {
        binding.btnVolver.setOnClickListener { finish() }
        binding.btnCancelar.setOnClickListener { finish() }

        binding.btnGuardarProgramacion.setOnClickListener {
            Toast.makeText(this, "Programación semanal guardada correctamente", Toast.LENGTH_LONG).show()
            finish()
        }

        // Lunes
        binding.btnEditarLunesManana.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoLunesManana) }
        binding.btnEditarLunesTarde.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoLunesTarde) }

        // Martes
        binding.btnEditarMartesManana.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoMartesManana) }
        binding.btnEditarMartesTarde.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoMartesTarde) }

        // Miércoles
        binding.btnEditarMiercolesManana.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoMiercolesManana) }
        binding.btnEditarMiercolesTarde.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoMiercolesTarde) }

        // Jueves
        binding.btnEditarJuevesManana.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoJuevesManana) }
        binding.btnEditarJuevesTarde.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoJuevesTarde) }

        // Viernes
        binding.btnEditarViernesManana.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoViernesManana) }
        binding.btnEditarViernesTarde.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoViernesTarde) }

        // Sábado
        binding.btnEditarSabadoManana.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoSabadoManana) }
        binding.btnEditarSabadoTarde.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoSabadoTarde) }

        // Domingo
        binding.btnEditarDomingoManana.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoDomingoManana) }
        binding.btnEditarDomingoTarde.setOnClickListener { mostrarSelectorPlato(binding.txtPlatoDomingoTarde) }
    }

    private fun mostrarSelectorPlato(textView: TextView) {
        AlertDialog.Builder(this)
            .setTitle("Seleccionar plato")
            .setItems(platosDisponibles) { _, which ->
                textView.text = platosDisponibles[which]
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

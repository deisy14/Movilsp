package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.databinding.ActivityResumenDiaBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResumenDiaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResumenDiaBinding
    private var jornadaActual = "MANANA"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityResumenDiaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        configurarFecha()
        configurarEventos()
        configurarNavegacionInferior()
        actualizarDatos()
    }

    private fun configurarFecha() {
        val formato = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES"))
        val fecha = formato.format(Date())
        binding.txtFecha.text = fecha.replaceFirstChar { it.uppercase() }
    }

    private fun configurarEventos() {
        binding.btnCerrar.setOnClickListener { finish() }

        binding.btnManana.setOnClickListener {
            jornadaActual = "MANANA"
            actualizarBotonesJornada()
            actualizarDatos()
        }

        binding.btnTarde.setOnClickListener {
            jornadaActual = "TARDE"
            actualizarBotonesJornada()
            actualizarDatos()
        }

        binding.btnVerHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialPreparacionesActivity::class.java))
        }
    }

    private fun actualizarBotonesJornada() {
        val amarillo = ContextCompat.getColor(this, R.color.amarillo_principal)
        val gris = ContextCompat.getColor(this, R.color.gris_claro)
        val blanco = ContextCompat.getColor(this, R.color.blanco)
        val negro = ContextCompat.getColor(this, R.color.negro_principal)

        if (jornadaActual == "MANANA") {
            binding.btnManana.setBackgroundColor(amarillo)
            binding.btnManana.setTextColor(blanco)
            binding.btnTarde.setBackgroundColor(gris)
            binding.btnTarde.setTextColor(negro)
        } else {
            binding.btnTarde.setBackgroundColor(amarillo)
            binding.btnTarde.setTextColor(blanco)
            binding.btnManana.setBackgroundColor(gris)
            binding.btnManana.setTextColor(negro)
        }
    }

    private fun actualizarDatos() {
        // Asistencia
        val ninos = if (jornadaActual == "MANANA") {
            AsistenciaManager.obtenerManana(this)
        } else {
            AsistenciaManager.obtenerTarde(this)
        }
        binding.txtNinos.text = "$ninos niños"

        // Preparaciones
        val preparaciones = PreparacionesManager.obtenerPreparaciones().filter {
            if (jornadaActual == "MANANA") it.jornada.equals("Mañana", true)
            else it.jornada.equals("Tarde", true)
        }

        binding.txtCantidadPreparaciones.text = preparaciones.size.toString()
        if (preparaciones.isNotEmpty()) {
            binding.txtMenu.text = preparaciones.first().menu
            binding.txtEstado.text = preparaciones.first().estado
            binding.txtPreparacionesDetalle.text = preparaciones.joinToString("\n") { "• ${it.menu} (${it.estado})" }
        } else {
            binding.txtMenu.text = "Sin menú programado"
            binding.txtEstado.text = "Pendiente"
            binding.txtPreparacionesDetalle.text = "No hay preparaciones registradas para esta jornada."
        }

        // Manipuladoras
        val manipuladoras = ManipuladorasManager.obtenerManipuladoras()
        binding.txtCantidadManipuladoras.text = "${manipuladoras.size} manipuladoras asignadas"
        binding.txtListaManipuladoras.text = manipuladoras.joinToString("\n") { "• ${it.nombre} (${it.funcion})" }

        // Inventario
        val inventario = InventarioManager.obtenerInventario()
        if (inventario.isNotEmpty()) {
            binding.txtInventarioUtilizado.text = inventario.entries.joinToString("\n") { "• ${it.key}: ${it.value} kg" }
            val total = inventario.values.sum()
            binding.txtTotalInventarioUtilizado.text = String.format("Total disponible: %.1f kg", total)
        } else {
            binding.txtInventarioUtilizado.text = "No hay alimentos registrados en el inventario."
            binding.txtTotalInventarioUtilizado.text = "Total disponible: 0 kg"
        }
    }

    override fun onResume() {
        super.onResume()
        actualizarDatos()
    }
}

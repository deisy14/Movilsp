package com.example.manipuladorajefe

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.manipuladorajefe.databinding.ActivityDetallePreparacionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetallePreparacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetallePreparacionBinding

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding =
            ActivityDetallePreparacionBinding.inflate(layoutInflater)

        setContentView(binding.root)

        configurarInsets()

        configurarFecha()

        cargarDatos()

        configurarBotones()
    }


    // =========================================================
    // INSETS
    // =========================================================

    private fun configurarInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }


    // =========================================================
    // FECHA
    // =========================================================

    private fun configurarFecha() {
        val fechaIntent = intent.getStringExtra("fecha")
        if (fechaIntent.isNullOrEmpty()) {
            val formato = SimpleDateFormat(
                "EEEE, dd 'de' MMMM",
                Locale("es", "ES")
            )
            val fecha = formato.format(Date())
            binding.txtFecha.text =
                fecha.replaceFirstChar {
                    it.uppercase()
                }
        } else {
            binding.txtFecha.text = fechaIntent
        }
    }


    // =========================================================
    // CARGAR DATOS
    // =========================================================

    private fun cargarDatos() {
        val platoIntent = intent.getStringExtra("plato") ?: "Arroz con pollo"
        val jornadaIntent = intent.getStringExtra("jornada") ?: "Mañana"
        val ninosIntent = intent.getStringExtra("ninos") ?: "35 niños"

        // -----------------------------------------------------
        // ESTADO
        // -----------------------------------------------------

        binding.txtEstado.text =
            "Programada / En curso"


        // -----------------------------------------------------
        // MENÚ
        // -----------------------------------------------------

        binding.txtMenu.text =
            platoIntent


        // -----------------------------------------------------
        // JORNADA
        // -----------------------------------------------------

        binding.txtJornada.text =
            jornadaIntent


        // -----------------------------------------------------
        // NIÑOS
        // -----------------------------------------------------

        binding.txtNinos.text =
            ninosIntent


        // -----------------------------------------------------
        // MANIPULADORAS
        // -----------------------------------------------------

        binding.txtManipuladoras.text =
            """
            Ana López
            María Rodríguez
            """.trimIndent()


        // -----------------------------------------------------
        // ALIMENTOS
        // -----------------------------------------------------

        binding.txtAlimentos.text =
            """
            Arroz       12 kg
            Pollo       18 kg
            Verduras     8 kg
            """.trimIndent()


        // -----------------------------------------------------
        // HORARIOS
        // -----------------------------------------------------

        binding.txtHoraInicio.text =
            "10:15 a. m."

        binding.txtHoraFin.text =
            "11:05 a. m."

        binding.txtDuracion.text =
            "50 minutos"


        // -----------------------------------------------------
        // OBSERVACIONES
        // -----------------------------------------------------

        binding.txtObservaciones.text =
            "Sin observaciones"
    }


    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        // -----------------------------------------------------
        // BOTÓN SUPERIOR
        // -----------------------------------------------------

        binding.btnCerrar.setOnClickListener {

            finish()
        }


        // -----------------------------------------------------
        // BOTÓN INFERIOR
        // -----------------------------------------------------

        binding.btnVolver.setOnClickListener {

            finish()
        }
    }
}
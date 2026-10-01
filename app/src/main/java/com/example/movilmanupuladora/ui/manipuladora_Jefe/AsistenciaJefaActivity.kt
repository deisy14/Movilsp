package com.example.manipuladorajefe

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.manipuladorajefe.databinding.ActivityAsistenciaJefaBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsistenciaJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsistenciaJefaBinding

    private var jornadaActual = "MANANA"

    private var asistenciaManana = 0
    private var asistenciaTarde = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAsistenciaJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarFecha()
        cargarAsistenciaGuardada()
        configurarJornadas()
        configurarBotones()
        configurarNavegacionInferior()
        actualizarInterfaz()
    }

    // =========================================================
    // FECHA
    // =========================================================

    private fun configurarFecha() {

        val formato = SimpleDateFormat(
            "EEEE, dd 'de' MMMM",
            Locale("es", "ES")
        )

        val fecha = formato.format(Date())

        binding.txtFechaAsistencia.text = "Hoy es $fecha"
    }

    // =========================================================
    // CARGAR ASISTENCIA
    // =========================================================

    private fun cargarAsistenciaGuardada() {

        asistenciaManana =
            AsistenciaManager.obtenerManana(this)

        asistenciaTarde =
            AsistenciaManager.obtenerTarde(this)
    }

    // =========================================================
    // JORNADAS
    // =========================================================

    private fun configurarJornadas() {

        binding.btnManana.setOnClickListener {

            jornadaActual = "MANANA"

            actualizarInterfaz()
        }

        binding.btnTarde.setOnClickListener {

            jornadaActual = "TARDE"

            actualizarInterfaz()
        }
    }

    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnRegistrarAsistencia.setOnClickListener {
            mostrarDialogoAsistencia()
        }
    }

    // =========================================================
    // ACTUALIZAR INTERFAZ
    // =========================================================

    private fun actualizarInterfaz() {

        actualizarBotonesJornada()
        actualizarDatosAsistencia()
    }

    // =========================================================
    // BOTONES DE JORNADA
    // =========================================================

    private fun actualizarBotonesJornada() {

        val amarillo = ContextCompat.getColor(
            this,
            R.color.amarillo_principal
        )

        val gris = ContextCompat.getColor(
            this,
            R.color.gris_claro
        )

        val blanco = ContextCompat.getColor(
            this,
            R.color.blanco
        )

        val negro = ContextCompat.getColor(
            this,
            R.color.negro_principal
        )

        if (jornadaActual == "MANANA") {

            binding.btnManana.backgroundTintList =
                ColorStateList.valueOf(amarillo)

            binding.btnManana.setTextColor(blanco)

            binding.btnTarde.backgroundTintList =
                ColorStateList.valueOf(gris)

            binding.btnTarde.setTextColor(negro)

        } else {

            binding.btnTarde.backgroundTintList =
                ColorStateList.valueOf(amarillo)

            binding.btnTarde.setTextColor(blanco)

            binding.btnManana.backgroundTintList =
                ColorStateList.valueOf(gris)

            binding.btnManana.setTextColor(negro)
        }
    }

    // =========================================================
    // DATOS DE ASISTENCIA
    // =========================================================

    private fun actualizarDatosAsistencia() {

        val cantidad = obtenerCantidadActual()

        binding.txtJornadaActual.text =
            "Jornada: ${obtenerNombreJornada()}"

        if (cantidad > 0) {

            binding.txtCantidadAsistencia.text =
                "$cantidad niños"

            binding.txtEstadoAsistencia.text =
                "Registrada"

            binding.txtEstadoAsistencia.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.negro_principal
                )
            )

            binding.btnRegistrarAsistencia.text =
                "Actualizar asistencia"

        } else {

            binding.txtCantidadAsistencia.text =
                "Sin registrar"

            binding.txtEstadoAsistencia.text =
                "Pendiente"

            binding.txtEstadoAsistencia.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.rojo_principal
                )
            )

            binding.btnRegistrarAsistencia.text =
                "Registrar asistencia"
        }
    }

    // =========================================================
    // OBTENER CANTIDAD ACTUAL
    // =========================================================

    private fun obtenerCantidadActual(): Int {

        return if (jornadaActual == "MANANA") {
            asistenciaManana
        } else {
            asistenciaTarde
        }
    }

    // =========================================================
    // DIÁLOGO DE ASISTENCIA
    // =========================================================

    private fun mostrarDialogoAsistencia() {

        val vistaDialogo = LayoutInflater.from(this).inflate(
            R.layout.dialog_registrar_asistencia,
            null
        )

        val txtJornada = vistaDialogo.findViewById<TextView>(
            R.id.txtJornadaDialog
        )

        val edtCantidad = vistaDialogo.findViewById<EditText>(
            R.id.edtCantidadAsistencia
        )

        val btnCancelar = vistaDialogo.findViewById<Button>(
            R.id.btnCancelarAsistencia
        )

        val btnGuardar = vistaDialogo.findViewById<Button>(
            R.id.btnGuardarAsistencia
        )

        txtJornada.text =
            "Jornada: ${obtenerNombreJornada()}"

        edtCantidad.inputType =
            InputType.TYPE_CLASS_NUMBER

        edtCantidad.filters =
            arrayOf(
                InputFilter.LengthFilter(3)
            )

        val cantidadActual = obtenerCantidadActual()

        if (cantidadActual > 0) {

            edtCantidad.setText(
                cantidadActual.toString()
            )

            edtCantidad.setSelection(
                edtCantidad.text.length
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setView(vistaDialogo)
            .create()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {

            val texto =
                edtCantidad.text.toString().trim()

            guardarAsistencia(
                texto,
                dialog
            )
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // =========================================================
    // GUARDAR ASISTENCIA
    // =========================================================

    private fun guardarAsistencia(
        texto: String,
        dialog: AlertDialog
    ) {

        if (texto.isEmpty()) {

            Toast.makeText(
                this,
                "Ingresa la cantidad de niños",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val cantidad = texto.toIntOrNull()

        if (cantidad == null) {

            Toast.makeText(
                this,
                "Ingresa una cantidad válida",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (cantidad <= 0) {

            Toast.makeText(
                this,
                "La cantidad debe ser mayor que cero",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (jornadaActual == "MANANA") {

            asistenciaManana = cantidad

            AsistenciaManager.guardarManana(
                this,
                cantidad
            )

        } else {

            asistenciaTarde = cantidad

            AsistenciaManager.guardarTarde(
                this,
                cantidad
            )
        }

        actualizarDatosAsistencia()

        dialog.dismiss()

        Toast.makeText(
            this,
            "Asistencia actualizada: $cantidad niños",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // NOMBRE DE JORNADA
    // =========================================================

    private fun obtenerNombreJornada(): String {

        return if (jornadaActual == "MANANA") {
            "Mañana"
        } else {
            "Tarde"
        }
    }
}
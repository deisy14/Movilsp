package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R

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
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.AsistenciaDiaria
import com.example.movilmanupuladora.data.repository.AsistenciaRepository
import com.example.movilmanupuladora.databinding.ActivityAsistenciaJefaBinding
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AsistenciaJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAsistenciaJefaBinding
    private lateinit var asistenciaRepository: AsistenciaRepository
    private lateinit var sessionManager: SessionManager

    private var jornadaActual = "MANANA"

    private var asistenciaManana = 0
    private var asistenciaTarde = 0

    private var idAsistenciaManana: Int? = null
    private var idAsistenciaTarde: Int? = null
    private var gradoId: Int = 9 // Default a grado existente

    private val fechaHoy: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAsistenciaJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        asistenciaRepository = AsistenciaRepository(RetrofitClient.apiService)
        sessionManager = SessionManager(this)

        configurarFecha()
        cargarAsistenciaGuardada()
        configurarJornadas()
        configurarBotones()
        configurarNavegacionInferior()
        actualizarInterfaz()

        cargarAsistenciaDelBackend()
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
    // CARGAR ASISTENCIA LOCAL
    // =========================================================

    private fun cargarAsistenciaGuardada() {
        asistenciaManana = AsistenciaManager.obtenerManana(this)
        asistenciaTarde = AsistenciaManager.obtenerTarde(this)
    }

    // =========================================================
    // CARGAR ASISTENCIA DEL BACKEND
    // =========================================================

    private fun cargarAsistenciaDelBackend() {
        lifecycleScope.launch {
            try {
                // 1. Obtener lista de grados para asociar el id_grado correcto
                val gradosRes = asistenciaRepository.obtenerGrados()
                if (gradosRes.isSuccessful && !gradosRes.body().isNullOrEmpty()) {
                    gradoId = gradosRes.body()!!.first().idGrado
                }

                // 2. Obtener asistencias registradas
                val res = asistenciaRepository.obtenerAsistencias()
                if (res.isSuccessful && res.body() != null) {
                    val lista = res.body()!!
                    val registrosHoy = lista.filter { it.fecha == fechaHoy }

                    if (registrosHoy.isNotEmpty()) {
                        val registro = registrosHoy.first()
                        if (jornadaActual == "MANANA") {
                            asistenciaManana = registro.ninosPresentes
                            idAsistenciaManana = registro.idAsistencia
                        } else {
                            asistenciaTarde = registro.ninosPresentes
                            idAsistenciaTarde = registro.idAsistencia
                        }
                        actualizarInterfaz()
                    }
                }
            } catch (e: Exception) {
                // Si falla la red, conserva los datos locales cargados
            }
        }
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
        val amarillo = ContextCompat.getColor(this, R.color.amarillo_principal)
        val gris = ContextCompat.getColor(this, R.color.gris_claro)
        val blanco = ContextCompat.getColor(this, R.color.blanco)
        val negro = ContextCompat.getColor(this, R.color.negro_principal)

        if (jornadaActual == "MANANA") {
            binding.btnManana.backgroundTintList = ColorStateList.valueOf(amarillo)
            binding.btnManana.setTextColor(blanco)

            binding.btnTarde.backgroundTintList = ColorStateList.valueOf(gris)
            binding.btnTarde.setTextColor(negro)
        } else {
            binding.btnTarde.backgroundTintList = ColorStateList.valueOf(amarillo)
            binding.btnTarde.setTextColor(blanco)

            binding.btnManana.backgroundTintList = ColorStateList.valueOf(gris)
            binding.btnManana.setTextColor(negro)
        }
    }

    // =========================================================
    // DATOS DE ASISTENCIA
    // =========================================================

    private fun actualizarDatosAsistencia() {
        val cantidad = obtenerCantidadActual()
        binding.txtJornadaActual.text = "Jornada: ${obtenerNombreJornada()}"

        if (cantidad > 0) {
            binding.txtCantidadAsistencia.text = "$cantidad niños"
            binding.txtEstadoAsistencia.text = "Registrada en el servidor"
            binding.txtEstadoAsistencia.setTextColor(
                ContextCompat.getColor(this, R.color.negro_principal)
            )
            binding.btnRegistrarAsistencia.text = "Actualizar asistencia"
        } else {
            binding.txtCantidadAsistencia.text = "Sin registrar"
            binding.txtEstadoAsistencia.text = "Pendiente"
            binding.txtEstadoAsistencia.setTextColor(
                ContextCompat.getColor(this, R.color.rojo_principal)
            )
            binding.btnRegistrarAsistencia.text = "Registrar asistencia"
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

        val txtJornada = vistaDialogo.findViewById<TextView>(R.id.txtJornadaDialog)
        val edtCantidad = vistaDialogo.findViewById<EditText>(R.id.edtCantidadAsistencia)
        val btnCancelar = vistaDialogo.findViewById<Button>(R.id.btnCancelarAsistencia)
        val btnGuardar = vistaDialogo.findViewById<Button>(R.id.btnGuardarAsistencia)

        txtJornada.text = "Jornada: ${obtenerNombreJornada()}"
        edtCantidad.inputType = InputType.TYPE_CLASS_NUMBER
        edtCantidad.filters = arrayOf(InputFilter.LengthFilter(3))

        val cantidadActual = obtenerCantidadActual()
        if (cantidadActual > 0) {
            edtCantidad.setText(cantidadActual.toString())
            edtCantidad.setSelection(edtCantidad.text.length)
        }

        val dialog = AlertDialog.Builder(this)
            .setView(vistaDialogo)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val texto = edtCantidad.text.toString().trim()
            guardarAsistenciaEnBackend(texto, dialog, btnGuardar)
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // =========================================================
    // GUARDAR ASISTENCIA EN EL BACKEND
    // =========================================================

    private fun guardarAsistenciaEnBackend(
        texto: String,
        dialog: AlertDialog,
        btnGuardar: Button
    ) {
        if (texto.isEmpty()) {
            Toast.makeText(this, "Ingresa la cantidad de niños", Toast.LENGTH_SHORT).show()
            return
        }

        val cantidad = texto.toIntOrNull()
        if (cantidad == null || cantidad <= 0) {
            Toast.makeText(this, "La cantidad debe ser mayor que cero", Toast.LENGTH_SHORT).show()
            return
        }

        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        lifecycleScope.launch {
            try {
                val idActual = if (jornadaActual == "MANANA") idAsistenciaManana else idAsistenciaTarde
                val userId = sessionManager.getUserId().takeIf { it > 0 }

                val payload = AsistenciaDiaria(
                    idAsistencia = idActual,
                    idGrado = gradoId,
                    fecha = fechaHoy,
                    ninosPresentes = cantidad,
                    idUsuarioManipuladora = userId
                )

                val response = if (idActual != null) {
                    asistenciaRepository.actualizarAsistencia(idActual, payload)
                } else {
                    asistenciaRepository.registrarAsistencia(payload)
                }

                if (response.isSuccessful && response.body() != null) {
                    val nueva = response.body()!!

                    if (jornadaActual == "MANANA") {
                        asistenciaManana = nueva.ninosPresentes
                        idAsistenciaManana = nueva.idAsistencia
                        AsistenciaManager.guardarManana(this@AsistenciaJefaActivity, cantidad)
                    } else {
                        asistenciaTarde = nueva.ninosPresentes
                        idAsistenciaTarde = nueva.idAsistencia
                        AsistenciaManager.guardarTarde(this@AsistenciaJefaActivity, cantidad)
                    }

                    actualizarDatosAsistencia()
                    dialog.dismiss()

                    Toast.makeText(
                        this@AsistenciaJefaActivity,
                        "¡Asistencia de $cantidad niños guardada en el backend!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar"
                    Toast.makeText(
                        this@AsistenciaJefaActivity,
                        "Error al guardar (${response.code()}): Verifique su conexión",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar"
                Toast.makeText(
                    this@AsistenciaJefaActivity,
                    "Error de red: ${e.localizedMessage ?: "No se pudo conectar"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
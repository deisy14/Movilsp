package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
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

    // Asistencia separada por nivel (Primaria vs Secundaria)
    private var primariaManana = 0
    private var secundariaManana = 0

    private var primariaTarde = 0
    private var secundariaTarde = 0

    private var idAsistenciaManana: Int? = null
    private var idAsistenciaTarde: Int? = null
    private var gradoId: Int = 9

    // Constantes de gramaje PAE (en kilogramos por estudiante)
    private val GRAMAJE_KG_PRIMARIA = 0.25 // 250g
    private val GRAMAJE_KG_SECUNDARIA = 0.38 // 380g

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
        val totalManana = AsistenciaManager.obtenerManana(this)
        val totalTarde = AsistenciaManager.obtenerTarde(this)

        // Asignación estimada si viene de guardado simple previa
        if (totalManana > 0) {
            primariaManana = (totalManana * 0.6).toInt()
            secundariaManana = totalManana - primariaManana
        }
        if (totalTarde > 0) {
            primariaTarde = (totalTarde * 0.6).toInt()
            secundariaTarde = totalTarde - primariaTarde
        }
    }

    // =========================================================
    // CARGAR ASISTENCIA DEL BACKEND
    // =========================================================

    private fun cargarAsistenciaDelBackend() {
        lifecycleScope.launch {
            try {
                val gradosRes = asistenciaRepository.obtenerGrados()
                if (gradosRes.isSuccessful && !gradosRes.body().isNullOrEmpty()) {
                    gradoId = gradosRes.body()!!.first().idGrado
                }

                val res = asistenciaRepository.obtenerAsistencias()
                if (res.isSuccessful && res.body() != null) {
                    val lista = res.body()!!
                    val registrosHoy = lista.filter { it.fecha == fechaHoy }

                    if (registrosHoy.isNotEmpty()) {
                        val registro = registrosHoy.first()
                        if (jornadaActual == "MANANA") {
                            val total = registro.ninosPresentes
                            primariaManana = (total * 0.6).toInt()
                            secundariaManana = total - primariaManana
                            idAsistenciaManana = registro.idAsistencia
                        } else {
                            val total = registro.ninosPresentes
                            primariaTarde = (total * 0.6).toInt()
                            secundariaTarde = total - primariaTarde
                            idAsistenciaTarde = registro.idAsistencia
                        }
                        actualizarInterfaz()
                    }
                }
            } catch (e: Exception) {
                // Silencioso en error de red
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
    // ACTUALIZAR INTERFAZ Y GRAMAJE
    // =========================================================

    private fun actualizarInterfaz() {
        actualizarBotonesJornada()
        actualizarDatosAsistencia()
    }

    private fun actualizarBotonesJornada() {
        val amarillo = ContextCompat.getColor(this, R.color.amarillo_principal)
        val blanco = ContextCompat.getColor(this, R.color.blanco)
        val negro = ContextCompat.getColor(this, R.color.negro_principal)

        if (jornadaActual == "MANANA") {
            binding.btnManana.backgroundTintList = android.content.res.ColorStateList.valueOf(amarillo)
            binding.btnManana.setTextColor(negro)

            binding.btnTarde.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris_claro))
            binding.btnTarde.setTextColor(negro)
        } else {
            binding.btnTarde.backgroundTintList = android.content.res.ColorStateList.valueOf(amarillo)
            binding.btnTarde.setTextColor(negro)

            binding.btnManana.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris_claro))
            binding.btnManana.setTextColor(negro)
        }
    }

    private fun actualizarDatosAsistencia() {
        val (prim, sec) = obtenerAsistenciaActualPorGrado()
        val totalEstudiantes = prim + sec
        val gramajeTotalKg = (prim * GRAMAJE_KG_PRIMARIA) + (sec * GRAMAJE_KG_SECUNDARIA)

        binding.txtJornadaActual.text = "Jornada: ${obtenerNombreJornada()}"
        binding.txtDesglosePrimariaSecundaria.text = "Primaria (1°-5°): $prim | Secundaria (6°-11°): $sec"
        binding.txtGramajeTotalCalculado.text = String.format(Locale.US, "Requerimiento Total Gramaje: %.1f kg PAE", gramajeTotalKg)

        if (totalEstudiantes > 0) {
            binding.txtCantidadAsistencia.text = "$totalEstudiantes estudiantes"
            binding.txtEstadoAsistencia.text = "Asistencia Registrada"
            binding.txtEstadoAsistencia.setTextColor(ContextCompat.getColor(this, R.color.verde_prinpipal))
            binding.btnRegistrarAsistencia.text = "Actualizar asistencia por grados"
        } else {
            binding.txtCantidadAsistencia.text = "Sin registrar"
            binding.txtEstadoAsistencia.text = "Pendiente"
            binding.txtEstadoAsistencia.setTextColor(ContextCompat.getColor(this, R.color.rojo_principal))
            binding.btnRegistrarAsistencia.text = "Registrar asistencia por grados"
        }
    }

    private fun obtenerAsistenciaActualPorGrado(): Pair<Int, Int> {
        return if (jornadaActual == "MANANA") {
            Pair(primariaManana, secundariaManana)
        } else {
            Pair(primariaTarde, secundariaTarde)
        }
    }

    // =========================================================
    // DIÁLOGO DE REGISTRO DE ASISTENCIA POR GRADOS
    // =========================================================

    private fun mostrarDialogoAsistencia() {
        val vistaDialogo = LayoutInflater.from(this).inflate(
            R.layout.dialog_registrar_asistencia,
            null
        )

        val txtJornada = vistaDialogo.findViewById<TextView>(R.id.txtJornadaDialog)
        val edtPrimaria = vistaDialogo.findViewById<EditText>(R.id.edtCantidadPrimaria)
        val edtSecundaria = vistaDialogo.findViewById<EditText>(R.id.edtCantidadSecundaria)
        val txtTotalCalc = vistaDialogo.findViewById<TextView>(R.id.txtTotalEstudiantesCalculado)
        val txtGramajeCalc = vistaDialogo.findViewById<TextView>(R.id.txtGramajeTotalEstimado)

        val btnCancelar = vistaDialogo.findViewById<Button>(R.id.btnCancelarAsistencia)
        val btnGuardar = vistaDialogo.findViewById<Button>(R.id.btnGuardarAsistencia)

        txtJornada.text = "Jornada: ${obtenerNombreJornada()}"

        val (primActual, secActual) = obtenerAsistenciaActualPorGrado()
        if (primActual > 0) edtPrimaria.setText(primActual.toString())
        if (secActual > 0) edtSecundaria.setText(secActual.toString())

        fun recalcularCalculosModal() {
            val p = edtPrimaria.text.toString().toIntOrNull() ?: 0
            val s = edtSecundaria.text.toString().toIntOrNull() ?: 0
            val total = p + s
            val gramajeKg = (p * GRAMAJE_KG_PRIMARIA) + (s * GRAMAJE_KG_SECUNDARIA)

            txtTotalCalc.text = "Total Estudiantes: $total"
            txtGramajeCalc.text = String.format(Locale.US, "Gramaje total estimado: %.1f kg a cocinar", gramajeKg)
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recalcularCalculosModal()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        edtPrimaria.addTextChangedListener(watcher)
        edtSecundaria.addTextChangedListener(watcher)
        recalcularCalculosModal()

        val dialog = AlertDialog.Builder(this)
            .setView(vistaDialogo)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            val p = edtPrimaria.text.toString().trim().toIntOrNull() ?: 0
            val s = edtSecundaria.text.toString().trim().toIntOrNull() ?: 0
            val total = p + s

            if (total <= 0) {
                Toast.makeText(this, "Ingresa al menos un estudiante en Primaria o Secundaria", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            guardarAsistenciaEnBackend(p, s, total, dialog, btnGuardar)
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
        primaria: Int,
        secundaria: Int,
        total: Int,
        dialog: AlertDialog,
        btnGuardar: Button
    ) {
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
                    ninosPresentes = total,
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
                        primariaManana = primaria
                        secundariaManana = secundaria
                        idAsistenciaManana = nueva.idAsistencia
                        AsistenciaManager.guardarManana(this@AsistenciaJefaActivity, total)
                    } else {
                        primariaTarde = primaria
                        secundariaTarde = secundaria
                        idAsistenciaTarde = nueva.idAsistencia
                        AsistenciaManager.guardarTarde(this@AsistenciaJefaActivity, total)
                    }

                    actualizarDatosAsistencia()
                    dialog.dismiss()

                    val gramajeTotal = (primaria * GRAMAJE_KG_PRIMARIA) + (secundaria * GRAMAJE_KG_SECUNDARIA)
                    Toast.makeText(
                        this@AsistenciaJefaActivity,
                        "¡Asistencia de $total estudiantes registrada! Gramaje PAE: ${String.format(Locale.US, "%.1f", gramajeTotal)} kg",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar"
                    Toast.makeText(this@AsistenciaJefaActivity, "Error al guardar en el servidor", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                btnGuardar.isEnabled = true
                btnGuardar.text = "Guardar"
                Toast.makeText(this@AsistenciaJefaActivity, "Error de red al guardar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerNombreJornada(): String {
        return if (jornadaActual == "MANANA") "Mañana" else "Tarde"
    }
}
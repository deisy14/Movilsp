package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.app.AlertDialog
import android.content.res.ColorStateList
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

    // IDs de asistencia para update (PUT)
    private var idAsistenciaPrimariaManana: Int? = null
    private var idAsistenciaSecundariaManana: Int? = null
    private var idAsistenciaPrimariaTarde: Int? = null
    private var idAsistenciaSecundariaTarde: Int? = null

    // IDs de grado en el backend
    private var idGradoPrimaria: Int = 16
    private var idGradoSecundaria: Int = 17

    // Gramajes PAE oficiales (kg por ración)
    private val GRAMAJE_KG_JARDIN = 0.15
    private val GRAMAJE_KG_PRIMARIA = 0.25
    private val GRAMAJE_KG_SECUNDARIA = 0.38

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

    private fun configurarFecha() {
        val formato = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES"))
        val fecha = formato.format(Date())
        binding.txtFechaAsistencia.text = "Hoy es $fecha"
    }

    private fun cargarAsistenciaGuardada() {
        val p = AsistenciaManager.obtenerPrimaria(this)
        val s = AsistenciaManager.obtenerSecundaria(this)
        if (p > 0 || s > 0) {
            primariaManana = p
            secundariaManana = s
        } else {
            val totalManana = AsistenciaManager.obtenerManana(this)
            if (totalManana > 0) {
                primariaManana = (totalManana * 0.6).toInt()
                secundariaManana = totalManana - primariaManana
            }
        }
    }

    private fun cargarAsistenciaDelBackend() {
        lifecycleScope.launch {
            try {
                // 1. Identificar IDs de grados para Primaria y Secundaria
                val gradosRes = asistenciaRepository.obtenerGrados()
                if (gradosRes.isSuccessful && !gradosRes.body().isNullOrEmpty()) {
                    val grados = gradosRes.body()!!
                    idGradoPrimaria = grados.find { it.nombreGrado.contains("primaria", ignoreCase = true) }?.idGrado ?: 16
                    idGradoSecundaria = grados.find { it.nombreGrado.contains("secundaria", ignoreCase = true) }?.idGrado ?: 17
                }

                // 2. Obtener asistencias de hoy del servidor
                val res = asistenciaRepository.obtenerAsistencias()
                if (res.isSuccessful && res.body() != null) {
                    val lista = res.body()!!
                    val registrosHoy = lista.filter { it.fecha == fechaHoy }

                    val regPrimaria = registrosHoy.find { it.idGrado == idGradoPrimaria }
                    val regSecundaria = registrosHoy.find { it.idGrado == idGradoSecundaria }

                    if (regPrimaria != null) {
                        primariaManana = regPrimaria.ninosPresentes
                        idAsistenciaPrimariaManana = regPrimaria.idAsistencia
                    }
                    if (regSecundaria != null) {
                        secundariaManana = regSecundaria.ninosPresentes
                        idAsistenciaSecundariaManana = regSecundaria.idAsistencia
                    }

                    // Respaldo de registro legacy (por si existía un único registro sin grado asignado)
                    if (regPrimaria == null && regSecundaria == null && registrosHoy.isNotEmpty()) {
                        val regLegacy = registrosHoy.first()
                        val total = regLegacy.ninosPresentes
                        primariaManana = (total * 0.6).toInt()
                        secundariaManana = total - primariaManana
                    }

                    // Actualizar cache local
                    AsistenciaManager.guardarPrimaria(this@AsistenciaJefaActivity, primariaManana)
                    AsistenciaManager.guardarSecundaria(this@AsistenciaJefaActivity, secundariaManana)
                    AsistenciaManager.guardarManana(this@AsistenciaJefaActivity, primariaManana + secundariaManana)

                    actualizarInterfaz()
                }
            } catch (e: Exception) {
                // Si no hay conexión, se mantienen los valores locales cargados
            }
        }
    }

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

    private fun configurarBotones() {
        binding.btnVolver.setOnClickListener {
            finish()
        }

        binding.btnRegistrarAsistencia.setOnClickListener {
            mostrarDialogoAsistencia()
        }
    }

    private fun actualizarInterfaz() {
        actualizarBotonesJornada()
        actualizarDatosAsistencia()
    }

    private fun actualizarBotonesJornada() {
        val amarillo = ContextCompat.getColor(this, R.color.amarillo_principal)
        val gris = ContextCompat.getColor(this, R.color.gris_claro)
        val negro = ContextCompat.getColor(this, R.color.negro_principal)

        if (jornadaActual == "MANANA") {
            binding.btnManana.backgroundTintList = ColorStateList.valueOf(amarillo)
            binding.btnManana.setTextColor(negro)

            binding.btnTarde.backgroundTintList = ColorStateList.valueOf(gris)
            binding.btnTarde.setTextColor(negro)
        } else {
            binding.btnTarde.backgroundTintList = ColorStateList.valueOf(amarillo)
            binding.btnTarde.setTextColor(negro)

            binding.btnManana.backgroundTintList = ColorStateList.valueOf(gris)
            binding.btnManana.setTextColor(negro)
        }
    }

    private fun actualizarDatosAsistencia() {
        val (prim, sec) = obtenerAsistenciaActualPorGrado()
        val totalEstudiantes = prim + sec
        val gramajeTotalKg = (prim * GRAMAJE_KG_PRIMARIA) + (sec * GRAMAJE_KG_SECUNDARIA)

        binding.txtJornadaActual.text = "Jornada: ${obtenerNombreJornada()}"
        binding.txtDesglosePrimariaSecundaria.text = "Primaria (Transición a 5°): $prim | Secundaria (6°-11°): $sec"
        binding.txtGramajeTotalCalculado.text = String.format(Locale.US, "Requerimiento Total Gramaje: %.1f kg PAE", gramajeTotalKg)

        if (totalEstudiantes > 0) {
            binding.txtCantidadAsistencia.text = "$totalEstudiantes estudiantes"
            binding.txtEstadoAsistencia.text = "Asistencia Registrada"
            binding.txtEstadoAsistencia.setTextColor(ContextCompat.getColor(this, R.color.verde_prinpipal))
            binding.btnRegistrarAsistencia.text = "Actualizar asistencia por grados (Transición a 11°)"
        } else {
            binding.txtCantidadAsistencia.text = "Sin registrar"
            binding.txtEstadoAsistencia.text = "Pendiente"
            binding.txtEstadoAsistencia.setTextColor(ContextCompat.getColor(this, R.color.rojo_principal))
            binding.btnRegistrarAsistencia.text = "Registrar asistencia por grados (Transición a 11°)"
        }
    }

    private fun obtenerAsistenciaActualPorGrado(): Pair<Int, Int> {
        return if (jornadaActual == "MANANA") {
            Pair(primariaManana, secundariaManana)
        } else {
            Pair(primariaTarde, secundariaTarde)
        }
    }

    private fun mostrarDialogoAsistencia() {
        val vistaDialogo = LayoutInflater.from(this).inflate(
            R.layout.dialog_registrar_asistencia,
            null
        )

        val txtJornada = vistaDialogo.findViewById<TextView>(R.id.txtJornadaDialog)
        val edtJardin = vistaDialogo.findViewById<EditText>(R.id.edtGradoJardin)
        val edtP1 = vistaDialogo.findViewById<EditText>(R.id.edtGradoPrimero)
        val edtP2 = vistaDialogo.findViewById<EditText>(R.id.edtGradoSegundo)
        val edtP3 = vistaDialogo.findViewById<EditText>(R.id.edtGradoTercero)
        val edtP4 = vistaDialogo.findViewById<EditText>(R.id.edtGradoCuarto)
        val edtP5 = vistaDialogo.findViewById<EditText>(R.id.edtGradoQuinto)

        val edtS6 = vistaDialogo.findViewById<EditText>(R.id.edtGradoSexto)
        val edtS7 = vistaDialogo.findViewById<EditText>(R.id.edtGradoSeptimo)
        val edtS8 = vistaDialogo.findViewById<EditText>(R.id.edtGradoOctavo)
        val edtS9 = vistaDialogo.findViewById<EditText>(R.id.edtGradoNoveno)
        val edtS10 = vistaDialogo.findViewById<EditText>(R.id.edtGradoDecimo)
        val edtS11 = vistaDialogo.findViewById<EditText>(R.id.edtCantidadSecundaria)

        val txtTotalCalc = vistaDialogo.findViewById<TextView>(R.id.txtTotalEstudiantesCalculado)
        val txtGramajeCalc = vistaDialogo.findViewById<TextView>(R.id.txtGramajeTotalEstimado)

        val btnCancelar = vistaDialogo.findViewById<Button>(R.id.btnCancelarAsistencia)
        val btnGuardar = vistaDialogo.findViewById<Button>(R.id.btnGuardarAsistencia)

        txtJornada.text = "Jornada: ${obtenerNombreJornada()}"

        val (primActual, secActual) = obtenerAsistenciaActualPorGrado()
        if (primActual > 0) {
            val base = primActual / 6
            edtJardin.setText(base.toString())
            edtP1.setText(base.toString())
            edtP2.setText(base.toString())
            edtP3.setText(base.toString())
            edtP4.setText(base.toString())
            edtP5.setText((primActual - (base * 5)).toString())
        }
        if (secActual > 0) {
            val baseS = secActual / 6
            edtS6.setText(baseS.toString())
            edtS7.setText(baseS.toString())
            edtS8.setText(baseS.toString())
            edtS9.setText(baseS.toString())
            edtS10.setText(baseS.toString())
            edtS11.setText((secActual - (baseS * 5)).toString())
        }

        fun recalcularCalculosModal() {
            val jar = edtJardin.text.toString().toIntOrNull() ?: 0
            val p1 = edtP1.text.toString().toIntOrNull() ?: 0
            val p2 = edtP2.text.toString().toIntOrNull() ?: 0
            val p3 = edtP3.text.toString().toIntOrNull() ?: 0
            val p4 = edtP4.text.toString().toIntOrNull() ?: 0
            val p5 = edtP5.text.toString().toIntOrNull() ?: 0

            val s6 = edtS6.text.toString().toIntOrNull() ?: 0
            val s7 = edtS7.text.toString().toIntOrNull() ?: 0
            val s8 = edtS8.text.toString().toIntOrNull() ?: 0
            val s9 = edtS9.text.toString().toIntOrNull() ?: 0
            val s10 = edtS10.text.toString().toIntOrNull() ?: 0
            val s11 = edtS11.text.toString().toIntOrNull() ?: 0

            val totalPrimaria = jar + p1 + p2 + p3 + p4 + p5
            val totalSecundaria = s6 + s7 + s8 + s9 + s10 + s11
            val total = totalPrimaria + totalSecundaria

            val gramajeKg = (jar * GRAMAJE_KG_JARDIN) + ((p1 + p2 + p3 + p4 + p5) * GRAMAJE_KG_PRIMARIA) + (totalSecundaria * GRAMAJE_KG_SECUNDARIA)

            txtTotalCalc.text = "Total Estudiantes: $total (Primaria: $totalPrimaria | Secundaria: $totalSecundaria)"
            txtGramajeCalc.text = String.format(Locale.US, "Gramaje total estimado: %.1f kg a cocinar", gramajeKg)
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                recalcularCalculosModal()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        edtJardin.addTextChangedListener(watcher)
        edtP1.addTextChangedListener(watcher)
        edtP2.addTextChangedListener(watcher)
        edtP3.addTextChangedListener(watcher)
        edtP4.addTextChangedListener(watcher)
        edtP5.addTextChangedListener(watcher)
        edtS6.addTextChangedListener(watcher)
        edtS7.addTextChangedListener(watcher)
        edtS8.addTextChangedListener(watcher)
        edtS9.addTextChangedListener(watcher)
        edtS10.addTextChangedListener(watcher)
        edtS11.addTextChangedListener(watcher)
        recalcularCalculosModal()

        val dialog = AlertDialog.Builder(this)
            .setView(vistaDialogo)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener { dialog.dismiss() }

        btnGuardar.setOnClickListener {
            val jar = edtJardin.text.toString().trim().toIntOrNull() ?: 0
            val p1 = edtP1.text.toString().trim().toIntOrNull() ?: 0
            val p2 = edtP2.text.toString().trim().toIntOrNull() ?: 0
            val p3 = edtP3.text.toString().trim().toIntOrNull() ?: 0
            val p4 = edtP4.text.toString().trim().toIntOrNull() ?: 0
            val p5 = edtP5.text.toString().trim().toIntOrNull() ?: 0

            val s6 = edtS6.text.toString().trim().toIntOrNull() ?: 0
            val s7 = edtS7.text.toString().trim().toIntOrNull() ?: 0
            val s8 = edtS8.text.toString().trim().toIntOrNull() ?: 0
            val s9 = edtS9.text.toString().trim().toIntOrNull() ?: 0
            val s10 = edtS10.text.toString().trim().toIntOrNull() ?: 0
            val s11 = edtS11.text.toString().trim().toIntOrNull() ?: 0

            val totalPrimaria = jar + p1 + p2 + p3 + p4 + p5
            val totalSecundaria = s6 + s7 + s8 + s9 + s10 + s11
            val total = totalPrimaria + totalSecundaria

            if (total <= 0) {
                Toast.makeText(this, "Ingresa al menos un estudiante en algún grado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            guardarAsistenciaEnBackend(totalPrimaria, totalSecundaria, total, dialog, btnGuardar)
        }

        dialog.show()

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun guardarAsistenciaEnBackend(
        totalPrimaria: Int,
        totalSecundaria: Int,
        total: Int,
        dialog: AlertDialog,
        btnGuardar: Button
    ) {
        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."

        lifecycleScope.launch {
            try {
                val userId = sessionManager.getUserId().takeIf { it > 0 }
                val idP = if (jornadaActual == "MANANA") idAsistenciaPrimariaManana else idAsistenciaPrimariaTarde
                val idS = if (jornadaActual == "MANANA") idAsistenciaSecundariaManana else idAsistenciaSecundariaTarde

                // 1. Guardar/Actualizar Primaria
                val payloadPrimaria = AsistenciaDiaria(
                    idAsistencia = idP,
                    idGrado = idGradoPrimaria,
                    fecha = fechaHoy,
                    ninosPresentes = totalPrimaria,
                    idUsuarioManipuladora = userId
                )
                val resP = if (idP != null) {
                    asistenciaRepository.actualizarAsistencia(idP, payloadPrimaria)
                } else {
                    asistenciaRepository.registrarAsistencia(payloadPrimaria)
                }

                // 2. Guardar/Actualizar Secundaria
                val payloadSecundaria = AsistenciaDiaria(
                    idAsistencia = idS,
                    idGrado = idGradoSecundaria,
                    fecha = fechaHoy,
                    ninosPresentes = totalSecundaria,
                    idUsuarioManipuladora = userId
                )
                val resS = if (idS != null) {
                    asistenciaRepository.actualizarAsistencia(idS, payloadSecundaria)
                } else {
                    asistenciaRepository.registrarAsistencia(payloadSecundaria)
                }

                if (resP.isSuccessful || resS.isSuccessful) {
                    if (resP.isSuccessful && resP.body() != null) {
                        if (jornadaActual == "MANANA") idAsistenciaPrimariaManana = resP.body()!!.idAsistencia
                        else idAsistenciaPrimariaTarde = resP.body()!!.idAsistencia
                    }
                    if (resS.isSuccessful && resS.body() != null) {
                        if (jornadaActual == "MANANA") idAsistenciaSecundariaManana = resS.body()!!.idAsistencia
                        else idAsistenciaSecundariaTarde = resS.body()!!.idAsistencia
                    }

                    if (jornadaActual == "MANANA") {
                        primariaManana = totalPrimaria
                        secundariaManana = totalSecundaria
                        AsistenciaManager.guardarManana(this@AsistenciaJefaActivity, total)
                    } else {
                        primariaTarde = totalPrimaria
                        secundariaTarde = totalSecundaria
                        AsistenciaManager.guardarTarde(this@AsistenciaJefaActivity, total)
                    }

                    // Guardar en cache local para acceso inmediato
                    AsistenciaManager.guardarPrimaria(this@AsistenciaJefaActivity, totalPrimaria)
                    AsistenciaManager.guardarSecundaria(this@AsistenciaJefaActivity, totalSecundaria)

                    actualizarDatosAsistencia()
                    dialog.dismiss()

                    val gramajeTotal = (totalPrimaria * GRAMAJE_KG_PRIMARIA) + (totalSecundaria * GRAMAJE_KG_SECUNDARIA)
                    Toast.makeText(
                        this@AsistenciaJefaActivity,
                        "¡Asistencia registrada! Primaria: $totalPrimaria | Secundaria: $totalSecundaria (Total: $total)",
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
                Toast.makeText(this@AsistenciaJefaActivity, "Error de red al guardar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun obtenerNombreJornada(): String {
        return if (jornadaActual == "MANANA") "Mañana" else "Tarde"
    }
}
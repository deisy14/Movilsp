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

    private var primariaManana = 0
    private var secundariaManana = 0

    private var primariaTarde = 0
    private var secundariaTarde = 0

    private var idAsistenciaManana: Int? = null
    private var idAsistenciaTarde: Int? = null
    private var gradoId: Int = 9

    private val GRAMAJE_KG_JARDIN = 0.15 // 150g
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

    private fun configurarFecha() {
        val formato = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES"))
        val fecha = formato.format(Date())
        binding.txtFechaAsistencia.text = "Hoy es $fecha"
    }

    private fun cargarAsistenciaGuardada() {
        val totalManana = AsistenciaManager.obtenerManana(this)
        val totalTarde = AsistenciaManager.obtenerTarde(this)

        if (totalManana > 0) {
            primariaManana = (totalManana * 0.6).toInt()
            secundariaManana = totalManana - primariaManana
        }
        if (totalTarde > 0) {
            primariaTarde = (totalTarde * 0.6).toInt()
            secundariaTarde = totalTarde - primariaTarde
        }
    }

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
                // Silencioso en fallo de red
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

        if (jornadaActual == "MANANA") {
            binding.btnManana.backgroundTintList = android.content.res.ColorStateList.valueOf(amarillo)
            binding.btnManana.setTextColor(ContextCompat.getColor(this, R.color.negro_principal))

            binding.btnTarde.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris_claro))
            binding.btnTarde.setTextColor(ContextCompat.getColor(this, R.color.negro_principal))
        } else {
            binding.btnTarde.backgroundTintList = android.content.res.ColorStateList.valueOf(amarillo)
            binding.btnTarde.setTextColor(ContextCompat.getColor(this, R.color.negro_principal))

            binding.btnManana.backgroundTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gris_claro))
            binding.btnManana.setTextColor(ContextCompat.getColor(this, R.color.negro_principal))
        }
    }

    private fun actualizarDatosAsistencia() {
        val (prim, sec) = obtenerAsistenciaActualPorGrado()
        val totalEstudiantes = prim + sec
        val gramajeTotalKg = (prim * GRAMAJE_KG_PRIMARIA) + (sec * GRAMAJE_KG_SECUNDARIA)

        binding.txtJornadaActual.text = "Jornada: ${obtenerNombreJornada()}"
        binding.txtDesglosePrimariaSecundaria.text = "Jardín-Primaria: $prim | Secundaria (6°-11°): $sec"
        binding.txtGramajeTotalCalculado.text = String.format(Locale.US, "Requerimiento Total Gramaje: %.1f kg PAE", gramajeTotalKg)

        if (totalEstudiantes > 0) {
            binding.txtCantidadAsistencia.text = "$totalEstudiantes estudiantes"
            binding.txtEstadoAsistencia.text = "Asistencia Registrada"
            binding.txtEstadoAsistencia.setTextColor(ContextCompat.getColor(this, R.color.verde_prinpipal))
            binding.btnRegistrarAsistencia.text = "Actualizar asistencia por grados (Jardín a 11°)"
        } else {
            binding.txtCantidadAsistencia.text = "Sin registrar"
            binding.txtEstadoAsistencia.text = "Pendiente"
            binding.txtEstadoAsistencia.setTextColor(ContextCompat.getColor(this, R.color.rojo_principal))
            binding.btnRegistrarAsistencia.text = "Registrar asistencia por grados (Jardín a 11°)"
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
            val base = primActual / 5
            edtP1.setText(base.toString())
            edtP2.setText(base.toString())
            edtP3.setText(base.toString())
            edtP4.setText(base.toString())
            edtP5.setText((primActual - (base * 4)).toString())
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

            val totalPrimaria = p1 + p2 + p3 + p4 + p5
            val totalSecundaria = s6 + s7 + s8 + s9 + s10 + s11
            val total = jar + totalPrimaria + totalSecundaria

            val gramajeKg = (jar * GRAMAJE_KG_JARDIN) + (totalPrimaria * GRAMAJE_KG_PRIMARIA) + (totalSecundaria * GRAMAJE_KG_SECUNDARIA)

            txtTotalCalc.text = "Total Estudiantes: $total (Jardín: $jar | Primaria: $totalPrimaria | Bachillerato: $totalSecundaria)"
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
                        "¡Asistencia de $total estudiantes (Jardín a 11°) registrada! Gramaje PAE: ${String.format(Locale.US, "%.1f", gramajeTotal)} kg",
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
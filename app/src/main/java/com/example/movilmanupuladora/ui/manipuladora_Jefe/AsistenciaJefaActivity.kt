package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private var idGradoSecundaria: Int = 19

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
        sessionManager.fetchAuthToken()

        configurarFecha()
        cargarAsistenciaGuardada()
        configurarJornadas()
        configurarBotones()
        actualizarInterfaz()

        cargarAsistenciaDelBackend()
    }

    private fun configurarFecha() {
        try {
            val formato = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "CO"))
            val fecha = formato.format(Date()).replaceFirstChar { it.uppercase() }
            binding.txtFechaAsistencia.text = fecha
        } catch (_: Exception) {
            val formato = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.getDefault())
            val fecha = formato.format(Date()).replaceFirstChar { it.uppercase() }
            binding.txtFechaAsistencia.text = fecha
        }
    }

    private fun cargarAsistenciaGuardada() {
        val p = AsistenciaManager.obtenerPrimaria(this)
        val s = AsistenciaManager.obtenerSecundaria(this)
        val totM = AsistenciaManager.obtenerManana(this)
        val totT = AsistenciaManager.obtenerTarde(this)

        if (p > 0 || s > 0) {
            primariaManana = p
            secundariaManana = s
        } else if (totM > 0) {
            primariaManana = (totM * 0.6).toInt()
            secundariaManana = totM - primariaManana
        }

        if (totT > 0) {
            primariaTarde = (totT * 0.6).toInt()
            secundariaTarde = totT - primariaTarde
        }
    }

    private fun cargarAsistenciaDelBackend() {
        lifecycleScope.launch {
            try {
                sessionManager.fetchAuthToken()

                // 1. Primero intentar sincronizar desde Turnos (inmediato en tiempo real)
                PreparacionSyncManager.sincronizarDesdeBackend(this@AsistenciaJefaActivity)
                val pSync = AsistenciaManager.obtenerPrimaria(this@AsistenciaJefaActivity)
                val sSync = AsistenciaManager.obtenerSecundaria(this@AsistenciaJefaActivity)
                val totMSync = AsistenciaManager.obtenerManana(this@AsistenciaJefaActivity)
                val totTSync = AsistenciaManager.obtenerTarde(this@AsistenciaJefaActivity)

                if (pSync > 0 || sSync > 0) {
                    primariaManana = pSync
                    secundariaManana = sSync
                } else if (totMSync > 0) {
                    primariaManana = (totMSync * 0.6).toInt()
                    secundariaManana = totMSync - primariaManana
                }
                if (totTSync > 0) {
                    primariaTarde = (totTSync * 0.6).toInt()
                    secundariaTarde = totTSync - primariaTarde
                }

                // 2. Identificar IDs de grados para Primaria y Secundaria del catálogo
                val gradosRes = asistenciaRepository.obtenerGrados()
                if (gradosRes.isSuccessful && !gradosRes.body().isNullOrEmpty()) {
                    val grados = gradosRes.body()!!
                    idGradoPrimaria = grados.find { it.obtenerNombreReal().contains("primaria", ignoreCase = true) }?.obtenerIdReal() ?: 16
                    idGradoSecundaria = grados.find { it.obtenerNombreReal().contains("secundaria", ignoreCase = true) }?.obtenerIdReal() ?: 19
                }

                // 3. Obtener asistencias de hoy del backend
                val res = asistenciaRepository.obtenerAsistencias()
                if (res.isSuccessful && res.body() != null) {
                    val lista = res.body()!!
                    var registrosHoy = lista.filter { it.fecha.trim().startsWith(fechaHoy) }
                    if (registrosHoy.isEmpty() && lista.isNotEmpty()) {
                        val fechaMasReciente = lista.maxByOrNull { it.obtenerIdReal() ?: 0 }?.fecha?.trim()?.take(10)
                        if (!fechaMasReciente.isNullOrEmpty()) {
                            registrosHoy = lista.filter { it.fecha.trim().startsWith(fechaMasReciente) }
                        }
                    }

                    val regPrimaria = registrosHoy
                        .filter { it.obtenerIdGradoReal() == idGradoPrimaria || it.obtenerIdGradoReal() == 16 }
                        .maxByOrNull { it.obtenerIdReal() ?: 0 }

                    val regSecundaria = registrosHoy
                        .filter { it.obtenerIdGradoReal() == idGradoSecundaria || it.obtenerIdGradoReal() == 19 }
                        .maxByOrNull { it.obtenerIdReal() ?: 0 }

                    if (regPrimaria != null) {
                        primariaManana = regPrimaria.ninosPresentes
                        idAsistenciaPrimariaManana = regPrimaria.obtenerIdReal()
                    }
                    if (regSecundaria != null) {
                        secundariaManana = regSecundaria.ninosPresentes
                        idAsistenciaSecundariaManana = regSecundaria.obtenerIdReal()
                    }

                    if (regPrimaria == null && regSecundaria == null && registrosHoy.isNotEmpty()) {
                        val regLegacy = registrosHoy.maxByOrNull { it.obtenerIdReal() ?: 0 }!!
                        val total = regLegacy.ninosPresentes
                        primariaManana = (total * 0.6).toInt()
                        secundariaManana = total - primariaManana
                    }

                    val totalCalculado = primariaManana + secundariaManana
                    if (totalCalculado > 0) {
                        AsistenciaManager.guardarPrimaria(this@AsistenciaJefaActivity, primariaManana)
                        AsistenciaManager.guardarSecundaria(this@AsistenciaJefaActivity, secundariaManana)
                        AsistenciaManager.guardarManana(this@AsistenciaJefaActivity, totalCalculado)
                    }
                }

                actualizarInterfaz()
            } catch (e: Exception) {
                actualizarInterfaz()
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

        binding.cardNivelPrimaria.setOnClickListener {
            mostrarDialogoAsistencia(modoInicial = "PRIMARIA")
        }

        binding.cardNivelSecundaria.setOnClickListener {
            mostrarDialogoAsistencia(modoInicial = "SECUNDARIA")
        }

        binding.btnRegistrarAsistencia.setOnClickListener {
            val (p, s) = obtenerAsistenciaActualPorGrado()
            val total = p + s
            if (total == 0) {
                mostrarDialogoAsistencia(modoInicial = null)
            } else {
                guardarAsistenciaEnBackend(p, s, total, dialog = null, btnGuardarModal = null)
            }
        }
    }

    private fun actualizarInterfaz() {
        actualizarBotonesJornada()
        actualizarDatosAsistencia()
    }

    private fun actualizarBotonesJornada() {
        val colorAmarillo = Color.parseColor("#F4B41F")
        val colorVerdeOscuro = Color.parseColor("#1B3317")
        val colorBlanco = Color.WHITE
        val colorBordeGris = Color.parseColor("#CBD5E1")

        if (jornadaActual == "MANANA") {
            binding.btnManana.backgroundTintList = ColorStateList.valueOf(colorAmarillo)
            binding.btnManana.setTextColor(colorVerdeOscuro)
            binding.btnManana.iconTint = ColorStateList.valueOf(colorVerdeOscuro)
            binding.btnManana.strokeWidth = 0

            binding.btnTarde.backgroundTintList = ColorStateList.valueOf(colorBlanco)
            binding.btnTarde.setTextColor(colorVerdeOscuro)
            binding.btnTarde.iconTint = ColorStateList.valueOf(colorVerdeOscuro)
            binding.btnTarde.strokeColor = ColorStateList.valueOf(colorBordeGris)
            binding.btnTarde.strokeWidth = dpToPx(1f)
        } else {
            binding.btnTarde.backgroundTintList = ColorStateList.valueOf(colorAmarillo)
            binding.btnTarde.setTextColor(colorVerdeOscuro)
            binding.btnTarde.iconTint = ColorStateList.valueOf(colorVerdeOscuro)
            binding.btnTarde.strokeWidth = 0

            binding.btnManana.backgroundTintList = ColorStateList.valueOf(colorBlanco)
            binding.btnManana.setTextColor(colorVerdeOscuro)
            binding.btnManana.iconTint = ColorStateList.valueOf(colorVerdeOscuro)
            binding.btnManana.strokeColor = ColorStateList.valueOf(colorBordeGris)
            binding.btnManana.strokeWidth = dpToPx(1f)
        }
    }

    private fun actualizarDatosAsistencia() {
        val (prim, sec) = obtenerAsistenciaActualPorGrado()

        // Actualizar subtítulos en las tarjetas de nivel según la jornada seleccionada
        if (prim > 0) {
            binding.txtSubtituloPrimaria.text = "$prim estudiantes registrados"
            binding.txtSubtituloPrimaria.setTextColor(Color.parseColor("#1B3317"))
        } else {
            binding.txtSubtituloPrimaria.text = "Registro de asistencia"
            binding.txtSubtituloPrimaria.setTextColor(Color.parseColor("#64748B"))
        }

        if (sec > 0) {
            binding.txtSubtituloSecundaria.text = "$sec estudiantes registrados"
            binding.txtSubtituloSecundaria.setTextColor(Color.parseColor("#1B3317"))
        } else {
            binding.txtSubtituloSecundaria.text = "Registro de asistencia"
            binding.txtSubtituloSecundaria.setTextColor(Color.parseColor("#64748B"))
        }

        // Actualizar tabla de resumen (Mañana vs Tarde)
        val totManana = primariaManana + secundariaManana
        val totTarde = primariaTarde + secundariaTarde

        binding.tvResumenPrimariaManana.text = primariaManana.toString()
        binding.tvResumenPrimariaTarde.text = primariaTarde.toString()

        binding.tvResumenSecundariaManana.text = secundariaManana.toString()
        binding.tvResumenSecundariaTarde.text = secundariaTarde.toString()

        binding.tvResumenTotalManana.text = totManana.toString()
        binding.tvResumenTotalTarde.text = totTarde.toString()
    }

    private fun obtenerAsistenciaActualPorGrado(): Pair<Int, Int> {
        return if (jornadaActual == "MANANA") {
            Pair(primariaManana, secundariaManana)
        } else {
            Pair(primariaTarde, secundariaTarde)
        }
    }

    private fun mostrarDialogoAsistencia(modoInicial: String? = null) {
        val vistaDialogo = LayoutInflater.from(this).inflate(
            R.layout.dialog_registrar_asistencia,
            null
        )

        val txtTitulo = vistaDialogo.findViewById<TextView>(R.id.txtTituloDialog)
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

        val layoutPrimaria = vistaDialogo.findViewById<LinearLayout>(R.id.layoutPrimaria)
        val layoutSecundaria = vistaDialogo.findViewById<LinearLayout>(R.id.layoutSecundaria)

        txtJornada.text = "Jornada: ${obtenerNombreJornada()}"
        if (modoInicial == "PRIMARIA") {
            txtTitulo.text = "Registro de Asistencia: Primaria (Jardín a 5°)"
            layoutPrimaria.visibility = View.VISIBLE
            layoutSecundaria.visibility = View.GONE
        } else if (modoInicial == "SECUNDARIA") {
            txtTitulo.text = "Registro de Asistencia: Secundaria (6° a 11°)"
            layoutPrimaria.visibility = View.GONE
            layoutSecundaria.visibility = View.VISIBLE
        } else {
            txtTitulo.text = "Registrar Asistencia por Grado"
            layoutPrimaria.visibility = View.VISIBLE
            layoutSecundaria.visibility = View.VISIBLE
        }

        // Cargar los valores guardados previamente
        val detalle = AsistenciaManager.obtenerDetalleGradosHoy(this)
        edtJardin.setText(detalle["jar"].orEmpty())
        edtP1.setText(detalle["p1"].orEmpty())
        edtP2.setText(detalle["p2"].orEmpty())
        edtP3.setText(detalle["p3"].orEmpty())
        edtP4.setText(detalle["p4"].orEmpty())
        edtP5.setText(detalle["p5"].orEmpty())

        edtS6.setText(detalle["s6"].orEmpty())
        edtS7.setText(detalle["s7"].orEmpty())
        edtS8.setText(detalle["s8"].orEmpty())
        edtS9.setText(detalle["s9"].orEmpty())
        edtS10.setText(detalle["s10"].orEmpty())
        edtS11.setText(detalle["s11"].orEmpty())

        fun recalcularCalculosModal() {
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
            val strJar = edtJardin.text.toString().trim()
            val strP1 = edtP1.text.toString().trim()
            val strP2 = edtP2.text.toString().trim()
            val strP3 = edtP3.text.toString().trim()
            val strP4 = edtP4.text.toString().trim()
            val strP5 = edtP5.text.toString().trim()

            val strS6 = edtS6.text.toString().trim()
            val strS7 = edtS7.text.toString().trim()
            val strS8 = edtS8.text.toString().trim()
            val strS9 = edtS9.text.toString().trim()
            val strS10 = edtS10.text.toString().trim()
            val strS11 = edtS11.text.toString().trim()

            val jar = strJar.toIntOrNull() ?: 0
            val p1 = strP1.toIntOrNull() ?: 0
            val p2 = strP2.toIntOrNull() ?: 0
            val p3 = strP3.toIntOrNull() ?: 0
            val p4 = strP4.toIntOrNull() ?: 0
            val p5 = strP5.toIntOrNull() ?: 0

            val s6 = strS6.toIntOrNull() ?: 0
            val s7 = strS7.toIntOrNull() ?: 0
            val s8 = strS8.toIntOrNull() ?: 0
            val s9 = strS9.toIntOrNull() ?: 0
            val s10 = strS10.toIntOrNull() ?: 0
            val s11 = strS11.toIntOrNull() ?: 0

            val totalPrimaria = jar + p1 + p2 + p3 + p4 + p5
            val totalSecundaria = s6 + s7 + s8 + s9 + s10 + s11
            val total = totalPrimaria + totalSecundaria

            // Persistir los campos vacíos o con números en el gestor local
            val mapaDetalle = mapOf(
                "jar" to strJar, "p1" to strP1, "p2" to strP2, "p3" to strP3, "p4" to strP4, "p5" to strP5,
                "s6" to strS6, "s7" to strS7, "s8" to strS8, "s9" to strS9, "s10" to strS10, "s11" to strS11
            )
            AsistenciaManager.guardarDetalleGradosHoy(this@AsistenciaJefaActivity, mapaDetalle)

            if (total == 0) {
                AlertDialog.Builder(this)
                    .setTitle("Sin asistencia registrada")
                    .setMessage("Todos los campos están en 0. ¿Deseas registrar este día sin estudiantes?")
                    .setPositiveButton("Sí, guardar 0") { _, _ ->
                        guardarAsistenciaEnBackend(0, 0, 0, dialog, btnGuardar)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
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
        dialog: AlertDialog? = null,
        btnGuardarModal: Button? = null
    ) {
        btnGuardarModal?.isEnabled = false
        btnGuardarModal?.text = "Guardando..."
        binding.btnRegistrarAsistencia.isEnabled = false
        binding.btnRegistrarAsistencia.text = "Guardando asistencia..."

        // 1. Guardar localmente de inmediato en AsistenciaManager
        if (jornadaActual == "MANANA") {
            primariaManana = totalPrimaria
            secundariaManana = totalSecundaria
            AsistenciaManager.guardarManana(this@AsistenciaJefaActivity, total)
        } else {
            primariaTarde = totalPrimaria
            secundariaTarde = totalSecundaria
            AsistenciaManager.guardarTarde(this@AsistenciaJefaActivity, total)
        }
        AsistenciaManager.guardarPrimaria(this@AsistenciaJefaActivity, totalPrimaria)
        AsistenciaManager.guardarSecundaria(this@AsistenciaJefaActivity, totalSecundaria)
        actualizarDatosAsistencia()

        lifecycleScope.launch {
            try {
                // 2. Sincronizar en la nube en Turnos de inmediato con el token de sesión (para todos los móviles)
                PreparacionSyncManager.guardarAsistenciaEnNube(
                    context = applicationContext,
                    primaria = totalPrimaria,
                    secundaria = totalSecundaria,
                    total = total,
                    jornada = jornadaActual
                )

                // 3. Persistir en la tabla de asistencia diaria del backend
                try {
                    sessionManager.fetchAuthToken()
                    val userId = sessionManager.getUserId().takeIf { it > 0 }
                    val idP = if (jornadaActual == "MANANA") idAsistenciaPrimariaManana else idAsistenciaPrimariaTarde
                    val idS = if (jornadaActual == "MANANA") idAsistenciaSecundariaManana else idAsistenciaSecundariaTarde

                    val payloadPrimaria = AsistenciaDiaria(
                        idAsistencia = idP,
                        idGrado = idGradoPrimaria,
                        grado = idGradoPrimaria,
                        fecha = fechaHoy,
                        ninosPresentes = totalPrimaria,
                        idUsuarioManipuladora = userId,
                        usuarioManipuladora = userId
                    )
                    var resP = if (idP != null) {
                        asistenciaRepository.actualizarAsistencia(idP, payloadPrimaria)
                    } else null
                    if (resP == null || !resP.isSuccessful) {
                        resP = asistenciaRepository.registrarAsistencia(payloadPrimaria)
                    }
                    if (resP.isSuccessful && resP.body() != null) {
                        val nuevoId = resP.body()!!.obtenerIdReal()
                        if (jornadaActual == "MANANA") idAsistenciaPrimariaManana = nuevoId
                        else idAsistenciaPrimariaTarde = nuevoId
                    }

                    val payloadSecundaria = AsistenciaDiaria(
                        idAsistencia = idS,
                        idGrado = idGradoSecundaria,
                        grado = idGradoSecundaria,
                        fecha = fechaHoy,
                        ninosPresentes = totalSecundaria,
                        idUsuarioManipuladora = userId,
                        usuarioManipuladora = userId
                    )
                    var resS = if (idS != null) {
                        asistenciaRepository.actualizarAsistencia(idS, payloadSecundaria)
                    } else null
                    if (resS == null || !resS.isSuccessful) {
                        resS = asistenciaRepository.registrarAsistencia(payloadSecundaria)
                    }
                    if (resS.isSuccessful && resS.body() != null) {
                        val nuevoId = resS.body()!!.obtenerIdReal()
                        if (jornadaActual == "MANANA") idAsistenciaSecundariaManana = nuevoId
                        else idAsistenciaSecundariaTarde = nuevoId
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AsistenciaJefa", "Error registrando asistencia en backend: ${e.message}")
                }

                dialog?.dismiss()
                binding.btnRegistrarAsistencia.isEnabled = true
                binding.btnRegistrarAsistencia.text = "Guardar asistencia"

                Toast.makeText(
                    this@AsistenciaJefaActivity,
                    "¡Asistencia sincronizada! Total: $total ($jornadaActual - Primaria: $totalPrimaria | Secundaria: $totalSecundaria)",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                dialog?.dismiss()
                binding.btnRegistrarAsistencia.isEnabled = true
                binding.btnRegistrarAsistencia.text = "Guardar asistencia"

                Toast.makeText(
                    this@AsistenciaJefaActivity,
                    "¡Asistencia guardada localmente! Total: $total",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun obtenerNombreJornada(): String {
        return if (jornadaActual == "MANANA") "Mañana" else "Tarde"
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}

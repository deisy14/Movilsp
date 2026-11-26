package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.ConfirmarRecepcionRequest
import com.example.movilmanupuladora.databinding.ActivityRegistrarEntregaBinding
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegistrarEntregaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrarEntregaBinding
    private lateinit var sessionManager: SessionManager

    data class ItemIngredienteVerificar(
        val id: Int,
        val nombre: String,
        val cantidad: Double,
        val unidad: String,
        val descripcion: String,
        var verificado: Boolean = true,
        val idEntrega: Int? = null,
        var tieneNovedad: Boolean = false,
        var cantidadRecibida: Double = cantidad,
        var cantidadFaltante: Double = 0.0,
        var motivoFaltante: String = ""
    )

    // Lista inicialmente vacía para evitar mostrar datos ficticios (Carlos Méndez)
    private val listaIngredientesDespacho = mutableListOf<ItemIngredienteVerificar>()

    private var numeroRemision = ""
    private var nombreSupervisor = ""
    private var entregaYaAceptada = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityRegistrarEntregaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutPrincipalEntrega) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        sessionManager = SessionManager(this)
        sessionManager.fetchAuthToken()
        RetrofitClient.cargarToken(this)

        if (intent.hasExtra("numero_remision")) {
            numeroRemision = intent.getStringExtra("numero_remision")?.trim() ?: ""
        }
        if (intent.hasExtra("supervisor_nombre")) {
            nombreSupervisor = intent.getStringExtra("supervisor_nombre")?.trim() ?: ""
        }

        verificarSiYaFueAceptadaLocalmente()
        configurarEncabezado()
        renderizarIngredientes()
        cargarEntregasDesdeServidor()
        configurarLienzoFirma()
        configurarBotonesAccion()
    }

    private fun normalizarClaveRemision(rem: String): String {
        return rem.replace("[^A-Za-z0-9_-]".toRegex(), "")
    }

    private fun verificarSiYaFueAceptadaLocalmente() {
        if (numeroRemision.isBlank()) return
        val prefs = getSharedPreferences("ENTREGAS_RECIBIDAS_PREFS", Context.MODE_PRIVATE)
        val key = normalizarClaveRemision(numeroRemision)
        if (prefs.getBoolean("remision_aceptada_$key", false)) {
            entregaYaAceptada = true
        }
    }

    private fun configurarEncabezado(sedeNombrePersonalizado: String? = null) {
        val sedeNombre = sedeNombrePersonalizado ?: sessionManager.getSedeNombre() ?: "Sede Escolar"
        if (intent.hasExtra("numero_remision") && numeroRemision.isBlank()) {
            numeroRemision = intent.getStringExtra("numero_remision")?.trim() ?: ""
        }
        if (intent.hasExtra("supervisor_nombre") && nombreSupervisor.isBlank()) {
            nombreSupervisor = intent.getStringExtra("supervisor_nombre")?.trim() ?: ""
        }

        val subtitulo = if (numeroRemision.isNotBlank()) {
            "$sedeNombre • Remisión $numeroRemision"
        } else {
            "$sedeNombre • Recepción de Despacho"
        }
        binding.txtSubtituloRemision.text = subtitulo
        binding.txtSedeDestino.text = sedeNombre
        binding.txtDetalleSupervisor.text = if (nombreSupervisor.isNotBlank()) nombreSupervisor else "Supervisor PAE"

        try {
            val formato = SimpleDateFormat("d 'de' MMMM, yyyy", Locale.forLanguageTag("es-CO"))
            binding.txtFechaDespacho.text = formato.format(Date())
        } catch (_: Exception) {
            binding.txtFechaDespacho.text = "Hoy"
        }

        binding.btnVolver.setOnClickListener {
            finish()
        }

        if (entregaYaAceptada) {
            aplicarModoEntregaAceptada()
        }
    }

    private fun cargarEntregasDesdeServidor() {
        val idSede = sessionManager.getSedeId()
        sessionManager.fetchAuthToken()
        RetrofitClient.cargarToken(this)

        lifecycleScope.launch {
            try {
                // Traer todas las entregas para poder consultar tanto las pendientes como las ya recibidas
                val resp = RetrofitClient.apiService.obtenerEntregas(
                    idSede = if (idSede > 0) idSede else null,
                    estado = null
                )
                if (resp.isSuccessful) {
                    val lista = resp.body()
                    if (!lista.isNullOrEmpty()) {
                        val remisionParam = intent.getStringExtra("numero_remision")?.trim() ?: numeroRemision.trim()
                        val entregasRemision = if (remisionParam.isNotBlank()) {
                            lista.filter {
                                val doc = it.documentoEntrega?.trim() ?: ""
                                doc.equals(remisionParam, ignoreCase = true) ||
                                        doc.contains(remisionParam, ignoreCase = true) ||
                                        remisionParam.contains(doc, ignoreCase = true)
                            }
                        } else {
                            val primerDoc = lista.firstOrNull { !it.documentoEntrega.isNullOrBlank() }?.documentoEntrega
                            if (!primerDoc.isNullOrBlank()) {
                                lista.filter { it.documentoEntrega == primerDoc }
                            } else {
                                listOf(lista.first())
                            }
                        }

                        if (entregasRemision.isNotEmpty()) {
                            val docDefinitivo = entregasRemision.first().documentoEntrega?.takeIf { it.isNotBlank() }
                                ?: remisionParam.ifBlank { "#REM-2026-001" }
                            numeroRemision = docDefinitivo

                            val sedeNombre = entregasRemision.firstOrNull()?.sedeNombre
                                ?: sessionManager.getSedeNombre()
                                ?: "Sede Escolar"

                            val supervisor = entregasRemision.firstOrNull()?.observaciones?.let { obs ->
                                val match = Regex("Responsable:\\s*([^|;\\n]+)", RegexOption.IGNORE_CASE).find(obs)
                                match?.groupValues?.get(1)?.trim()
                            } ?: intent.getStringExtra("supervisor_nombre")?.takeIf { it.isNotBlank() } ?: "Santiago"
                            nombreSupervisor = supervisor

                            // Verificar si todos los alimentos de esta remisión ya fueron aceptados
                            val yaAceptadaBackend = entregasRemision.all {
                                (it.estado ?: "").equals("RECIBIDA", ignoreCase = true)
                            }

                            val key = normalizarClaveRemision(numeroRemision)
                            val prefs = getSharedPreferences("ENTREGAS_RECIBIDAS_PREFS", Context.MODE_PRIVATE)
                            val yaAceptadaLocal = prefs.getBoolean("remision_aceptada_$key", false)

                            if (yaAceptadaBackend || yaAceptadaLocal) {
                                entregaYaAceptada = true
                            }

                            listaIngredientesDespacho.clear()
                            for (e in entregasRemision) {
                                val id = e.idIngrediente ?: e.idEntrega ?: (listaIngredientesDespacho.size + 1)
                                val nombreLimpio = InventarioManager.limpiarNombre(e.ingredienteNombre ?: "Alimento PAE")
                                val cant = e.cantidad ?: 1.0
                                val uni = e.unidad ?: "kg"
                                val desc = e.observaciones?.takeIf { it.isNotBlank() } ?: "Víveres entregados por Supervisor"
                                listaIngredientesDespacho.add(
                                    ItemIngredienteVerificar(
                                        id = id,
                                        idEntrega = e.idEntrega,
                                        nombre = nombreLimpio,
                                        cantidad = cant,
                                        unidad = uni,
                                        descripcion = desc,
                                        verificado = true,
                                        tieneNovedad = false,
                                        cantidadRecibida = cant,
                                        cantidadFaltante = 0.0,
                                        motivoFaltante = ""
                                    )
                                )
                            }

                            configurarEncabezado(sedeNombrePersonalizado = sedeNombre)
                            renderizarIngredientes()

                            if (entregaYaAceptada) {
                                aplicarModoEntregaAceptada(null)
                            }
                        } else {
                            mostrarVistaVacia("No se encontraron alimentos para la remisión $numeroRemision")
                        }
                    } else {
                        mostrarVistaVacia("No hay despachos registrados para esta sede escolar.")
                    }
                } else {
                    mostrarVistaVacia("No se pudieron cargar los datos del despacho (Código ${resp.code()}).")
                }
            } catch (e: Exception) {
                mostrarVistaVacia("Error al conectar con el servidor: ${e.message}")
            }
        }
    }

    private fun mostrarVistaVacia(mensaje: String) {
        val contenedor = binding.contenedorIngredientesCheck
        contenedor.removeAllViews()
        binding.txtConteoCheck.text = "0 insumos"
        binding.btnMarcarTodos.visibility = View.GONE

        val txt = TextView(this).apply {
            text = mensaje
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(dpToPx(16f), dpToPx(32f), dpToPx(16f), dpToPx(32f))
        }
        contenedor.addView(txt)
    }

    private fun renderizarIngredientes() {
        val contenedor = binding.contenedorIngredientesCheck
        contenedor.removeAllViews()

        if (listaIngredientesDespacho.isEmpty()) {
            binding.txtConteoCheck.text = "Consultando insumos..."
            binding.btnMarcarTodos.visibility = View.GONE

            val layoutLoading = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(16f), dpToPx(32f), dpToPx(16f), dpToPx(32f))
            }

            val progressBar = ProgressBar(this).apply {
                indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#F4B41F"))
            }
            val txtLoading = TextView(this).apply {
                text = "Cargando insumos despachados por el supervisor..."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(12f), 0, 0)
            }

            layoutLoading.addView(progressBar)
            layoutLoading.addView(txtLoading)
            contenedor.addView(layoutLoading)
            return
        }

        binding.btnMarcarTodos.visibility = if (entregaYaAceptada) View.GONE else View.VISIBLE

        val inflater = LayoutInflater.from(this)

        for (item in listaIngredientesDespacho) {
            val itemView = inflater.inflate(R.layout.item_ingrediente_entrega_check, contenedor, false)
            val card = itemView.findViewById<MaterialCardView>(R.id.cardIngredienteCheck)
            val filaCabecera = itemView.findViewById<LinearLayout>(R.id.filaCabeceraItem)
            val imgCheck = itemView.findViewById<ImageView>(R.id.imgCheckEstado)
            val txtNombre = itemView.findViewById<TextView>(R.id.txtNombreIngrediente)
            val txtDetalle = itemView.findViewById<TextView>(R.id.txtDetalleIngrediente)
            val badge = itemView.findViewById<TextView>(R.id.badgeEstadoCheck)

            txtNombre.text = item.nombre
            txtDetalle.text = "Cantidad: ${formatearNumero(item.cantidad)} ${item.unidad}"

            fun actualizarVisualItem() {
                if (!item.verificado && !entregaYaAceptada) {
                    imgCheck.setImageResource(R.drawable.ic_check_circle)
                    imgCheck.imageTintList = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                    badge.text = "Pendiente"
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                    badge.setTextColor(Color.parseColor("#94A3B8"))
                } else {
                    imgCheck.setImageResource(R.drawable.ic_check_circle)
                    imgCheck.imageTintList = ColorStateList.valueOf(Color.parseColor("#F4B41F"))
                    badge.text = "Conforme"
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    badge.setTextColor(Color.parseColor("#B45309"))

                }
            }

            actualizarVisualItem()

            if (!entregaYaAceptada) {
                val toggleItem = View.OnClickListener {
                    item.verificado = !item.verificado
                    actualizarVisualItem()
                    actualizarConteoCheck()
                }

                filaCabecera.setOnClickListener(toggleItem)
                imgCheck.setOnClickListener(toggleItem)
                card.setOnClickListener(toggleItem)
            } else {
                filaCabecera.isClickable = false
                imgCheck.isClickable = false
                card.isClickable = false
            }

            contenedor.addView(itemView)
        }

        actualizarConteoCheck()

        if (!entregaYaAceptada) {
            binding.btnMarcarTodos.setOnClickListener {
                val todosMarcados = listaIngredientesDespacho.all { it.verificado }
                listaIngredientesDespacho.forEach { it.verificado = !todosMarcados }
                renderizarIngredientes()
            }
        }
    }

    private fun formatearNumero(valor: Double): String {
        return if (valor % 1.0 == 0.0) {
            valor.toInt().toString()
        } else {
            "%.2f".format(Locale.US, valor).trimEnd('0').trimEnd('.')
        }
    }

    private fun actualizarConteoCheck() {
        val total = listaIngredientesDespacho.size
        if (entregaYaAceptada) {
            binding.txtConteoCheck.text = "$total de $total insumos verificados conforme"
            binding.btnMarcarTodos.visibility = View.GONE
            return
        }
        val marcados = listaIngredientesDespacho.count { it.verificado }
        binding.txtConteoCheck.text = "$marcados de $total insumos verificados"
        if (marcados == total) {
            binding.btnMarcarTodos.text = "Desmarcar todos"
        } else {
            binding.btnMarcarTodos.text = "Marcar todos"
        }
    }

    private fun configurarLienzoFirma() {
        binding.signatureView.setOnTouchListener { _, _ ->
            if (!entregaYaAceptada) {
                binding.txtPistaFirma.visibility = View.GONE
            }
            false
        }

        binding.btnBorrarFirma.setOnClickListener {
            if (!entregaYaAceptada) {
                binding.signatureView.clear()
                binding.txtPistaFirma.visibility = View.VISIBLE
            }
        }
    }

    private fun configurarBotonesAccion() {
        binding.btnRechazarEntrega.setOnClickListener {
            if (!entregaYaAceptada) {
                mostrarDialogoRechazo()
            }
        }

        binding.btnAceptarEntrega.setOnClickListener {
            if (entregaYaAceptada) {
                finish() 
            } else {
                confirmarYAceptarEntrega()
            }
        }
    }

    private fun aplicarModoEntregaAceptada(obsGuardada: String? = null) {
        entregaYaAceptada = true

        // Badge superior de estado: Verde "Verificada Conforme"
        binding.badgeEstadoEntrega.text = "Verificada Conforme"
        binding.badgeEstadoEntrega.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
        binding.badgeEstadoEntrega.setTextColor(Color.parseColor("#15803D"))

        // Ocultar botón de marcar todos y actualizar conteo
        binding.btnMarcarTodos.visibility = View.GONE
        binding.txtConteoCheck.text = "${listaIngredientesDespacho.size} de ${listaIngredientesDespacho.size} insumos recibidos conforme"

        // Inhabilitar campo de observaciones mostrando lo registrado
        val obsTexto = obsGuardada?.takeIf { it.isNotBlank() }
            ?: run {
                val key = normalizarClaveRemision(numeroRemision)
                getSharedPreferences("ENTREGAS_RECIBIDAS_PREFS", Context.MODE_PRIVATE)
                    .getString("remision_obs_$key", null)
            } ?: "Víveres verificados y acta firmada en sede escolar."

        binding.edtObservacionesEntrega.setText(obsTexto)
        binding.edtObservacionesEntrega.isEnabled = false
        binding.edtObservacionesEntrega.isFocusable = false

        // Bloquear lienzo de firma digital
        binding.btnBorrarFirma.visibility = View.GONE
        binding.signatureView.isEnabled = false
        binding.txtPistaFirma.text = "✓ Factura firmada y aceptada digitalmente"
        binding.txtPistaFirma.setTextColor(Color.parseColor("#15803D"))
        binding.txtPistaFirma.visibility = View.VISIBLE

        // Ocultar barra inferior (para devolverse se usa el botón < del header)
        binding.barraAccionesEntrega.visibility = View.GONE
        binding.btnRechazarEntrega.visibility = View.GONE
        binding.btnAceptarEntrega.visibility = View.GONE
    }

    private fun mostrarDialogoRechazo() {
        val input = EditText(this).apply {
            hint = "Escribe el motivo del rechazo..."
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Rechazar Entrega")
            .setMessage("¿Estás segura de rechazar la remisión $numeroRemision? El supervisor será notificado.")
            .setView(input)
            .setPositiveButton("Confirmar Rechazo") { _, _ ->
                val motivo = input.text.toString().trim().ifEmpty { "Inconsistencia física en los víveres entregados." }
                notificarSupervisorRechazo(motivo)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun notificarSupervisorRechazo(motivo: String) {
        val sedeNombre = sessionManager.getSedeNombre() ?: "Sede"
        sessionManager.fetchAuthToken()
        RetrofitClient.cargarToken(this)

        lifecycleScope.launch {
            try {
                for (item in listaIngredientesDespacho) {
                    item.idEntrega?.let { idEnt ->
                        try {
                            RetrofitClient.apiService.confirmarRecepcionEntrega(
                                idEnt,
                                ConfirmarRecepcionRequest(
                                    cantidadRecibida = 0.0,
                                    cantidadRechazada = item.cantidad,
                                    motivoRechazo = motivo
                                )
                            )
                        } catch (_: Exception) {}
                    }
                }

                val tituloBody = "⚠️ Entrega Rechazada en $sedeNombre".toRequestBody("text/plain".toMediaTypeOrNull())
                val mensajeBody = "La Jefa de Manipuladoras ha rechazado la remisión $numeroRemision. Motivo: $motivo".toRequestBody("text/plain".toMediaTypeOrNull())

                RetrofitClient.apiService.enviarNotificacion(tituloBody, mensajeBody, null)
            } catch (_: Exception) {}

            Toast.makeText(this@RegistrarEntregaActivity, "La entrega ha sido rechazada y el supervisor fue notificado.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun confirmarYAceptarEntrega() {
        if (entregaYaAceptada) {
            finish()
            return
        }

        if (!binding.signatureView.hasSignature()) {
            Toast.makeText(this, "Por favor estampa tu firma digital en el recuadro antes de aceptar.", Toast.LENGTH_LONG).show()
            return
        }

        val noVerificados = listaIngredientesDespacho.filter { !it.verificado }
        if (noVerificados.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Alimentos sin verificar")
                .setMessage("Hay ${noVerificados.size} insumo(s) sin marcar como recibidos conforme. ¿Deseas continuar y registrar la entrega?")
                .setPositiveButton("Sí, continuar") { _, _ ->
                    ejecutarAceptacionFinal()
                }
                .setNegativeButton("Revisar", null)
                .show()
            return
        }

        ejecutarAceptacionFinal()
    }

    private fun ejecutarAceptacionFinal() {
        if (entregaYaAceptada) {
            finish()
            return
        }

        binding.btnAceptarEntrega.isEnabled = false
        binding.btnAceptarEntrega.text = "Enviando..."

        val obs = binding.edtObservacionesEntrega.text.toString().trim()
            .ifEmpty { "Víveres verificados y acta firmada en sede escolar." }
        val sedeNombre = sessionManager.getSedeNombre() ?: "Sede Central"

        lifecycleScope.launch {
            // 1. PERSISTENCIA LOCAL GARANTIZADA DE ACEPTACIÓN
            val key = normalizarClaveRemision(numeroRemision)
            val prefs = getSharedPreferences("ENTREGAS_RECIBIDAS_PREFS", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("remision_aceptada_$key", true)
                .putString("remision_obs_$key", obs)
                .putLong("remision_fecha_$key", System.currentTimeMillis())
                .apply()

            // 2. AGREGAR AUTOMÁTICAMENTE TODOS LOS PRODUCTOS VERIFICADOS AL INVENTARIO
            val itemsVerificados = listaIngredientesDespacho.filter { it.verificado }
            try {
                InventarioManager.agregarOIncrementarAlimentos(this@RegistrarEntregaActivity, itemsVerificados)
                for (item in itemsVerificados) {
                    InventarioManager.guardarCantidad(
                        alimento = item.nombre,
                        cantidad = item.cantidad,
                        jornada = "MANANA"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("REGISTRAR_ENTREGA", "Error actualizando inventario local: ${e.message}")
            }

            // 3. NOTIFICACIÓN LOCAL EN LA APP PARA LA JEFA
            try {
                NotificacionesJefaManager.agregarNotificacionSede(
                    this@RegistrarEntregaActivity,
                    "✅ Entrega recibida conforme ($numeroRemision)",
                    "Remisión $numeroRemision aceptada e ingresada al inventario de la sede. Observaciones: $obs",
                    "Inventario"
                )
            } catch (_: Exception) {}

            // 4. AUTENTICACIÓN Y CONFIRMACIÓN EN EL BACKEND
            sessionManager.fetchAuthToken()
            RetrofitClient.cargarToken(this@RegistrarEntregaActivity)

            for (item in listaIngredientesDespacho) {
                item.idEntrega?.let { idEnt ->
                    try {
                        val cantRecibida = if (item.verificado) item.cantidad else 0.0
                        val cantRechazada = if (item.verificado) 0.0 else item.cantidad
                        val motivoItem = if (!item.verificado) "No verificado por la sede: $obs" else ""

                        RetrofitClient.apiService.confirmarRecepcionEntrega(
                            idEnt,
                            ConfirmarRecepcionRequest(
                                cantidadRecibida = cantRecibida,
                                cantidadRechazada = cantRechazada,
                                motivoRechazo = motivoItem
                            )
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("REGISTRAR_ENTREGA", "Error confirmando entrega $idEnt en backend: ${e.message}")
                    }
                }
            }

            // 5. NOTIFICACIONES REMOTAS AL SUPERVISOR Y AL ADMINISTRADOR
            try {
                val titSup = "✅ Entrega Aceptada Conforme en $sedeNombre"
                val msgSup = "La remisión $numeroRemision ha sido verificada completa, firmada digitalmente y aceptada por la Jefa de Manipuladoras en $sedeNombre. Observaciones: $obs."
                RetrofitClient.apiService.enviarNotificacion(
                    titSup.toRequestBody("text/plain".toMediaTypeOrNull()),
                    msgSup.toRequestBody("text/plain".toMediaTypeOrNull()),
                    null
                )
            } catch (_: Exception) {}

            try {
                val titAdmin = "🔔 Recepción firmada en sede: $sedeNombre"
                val msgAdmin = "La Jefa de Manipuladoras ha firmado la remisión $numeroRemision en $sedeNombre. Ingreso acreditado al inventario. Observaciones: $obs."
                RetrofitClient.apiService.enviarNotificacion(
                    titAdmin.toRequestBody("text/plain".toMediaTypeOrNull()),
                    msgAdmin.toRequestBody("text/plain".toMediaTypeOrNull()),
                    null
                )
            } catch (_: Exception) {}

            aplicarModoEntregaAceptada(obs)

            val mensajeFinal = "Se ha enviado el comprobante firmado digitalmente.\n\n• El Supervisor ($nombreSupervisor) recibió la confirmación conforme.\n• Todos los insumos verificados fueron agregados al inventario de la sede."

            MaterialAlertDialogBuilder(this@RegistrarEntregaActivity)
                .setTitle("¡Entrega Aceptada Conforme!")
                .setMessage(mensajeFinal)
                .setPositiveButton("Aceptar") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun dpToPx(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}

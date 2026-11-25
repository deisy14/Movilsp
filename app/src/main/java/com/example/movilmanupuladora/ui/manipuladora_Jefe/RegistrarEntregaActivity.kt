package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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

    private val listaIngredientesDespacho = mutableListOf(
        ItemIngredienteVerificar(1, "Arroz Blanco", 50.0, "kg", "Grano seleccionado primera calidad", true),
        ItemIngredienteVerificar(2, "Pechuga de Pollo", 25.0, "kg", "Pollo fresco refrigerado en cadena de frío", true),
        ItemIngredienteVerificar(3, "Lentejas", 15.0, "kg", "Leguminosa seca en empaque hermético", true),
        ItemIngredienteVerificar(4, "Aceite Vegetal", 10.0, "Litros", "Aceite vegetal 100% puro para cocción", true),
        ItemIngredienteVerificar(5, "Tomate Chonto", 8.0, "kg", "Verdura fresca para ensaladas", true)
    )

    private var numeroRemision = "#REM-2026-104"
    private var nombreSupervisor = "Carlos Méndez"

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

        configurarEncabezado()
        renderizarIngredientes()
        cargarEntregasDesdeServidor()
        configurarLienzoFirma()
        configurarBotonesAccion()
    }

    private fun configurarEncabezado(sedeNombrePersonalizado: String? = null) {
        val sedeNombre = sedeNombrePersonalizado ?: sessionManager.getSedeNombre() ?: "Sede Escolar"
        if (intent.hasExtra("numero_remision")) {
            numeroRemision = intent.getStringExtra("numero_remision") ?: numeroRemision
        }
        if (intent.hasExtra("supervisor_nombre")) {
            nombreSupervisor = intent.getStringExtra("supervisor_nombre") ?: nombreSupervisor
        }

        binding.txtSubtituloRemision.text = "$sedeNombre • Remisión $numeroRemision"
        binding.txtSedeDestino.text = sedeNombre
        binding.txtDetalleSupervisor.text = "Supervisor PAE: $nombreSupervisor • Mercados del Mes"

        try {
            val formato = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"))
            binding.txtFechaDespacho.text = formato.format(Date()).replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            binding.txtFechaDespacho.text = "Hoy, 14 Nov 2026"
        }

        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun cargarEntregasDesdeServidor() {
        val idSede = sessionManager.getSedeId()
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.apiService.obtenerEntregas(
                    idSede = if (idSede > 0) idSede else null,
                    estado = "EN_TRANSITO"
                )
                if (resp.isSuccessful) {
                    val lista = resp.body()
                    if (!lista.isNullOrEmpty()) {
                        val remisionParam = intent.getStringExtra("numero_remision")
                        val remisionTarget = if (!remisionParam.isNullOrBlank()) {
                            remisionParam
                        } else {
                            lista.firstOrNull { !it.documentoEntrega.isNullOrBlank() }?.documentoEntrega
                                ?: lista.first().documentoEntrega ?: "#REM-2026-001"
                        }

                        numeroRemision = remisionTarget
                        val entregasRemision = lista.filter { it.documentoEntrega == remisionTarget || remisionTarget.isBlank() }
                        val sedeNombre = entregasRemision.firstOrNull()?.sedeNombre
                            ?: sessionManager.getSedeNombre()
                            ?: "Sede Escolar"

                        val supervisor = entregasRemision.firstOrNull()?.observaciones?.let { obs ->
                            val match = Regex("Responsable:\\s*([^|;\\n]+)", RegexOption.IGNORE_CASE).find(obs)
                            match?.groupValues?.get(1)?.trim()
                        } ?: intent.getStringExtra("supervisor_nombre") ?: "Supervisor PAE"
                        nombreSupervisor = supervisor

                        listaIngredientesDespacho.clear()
                        for (e in entregasRemision) {
                            val id = e.idIngrediente ?: e.idEntrega ?: (listaIngredientesDespacho.size + 1)
                            val nombre = e.ingredienteNombre ?: "Alimento PAE"
                            val cant = e.cantidad ?: 1.0
                            val uni = e.unidad ?: "kg"
                            val desc = e.observaciones?.takeIf { it.isNotBlank() } ?: "Víveres entregados por Supervisor"
                            listaIngredientesDespacho.add(
                                ItemIngredienteVerificar(
                                    id = id,
                                    idEntrega = e.idEntrega,
                                    nombre = nombre,
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
                    }
                }
            } catch (_: Exception) {
                // Conserva los alimentos precargados si el backend no responde
            }
        }
    }

    private fun renderizarIngredientes() {
        val contenedor = binding.contenedorIngredientesCheck
        contenedor.removeAllViews()

        val inflater = LayoutInflater.from(this)

        for (item in listaIngredientesDespacho) {
            val itemView = inflater.inflate(R.layout.item_ingrediente_entrega_check, contenedor, false)
            val card = itemView.findViewById<MaterialCardView>(R.id.cardIngredienteCheck)
            val filaCabecera = itemView.findViewById<LinearLayout>(R.id.filaCabeceraItem)
            val imgCheck = itemView.findViewById<ImageView>(R.id.imgCheckEstado)
            val txtNombre = itemView.findViewById<TextView>(R.id.txtNombreIngrediente)
            val txtDetalle = itemView.findViewById<TextView>(R.id.txtDetalleIngrediente)
            val badge = itemView.findViewById<TextView>(R.id.badgeEstadoCheck)

            val btnToggleReportarFaltante = itemView.findViewById<TextView>(R.id.btnToggleReportarFaltante)
            val panelFaltante = itemView.findViewById<LinearLayout>(R.id.panelFaltanteInputs)
            val edtCantidadRecibida = itemView.findViewById<EditText>(R.id.edtCantidadRecibidaItem)
            val txtUnidadMedida = itemView.findViewById<TextView>(R.id.txtUnidadMedidaItem)
            val txtFaltanteCalculado = itemView.findViewById<TextView>(R.id.txtFaltanteCalculadoItem)
            val edtMotivoFaltante = itemView.findViewById<EditText>(R.id.edtMotivoFaltanteItem)

            txtNombre.text = item.nombre
            txtUnidadMedida.text = item.unidad
            txtDetalle.text = "Despachado: ${formatearNumero(item.cantidad)} ${item.unidad} • ${item.descripcion}"

            val cantTexto = if (item.tieneNovedad) formatearNumero(item.cantidadRecibida) else formatearNumero(item.cantidad)
            edtCantidadRecibida.setText(cantTexto)
            edtMotivoFaltante.setText(item.motivoFaltante)

            fun actualizarVisualItem() {
                if (!item.verificado) {
                    imgCheck.setImageResource(R.drawable.ic_check_circle)
                    imgCheck.imageTintList = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                    badge.text = "Pendiente"
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                    badge.setTextColor(Color.parseColor("#94A3B8"))
                    card.strokeColor = Color.parseColor("#E2E8F0")
                    card.strokeWidth = dpToPx(1f)
                    panelFaltante.visibility = View.GONE
                } else if (item.tieneNovedad && item.cantidadFaltante > 0) {
                    imgCheck.setImageResource(R.drawable.ic_check_circle)
                    imgCheck.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
                    val faltanteStr = formatearNumero(item.cantidadFaltante)
                    badge.text = "Faltan $faltanteStr ${item.unidad}"
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    badge.setTextColor(Color.parseColor("#B45309"))
                    card.strokeColor = Color.parseColor("#F59E0B")
                    card.strokeWidth = dpToPx(1.5f)
                    panelFaltante.visibility = View.VISIBLE
                    btnToggleReportarFaltante.text = "✓ Marcar producto completo conforme"
                    btnToggleReportarFaltante.setTextColor(Color.parseColor("#15803D"))
                } else {
                    imgCheck.setImageResource(R.drawable.ic_check_circle)
                    imgCheck.imageTintList = ColorStateList.valueOf(Color.parseColor("#5F7625"))
                    badge.text = "Recibido Conforme"
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEF4DF"))
                    badge.setTextColor(Color.parseColor("#5F7625"))
                    card.strokeColor = Color.parseColor("#5F7625")
                    card.strokeWidth = dpToPx(1.5f)
                    panelFaltante.visibility = if (item.tieneNovedad) View.VISIBLE else View.GONE
                    btnToggleReportarFaltante.text = "⚠️ ¿Llegó incompleto o con faltante? Reportar"
                    btnToggleReportarFaltante.setTextColor(Color.parseColor("#D97706"))
                }

                val faltante = (item.cantidad - item.cantidadRecibida).coerceAtLeast(0.0)
                txtFaltanteCalculado.text = "Faltan: ${formatearNumero(faltante)} ${item.unidad}"
            }

            actualizarVisualItem()

            filaCabecera.setOnClickListener {
                item.verificado = !item.verificado
                actualizarVisualItem()
                actualizarConteoCheck()
            }

            imgCheck.setOnClickListener {
                item.verificado = !item.verificado
                actualizarVisualItem()
                actualizarConteoCheck()
            }

            btnToggleReportarFaltante.setOnClickListener {
                if (item.tieneNovedad) {
                    item.tieneNovedad = false
                    item.cantidadRecibida = item.cantidad
                    item.cantidadFaltante = 0.0
                    item.motivoFaltante = ""
                    edtCantidadRecibida.setText(formatearNumero(item.cantidad))
                    edtMotivoFaltante.setText("")
                } else {
                    item.tieneNovedad = true
                    item.verificado = true
                    panelFaltante.visibility = View.VISIBLE
                    val actual = edtCantidadRecibida.text.toString().toDoubleOrNull() ?: item.cantidad
                    item.cantidadRecibida = actual
                    item.cantidadFaltante = (item.cantidad - actual).coerceAtLeast(0.0)
                }
                actualizarVisualItem()
                actualizarConteoCheck()
            }

            edtCantidadRecibida.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val valor = s?.toString()?.toDoubleOrNull() ?: 0.0
                    item.cantidadRecibida = valor
                    val faltante = (item.cantidad - valor).coerceAtLeast(0.0)
                    item.cantidadFaltante = faltante
                    val faltanteStr = formatearNumero(faltante)
                    txtFaltanteCalculado.text = "Faltan: $faltanteStr ${item.unidad}"

                    if (item.verificado && item.tieneNovedad) {
                        if (faltante > 0) {
                            badge.text = "Faltan $faltanteStr ${item.unidad}"
                            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                            badge.setTextColor(Color.parseColor("#B45309"))
                            card.strokeColor = Color.parseColor("#F59E0B")
                        } else {
                            badge.text = "Recibido Completo"
                            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEF4DF"))
                            badge.setTextColor(Color.parseColor("#5F7625"))
                            card.strokeColor = Color.parseColor("#5F7625")
                        }
                    }
                }
            })

            edtMotivoFaltante.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    item.motivoFaltante = s?.toString()?.trim().orEmpty()
                }
            })

            contenedor.addView(itemView)
        }

        actualizarConteoCheck()

        binding.btnMarcarTodos.setOnClickListener {
            val todosMarcados = listaIngredientesDespacho.all { it.verificado }
            listaIngredientesDespacho.forEach { it.verificado = !todosMarcados }
            renderizarIngredientes()
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
        val marcados = listaIngredientesDespacho.count { it.verificado }
        binding.txtConteoCheck.text = "$marcados de $total alimentos verificados conforme"
        if (marcados == total) {
            binding.btnMarcarTodos.text = "Desmarcar todos"
        } else {
            binding.btnMarcarTodos.text = "Marcar todos"
        }
    }

    private fun configurarLienzoFirma() {
        binding.signatureView.setOnTouchListener { _, _ ->
            binding.txtPistaFirma.visibility = View.GONE
            false
        }

        binding.btnBorrarFirma.setOnClickListener {
            binding.signatureView.clear()
            binding.txtPistaFirma.visibility = View.VISIBLE
        }
    }

    private fun configurarBotonesAccion() {
        // BOTÓN RECHAZAR ENTREGA
        binding.btnRechazarEntrega.setOnClickListener {
            mostrarDialogoRechazo()
        }

        // BOTÓN ACEPTAR ENTREGA Y NOTIFICAR
        binding.btnAceptarEntrega.setOnClickListener {
            confirmarYAceptarEntrega()
        }
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
        lifecycleScope.launch {
            try {
                // Confirmar rechazo en el backend para cada ítem en tránsito
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
        if (!binding.signatureView.hasSignature()) {
            Toast.makeText(this, "Por favor estampa tu firma digital en el recuadro antes de aceptar.", Toast.LENGTH_LONG).show()
            return
        }

        val faltantesConNovedad = listaIngredientesDespacho.filter { it.tieneNovedad && it.cantidadFaltante > 0 }
        val noVerificados = listaIngredientesDespacho.filter { !it.verificado }

        if (faltantesConNovedad.isNotEmpty()) {
            val listaReporte = faltantesConNovedad.joinToString("\n") {
                val fStr = formatearNumero(it.cantidadFaltante)
                val recStr = formatearNumero(it.cantidadRecibida)
                val mot = if (it.motivoFaltante.isNotBlank()) " - Motivo: ${it.motivoFaltante}" else ""
                "• ${it.nombre}: Recibidos $recStr ${it.unidad} (Faltaron $fStr ${it.unidad}$mot)"
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Confirmar Entrega con Faltantes")
                .setMessage("Vas a firmar la entrega reportando los siguientes faltantes:\n\n$listaReporte\n\nEl supervisor recibirá esta notificación con el reporte exacto y se sumará lo recibido al inventario de la sede. ¿Deseas firmar y enviar?")
                .setPositiveButton("Sí, firmar y enviar") { _, _ ->
                    ejecutarAceptacionFinal()
                }
                .setNegativeButton("Revisar", null)
                .show()
            return
        }

        if (noVerificados.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Alimentos sin chulear")
                .setMessage("Hay ${noVerificados.size} alimentos sin verificar. ¿Deseas continuar y registrar la entrega?")
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
        binding.btnAceptarEntrega.isEnabled = false
        binding.btnAceptarEntrega.text = "Enviando..."

        val obs = binding.edtObservacionesEntrega.text.toString().trim()
            .ifEmpty { "Víveres verificados y acta firmada en sede escolar." }
        val sedeNombre = sessionManager.getSedeNombre() ?: "Sede"

        val faltantesConNovedad = listaIngredientesDespacho.filter { it.tieneNovedad && it.cantidadFaltante > 0 }
        val tieneNovedades = faltantesConNovedad.isNotEmpty()

        val reporteFaltantesTexto = if (tieneNovedades) {
            faltantesConNovedad.joinToString("\n") {
                val fStr = formatearNumero(it.cantidadFaltante)
                val recStr = formatearNumero(it.cantidadRecibida)
                val mot = if (it.motivoFaltante.isNotBlank()) " - Motivo: ${it.motivoFaltante}" else ""
                "• ${it.nombre}: Recibido $recStr ${it.unidad} (Faltaron $fStr ${it.unidad}$mot)"
            }
        } else ""

        lifecycleScope.launch {
            try {
                // 1. Confirmar recepción en el backend (actualiza estado a RECIBIDA o CON_NOVEDAD en base de datos y suma al Inventario de la sede)
                for (item in listaIngredientesDespacho) {
                    item.idEntrega?.let { idEnt ->
                        try {
                            val cantRecibida = if (!item.verificado) 0.0 else (if (item.tieneNovedad) item.cantidadRecibida else item.cantidad)
                            val cantRechazada = if (!item.verificado) item.cantidad else (if (item.tieneNovedad) item.cantidadFaltante else 0.0)
                            val motivoItem = if (!item.verificado) {
                                "No verificado por la sede: $obs"
                            } else if (item.tieneNovedad && item.cantidadFaltante > 0) {
                                item.motivoFaltante.ifEmpty { "Producto recibido incompleto / faltante en sede" }
                            } else {
                                ""
                            }

                            RetrofitClient.apiService.confirmarRecepcionEntrega(
                                idEnt,
                                ConfirmarRecepcionRequest(
                                    cantidadRecibida = cantRecibida,
                                    cantidadRechazada = cantRechazada,
                                    motivoRechazo = motivoItem
                                )
                            )
                        } catch (_: Exception) {}
                    }
                }

                // 2. Notificación al Supervisor
                val titSup = if (tieneNovedades) {
                    "⚠️ Entrega Recibida CON NOVEDAD en $sedeNombre"
                } else {
                    "✅ Entrega Aceptada Conforme en $sedeNombre"
                }

                val msgSup = if (tieneNovedades) {
                    "La Jefa de Manipuladoras ha firmado la remisión $numeroRemision en $sedeNombre con reporte de faltantes:\n\n$reporteFaltantesTexto\n\nFirma digital registrada. Observaciones: $obs."
                } else {
                    "La remisión $numeroRemision ha sido verificada completa, firmada digitalmente y aceptada conforme por la Jefa de manipuladoras en $sedeNombre. El inventario fue actualizado."
                }

                try {
                    RetrofitClient.apiService.enviarNotificacion(
                        titSup.toRequestBody("text/plain".toMediaTypeOrNull()),
                        msgSup.toRequestBody("text/plain".toMediaTypeOrNull()),
                        null
                    )
                } catch (_: Exception) {}

                // 3. Notificación al Administrador Institucional
                val titAdmin = "🔔 Recepción firmada en sede: $sedeNombre"
                val msgAdmin = if (tieneNovedades) {
                    "Acta de recepción firmada para remisión $numeroRemision con reporte de faltantes en $sedeNombre:\n$reporteFaltantesTexto"
                } else {
                    "La Jefa de Manipuladoras ha firmado la remisión $numeroRemision en $sedeNombre sin novedades. Ingreso acreditado al inventario."
                }
                try {
                    RetrofitClient.apiService.enviarNotificacion(
                        titAdmin.toRequestBody("text/plain".toMediaTypeOrNull()),
                        msgAdmin.toRequestBody("text/plain".toMediaTypeOrNull()),
                        null
                    )
                } catch (_: Exception) {}

                // 4. Cargar cantidades verificadas al inventario de la sede
                for (item in listaIngredientesDespacho.filter { it.verificado }) {
                    val cantFinal = if (item.tieneNovedad) item.cantidadRecibida else item.cantidad
                    if (cantFinal > 0) {
                        InventarioManager.guardarCantidad(
                            alimento = item.nombre,
                            cantidad = cantFinal,
                            jornada = "MANANA"
                        )
                    }
                }

                // 5. Agregar notificación local en la app para la Jefa y su equipo
                NotificacionesJefaManager.agregarNotificacionSede(
                    this@RegistrarEntregaActivity,
                    if (tieneNovedades) "⚠️ Entrega recibida con novedades ($numeroRemision)" else "✅ Entrega recibida conforme ($numeroRemision)",
                    if (tieneNovedades) "Se aceptó la remisión con reporte de faltantes:\n$reporteFaltantesTexto" else "Remisión $numeroRemision aceptada e ingresada al inventario de la sede.",
                    "Inventario"
                )

            } catch (_: Exception) {
                // Sigue adelante si el backend no responde
            }

            val mensajeFinal = if (tieneNovedades) {
                "Se ha enviado el acta firmada con novedades.\n\n• El Supervisor ($nombreSupervisor) fue notificado del reporte de faltantes:\n$reporteFaltantesTexto\n\n• Las cantidades efectivamente recibidas ya se agregaron al inventario de tu sede."
            } else {
                "Se ha enviado el comprobante firmado digitalmente.\n\n• El Supervisor ($nombreSupervisor) recibió la confirmación conforme.\n• Todos los productos despachados fueron agregados al inventario de la sede."
            }

            MaterialAlertDialogBuilder(this@RegistrarEntregaActivity)
                .setTitle(if (tieneNovedades) "¡Entrega Aceptada con Novedades!" else "¡Entrega Aceptada Conforme!")
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

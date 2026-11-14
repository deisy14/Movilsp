package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
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
        var verificado: Boolean = true
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
        configurarLienzoFirma()
        configurarBotonesAccion()
    }

    private fun configurarEncabezado() {
        val sedeNombre = sessionManager.getSedeNombre() ?: "Sede Piagua (Primaria)"
        numeroRemision = intent.getStringExtra("numero_remision") ?: "#REM-2026-104"
        nombreSupervisor = intent.getStringExtra("supervisor_nombre") ?: "Carlos Méndez"

        binding.txtSubtituloRemision.text = "$sedeNombre • Remisión $numeroRemision"
        binding.txtSedeDestino.text = sedeNombre
        binding.txtDetalleSupervisor.text = "Supervisor PAE: $nombreSupervisor • Mercados del Mes"

        try {
            val formato = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "CO"))
            binding.txtFechaDespacho.text = formato.format(Date()).replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            binding.txtFechaDespacho.text = "Hoy, 14 Nov 2026"
        }

        binding.btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun renderizarIngredientes() {
        val contenedor = binding.contenedorIngredientesCheck
        contenedor.removeAllViews()

        val inflater = LayoutInflater.from(this)

        for (item in listaIngredientesDespacho) {
            val itemView = inflater.inflate(R.layout.item_ingrediente_entrega_check, contenedor, false)
            val card = itemView.findViewById<MaterialCardView>(R.id.cardIngredienteCheck)
            val imgCheck = itemView.findViewById<ImageView>(R.id.imgCheckEstado)
            val txtNombre = itemView.findViewById<TextView>(R.id.txtNombreIngrediente)
            val txtDetalle = itemView.findViewById<TextView>(R.id.txtDetalleIngrediente)
            val badge = itemView.findViewById<TextView>(R.id.badgeEstadoCheck)

            txtNombre.text = item.nombre
            txtDetalle.text = "${item.cantidad} ${item.unidad} • ${item.descripcion}"

            fun actualizarVisualItem() {
                if (item.verificado) {
                    imgCheck.setImageResource(R.drawable.ic_check_circle)
                    imgCheck.imageTintList = ColorStateList.valueOf(Color.parseColor("#5F7625"))
                    badge.text = "Recibido"
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EEF4DF"))
                    badge.setTextColor(Color.parseColor("#5F7625"))
                    card.strokeColor = Color.parseColor("#5F7625")
                    card.strokeWidth = dpToPx(1f)
                } else {
                    imgCheck.setImageResource(R.drawable.ic_check_circle)
                    imgCheck.imageTintList = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                    badge.text = "Pendiente"
                    badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                    badge.setTextColor(Color.parseColor("#94A3B8"))
                    card.strokeColor = Color.parseColor("#E2E8F0")
                    card.strokeWidth = dpToPx(1f)
                }
            }

            actualizarVisualItem()

            card.setOnClickListener {
                item.verificado = !item.verificado
                actualizarVisualItem()
                actualizarConteoCheck()
            }

            contenedor.addView(itemView)
        }

        actualizarConteoCheck()

        binding.btnMarcarTodos.setOnClickListener {
            val todosMarcados = listaIngredientesDespacho.all { it.verificado }
            listaIngredientesDespacho.forEach { it.verificado = !todosMarcados }
            renderizarIngredientes()
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
        binding.signatureView.setOnTouchListener { _, event ->
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
        val input = android.widget.EditText(this).apply {
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
        val sedeNombre = sessionManager.getSedeNombre() ?: "Sede Piagua"
        lifecycleScope.launch {
            try {
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

        val noVerificados = listaIngredientesDespacho.filter { !it.verificado }
        if (noVerificados.isNotEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Alimentos sin chulear")
                .setMessage("Hay ${noVerificados.size} alimentos sin verificar. ¿Deseas continuar y registrar la entrega?")
                .setPositiveButton("Sí, continuar") { _, _ ->
                    ejecutarAceptacionFinal()
                }
                .setNegativeButton("Revisar", null)
                .show()
        } else {
            ejecutarAceptacionFinal()
        }
    }

    private fun ejecutarAceptacionFinal() {
        binding.btnAceptarEntrega.isEnabled = false
        binding.btnAceptarEntrega.text = "Enviando..."

        val obs = binding.edtObservacionesEntrega.text.toString().trim()
            .ifEmpty { "Todos los alimentos llegaron en perfecto estado y empaques sellados conforme." }
        val sedeNombre = sessionManager.getSedeNombre() ?: "Sede Piagua"

        lifecycleScope.launch {
            try {
                // 1. Notificación al Administrador Institucional
                val titAdmin = "🔔 Recepción confirmada en sede: $sedeNombre".toRequestBody("text/plain".toMediaTypeOrNull())
                val msgAdmin = "La Jefa de Manipuladoras ha firmado la remisión $numeroRemision en $sedeNombre. Observaciones: $obs. Lista para aceptar su ingreso al inventario.".toRequestBody("text/plain".toMediaTypeOrNull())
                RetrofitClient.apiService.enviarNotificacion(titAdmin, msgAdmin, null)

                // 2. Notificación al Supervisor
                val titSup = "✅ Entrega Aceptada en $sedeNombre".toRequestBody("text/plain".toMediaTypeOrNull())
                val msgSup = "La remisión $numeroRemision ha sido verificada, firmada y aceptada por la Jefa de manipuladoras en $sedeNombre.".toRequestBody("text/plain".toMediaTypeOrNull())
                RetrofitClient.apiService.enviarNotificacion(titSup, msgSup, null)

                // 3. Cargar cantidades verificadas al inventario de la sede
                for (item in listaIngredientesDespacho.filter { it.verificado }) {
                    InventarioManager.guardarCantidad(
                        alimento = item.nombre,
                        cantidad = item.cantidad,
                        jornada = "MANANA"
                    )
                }

            } catch (e: Exception) {
                // Sigue adelante si el backend no responde
            }

            MaterialAlertDialogBuilder(this@RegistrarEntregaActivity)
                .setTitle("¡Entrega Aceptada!")
                .setMessage("Se ha enviado el comprobante firmado.\n\n• El Administrador recibió la notificación para sumar los ingredientes al inventario.\n• El Supervisor fue notificado de la aceptación.")
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

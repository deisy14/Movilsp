package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityTodosLosPlatosBinding
import com.example.movilmanupuladora.databinding.DialogDetallePlatoBinding
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TodosLosPlatosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTodosLosPlatosBinding
    private val menuRepository = MenuRepository(RetrofitClient.apiService)
    private var fechaSeleccionada: String = ""

    private val platosRespaldo = MenuPaeCatalog.menus.map {
        PlatoResponse(idPlato = it.id, nombrePlato = it.nombre, componente = it.componente)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTodosLosPlatosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fechaSeleccionada = intent.getStringExtra("fecha_seleccionada") ?: run {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnVolver.setOnClickListener { finish() }

        cargarPlatosBackend()
    }

    private fun cargarPlatosBackend() {
        // Mostrar el catálogo oficial completo del PAE con los nombres e imágenes actualizados
        mostrarLista(platosRespaldo)

        lifecycleScope.launch {
            try {
                val res = menuRepository.obtenerPlatos()
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    val lista = res.body()!!
                    // Solo si el backend devuelve el catálogo completo de 15 o más platos
                    if (lista.size >= 15) {
                        mostrarLista(lista)
                    }
                }
            } catch (_: Exception) {
                // Mantiene el catálogo oficial del PAE
            }
        }
    }

    private fun mostrarLista(platos: List<PlatoResponse>) {
        val adapter = PlatoCatalogoAdapter(platos) { plato ->
            mostrarDialogoDetallePlato(plato)
        }
        binding.recyclerTodosLosPlatos.adapter = adapter
        binding.recyclerTodosLosPlatos.layoutManager = LinearLayoutManager(this)
    }

    private fun mostrarDialogoDetallePlato(plato: PlatoResponse) {
        val dialogBinding = DialogDetallePlatoBinding.inflate(layoutInflater)
        val sessionMgr = SessionManager(this)

        val nombrePlatoActual = plato.nombrePlato ?: "Plato PAE"
        dialogBinding.tvTituloPlatoModal.text = nombrePlatoActual
        dialogBinding.tvSeccionModal.text = "Menú Disponible PAE"
        dialogBinding.tvComponenteModal.text = "Componente: ${plato.componente ?: "Sopas, Sazón y Proteína"}"
        dialogBinding.tvPorcionModal.text = "100g aprox. por ración"
        dialogBinding.tvTotalPrepararModal.text = "120 porciones"
        dialogBinding.tvBadgeEstadoModal.text = "Disponible"

        val imgRes = if (plato.idPlato in 1..20) {
            MenuPaeCatalog.obtenerMenuPorId(plato.idPlato).imagenRes
        } else {
            MenuPaeCatalog.obtenerImagenPorNombre(plato.nombrePlato ?: plato.componente)
        }
        dialogBinding.imgPlatoModal.setImageResource(imgRes)

        // Ocultar botón de Iniciar Preparación según HU
        dialogBinding.btnOtroAleatorioModal.visibility = View.GONE
        dialogBinding.layoutSeleccionServicioModal.visibility = View.VISIBLE

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCerrarModal.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnVerComponentesModal.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                putExtra("nombre_menu", plato.nombrePlato)
                putExtra("id_plato", plato.idPlato)
            }
            startActivity(intent)
        }

        // Calcular platos servidos el día inmediatamente anterior
        val calAyer = Calendar.getInstance().apply {
            try {
                time = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fechaSeleccionada) ?: Date()
            } catch (_: Exception) { }
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val fechaAyer = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calAyer.time)
        val platosAyer = listOfNotNull(
            sessionMgr.getPlatoDiaServicio(this, fechaAyer, "Desayuno"),
            sessionMgr.getPlatoDiaServicio(this, fechaAyer, "Almuerzo"),
            sessionMgr.getPlatoDiaServicio(this, fechaAyer, "Merienda")
        ).toSet()

        // Configurar botones de servicio (Desayuno, Almuerzo, Merienda)
        fun actualizarBotonesServicios() {
            val platoDesayuno = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, "Desayuno")
                ?: sessionMgr.getPlatoServicio(this, "Desayuno")
            val platoAlmuerzo = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, "Almuerzo")
                ?: sessionMgr.getPlatoServicio(this, "Almuerzo")
            val platoMerienda = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, "Merienda")
                ?: sessionMgr.getPlatoServicio(this, "Merienda")

            val esDesayuno = platoDesayuno != null && platoDesayuno.equals(nombrePlatoActual, ignoreCase = true)
            val esAlmuerzo = platoAlmuerzo != null && platoAlmuerzo.equals(nombrePlatoActual, ignoreCase = true)
            val esMerienda = platoMerienda != null && platoMerienda.equals(nombrePlatoActual, ignoreCase = true)
            val esDeAyer = platosAyer.contains(nombrePlatoActual)

            fun pintarBoton(
                btn: MaterialButton,
                servicioNombre: String,
                esEste: Boolean,
                platoAsignado: String?
            ) {
                if (esEste) {
                    btn.text = "✓ $servicioNombre"
                    btn.setBackgroundColor(Color.parseColor("#176B35"))
                    btn.setTextColor(Color.WHITE)
                    btn.strokeWidth = 0
                } else if (esDeAyer) {
                    btn.text = "$servicioNombre"
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(Color.parseColor("#94A3B8"))
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                    btn.strokeWidth = (1 * resources.displayMetrics.density).toInt()
                } else if (!platoAsignado.isNullOrBlank()) {
                    btn.text = "$servicioNombre*"
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(Color.parseColor("#64748B"))
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                    btn.strokeWidth = (1.5 * resources.displayMetrics.density).toInt()
                } else {
                    btn.text = servicioNombre
                    btn.setBackgroundColor(Color.TRANSPARENT)
                    btn.setTextColor(Color.parseColor("#176B35"))
                    btn.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#176B35"))
                    btn.strokeWidth = (1.5 * resources.displayMetrics.density).toInt()
                }
            }

            pintarBoton(dialogBinding.btnAsignarDesayunoModal, "Desayuno", esDesayuno, platoDesayuno)
            pintarBoton(dialogBinding.btnAsignarAlmuerzoModal, "Almuerzo", esAlmuerzo, platoAlmuerzo)
            pintarBoton(dialogBinding.btnAsignarMeriendaModal, "Merienda", esMerienda, platoMerienda)

            // Texto informativo del estado
            when {
                esDeAyer -> {
                    dialogBinding.tvEstadoAsignacionPlato.text = "⚠️ Este plato fue servido el día anterior. No se puede repetir en días consecutivos."
                    dialogBinding.tvBadgeEstadoModal.text = "Servido ayer"
                }
                esDesayuno -> {
                    dialogBinding.tvEstadoAsignacionPlato.text = "✓ Asignado como Desayuno. Toca para desmarcarlo o cambiarlo."
                    dialogBinding.tvBadgeEstadoModal.text = "En Desayuno"
                }
                esAlmuerzo -> {
                    dialogBinding.tvEstadoAsignacionPlato.text = "✓ Asignado como Almuerzo. Toca para desmarcarlo o cambiarlo."
                    dialogBinding.tvBadgeEstadoModal.text = "En Almuerzo"
                }
                esMerienda -> {
                    dialogBinding.tvEstadoAsignacionPlato.text = "✓ Asignado como Merienda. Toca para desmarcarlo o cambiarlo."
                    dialogBinding.tvBadgeEstadoModal.text = "En Merienda"
                }
                else -> {
                    val disponibles = mutableListOf<String>()
                    if (platoDesayuno.isNullOrBlank()) disponibles.add("Desayuno")
                    if (platoAlmuerzo.isNullOrBlank()) disponibles.add("Almuerzo")
                    if (platoMerienda.isNullOrBlank()) disponibles.add("Merienda")

                    if (disponibles.isNotEmpty()) {
                        dialogBinding.tvEstadoAsignacionPlato.text = "Servicios disponibles: ${disponibles.joinToString(", ")}"
                    } else {
                        dialogBinding.tvEstadoAsignacionPlato.text = "Toca un servicio para reemplazar su plato actual."
                    }
                }
            }
        }

        actualizarBotonesServicios()

        fun procesarClickServicio(servicio: String) {
            // 1. Validar si ya fue servido ayer
            if (platosAyer.contains(nombrePlatoActual)) {
                val nombreAyer = SimpleDateFormat("EEEE d", Locale.forLanguageTag("es-CO")).format(calAyer.time)
                MaterialAlertDialogBuilder(this)
                    .setTitle("Plato no disponible para este día")
                    .setMessage("El plato \"$nombrePlatoActual\" ya fue seleccionado para el día anterior ($nombreAyer).\n\nPor normatividad PAE de rotación de menú, no se puede repetir en días consecutivos.")
                    .setPositiveButton("Entendido", null)
                    .show()
                return
            }

            val platoServicioActual = sessionMgr.getPlatoDiaServicio(this, fechaSeleccionada, servicio)
                ?: sessionMgr.getPlatoServicio(this, servicio)

            // 2. Si este plato YA está asignado a este servicio -> Confirmar si desea quitarlo
            if (platoServicioActual != null && platoServicioActual.equals(nombrePlatoActual, ignoreCase = true)) {
                val dialogBinding = com.example.movilmanupuladora.databinding.DialogQuitarPlatoBinding.inflate(layoutInflater)
                dialogBinding.txtMensajeQuitarPlato.text = "¿Estás segura de que deseas quitar \"$nombrePlatoActual\" del servicio de $servicio?"

                val confirmDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setView(dialogBinding.root)
                    .create()

                confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

                dialogBinding.btnSiQuitar.setOnClickListener {
                    confirmDialog.dismiss()
                    lifecycleScope.launch {
                        PreparacionSyncManager.desasignarPlatoServicio(
                            context = this@TodosLosPlatosActivity,
                            servicio = servicio,
                            fecha = fechaSeleccionada
                        )
                    }
                    Toast.makeText(this@TodosLosPlatosActivity, "✓ Plato quitado de $servicio", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }

                dialogBinding.btnCancelarQuitar.setOnClickListener {
                    confirmDialog.dismiss()
                }

                confirmDialog.show()
                return
            }

            // 3. Si el servicio ya tiene OTRO plato asignado -> Alerta de confirmación para cambiar
            if (!platoServicioActual.isNullOrBlank() && !platoServicioActual.equals(nombrePlatoActual, ignoreCase = true)) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Servicio ya seleccionado")
                    .setMessage("El servicio de $servicio ya tiene asignado \"$platoServicioActual\". ¿Deseas cambiar esta selección por \"$nombrePlatoActual\"?")
                    .setPositiveButton("Sí, cambiar") { _, _ ->
                        lifecycleScope.launch {
                            PreparacionSyncManager.guardarPlatoServicio(
                                context = this@TodosLosPlatosActivity,
                                servicio = servicio,
                                nombrePlato = nombrePlatoActual,
                                idPlato = plato.idPlato,
                                fecha = fechaSeleccionada
                            )
                        }
                        Toast.makeText(this@TodosLosPlatosActivity, "✓ Plato asignado a $servicio", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                return
            }

            // 4. Asignación directa si el servicio estaba libre
            lifecycleScope.launch {
                PreparacionSyncManager.guardarPlatoServicio(
                    context = this@TodosLosPlatosActivity,
                    servicio = servicio,
                    nombrePlato = nombrePlatoActual,
                    idPlato = plato.idPlato,
                    fecha = fechaSeleccionada
                )
            }
            Toast.makeText(this, "✓ Plato asignado a $servicio", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialogBinding.btnAsignarDesayunoModal.setOnClickListener {
            procesarClickServicio("Desayuno")
        }

        dialogBinding.btnAsignarAlmuerzoModal.setOnClickListener {
            procesarClickServicio("Almuerzo")
        }

        dialogBinding.btnAsignarMeriendaModal.setOnClickListener {
            procesarClickServicio("Merienda")
        }

        dialog.show()
    }
}
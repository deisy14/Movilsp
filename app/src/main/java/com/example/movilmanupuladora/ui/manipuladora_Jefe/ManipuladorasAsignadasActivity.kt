package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.databinding.ActivityManipuladorasAsignadasBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.IniciarPreparacionActivity
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.Normalizer

class ManipuladorasAsignadasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManipuladorasAsignadasBinding

    private val manipuladoras = mutableListOf<ManipuladoraLocal>()

    private var jornada = "Mañana"
    private var nombreMenu = "Arroz con Pollo Criollo"
    private var idPlato = -1

    private var componenteJefa: String? = null
    private var nombreJefa: String = "Isa Pérez"
    private var correoJefa: String = ""

    data class ManipuladoraLocal(
        val nombre: String,
        val correo: String = "",
        val rol: String = "Manipuladora de alimentos",
        var estado: String = "Asignada",
        var componenteAsignado: String? = null,
        val idUsuario: Int = -1
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityManipuladorasAsignadasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        obtenerDatos()

        // Sincronizar asignaciones y cargar manipuladoras reales desde backend
        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@ManipuladorasAsignadasActivity)
                ManipuladorasManager.cargarDesdeBackend()
            } catch (_: Exception) { }
            cargarManipuladoras()
            configurarEventos()
            actualizarInterfaz()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.PreparacionSyncManager.sincronizarDesdeBackend(this@ManipuladorasAsignadasActivity)
            } catch (_: Exception) { }
            cargarManipuladoras()
            actualizarInterfaz()
        }
    }

    private fun obtenerDatos() {
        jornada = intent.getStringExtra("jornada") ?: "Mañana"
        nombreMenu = intent.getStringExtra("nombre_menu") ?: "Arroz con Pollo Criollo"
        idPlato = intent.getIntExtra("id_plato", -1)

        val sessionMgr = SessionManager(this)
        nombreJefa = sessionMgr.getUserName() ?: "Isa Pérez"
        correoJefa = sessionMgr.getUserEmail() ?: ""

        binding.txtJornada.text = "Jornada: $jornada"
        binding.txtNombrePlatoAsignar.text = nombreMenu

        val imgRes = InicioJefaActivity.obtenerImagenPlatoPorNombre(nombreMenu)
        binding.imgPlatoAsignar.setImageResource(imgRes)

        val componentes = obtenerComponentesPlato(nombreMenu)
        binding.txtComponentesDisponibles.text = "• " + componentes.joinToString("\n• ")
    }

    private fun obtenerComponentesPlato(nombre: String): List<String> {
        return MenuPaeCatalog.obtenerComponentesPorNombre(nombre)
    }

    private fun cargarManipuladoras() {
        manipuladoras.clear()

        val prefs = getSharedPreferences("ManipuladoraJefe", MODE_PRIVATE)

        // Cargar componente asignado a la Jefa de Cocina
        componenteJefa = prefs.getString("comp_jefa", null)

        // Obtener manipuladoras reales del ManipuladorasManager
        val listaReal = ManipuladorasManager.obtenerManipuladoras().filter { it.activa }

        val componentesOcupados = mutableSetOf<String>()
        if (!componenteJefa.isNullOrBlank()) {
            componentesOcupados.add(componenteJefa!!.lowercase().trim())
        }

        for (m in listaReal) {
            // Buscar componente guardado por correo (preferido) o por nombre
            val keyCorreo = "comp_correo_" + m.correo.replace("@", "_").replace(".", "_").lowercase()
            val keyNombre = "comp_" + m.nombre.replace(" ", "_").lowercase()
            val guardado = prefs.getString(keyCorreo, null) ?: prefs.getString(keyNombre, null)

            val componenteValido = if (!guardado.isNullOrBlank() && !componentesOcupados.contains(guardado.lowercase().trim())) {
                componentesOcupados.add(guardado.lowercase().trim())
                guardado
            } else {
                null
            }

            manipuladoras.add(
                ManipuladoraLocal(
                    nombre = m.nombre,
                    correo = m.correo,
                    rol = "Manipuladora activa",
                    estado = "Asignada",
                    componenteAsignado = componenteValido,
                    idUsuario = if (m.idUsuario > 0) m.idUsuario else m.id
                )
            )
        }
    }

    private fun configurarEventos() {
        binding.btnCerrar.setOnClickListener {
            finish()
        }

        binding.btnAgregarManipuladora.setOnClickListener {
            mostrarDialogoAgregar()
        }

        binding.btnConfirmarAsignacion.setOnClickListener {
            confirmarAsignacion()
        }

        binding.btnCancelar.setOnClickListener {
            confirmarCancelar()
        }

        // Clic en la tarjeta de la Jefa de Cocina
        binding.cardJefa.setOnClickListener {
            mostrarOpcionesJefa()
        }

        binding.cardManipuladora1.setOnClickListener {
            if (manipuladoras.isNotEmpty()) {
                mostrarOpcionesManipuladora(manipuladoras[0])
            }
        }

        binding.cardManipuladora2.setOnClickListener {
            if (manipuladoras.size > 1) {
                mostrarOpcionesManipuladora(manipuladoras[1])
            }
        }

        binding.cardManipuladora3.setOnClickListener {
            if (manipuladoras.size > 2) {
                mostrarOpcionesManipuladora(manipuladoras[2])
            }
        }
    }

    private fun actualizarInterfaz() {
        val total = manipuladoras.size
        val asignadasConComp = manipuladoras.count { !it.componenteAsignado.isNullOrBlank() }
        val jefaAsignada = if (!componenteJefa.isNullOrBlank()) 1 else 0

        binding.txtCantidadManipuladoras.text = "$total manipuladoras activas ($asignadasConComp con componente) + Jefa de Cocina ($jefaAsignada componente)"

        // Actualizar tarjeta de la Jefa
        actualizarTarjetaJefa()

        // Actualizar tarjetas de manipuladoras
        actualizarTarjeta(0)
        actualizarTarjeta(1)
        actualizarTarjeta(2)
    }

    private fun actualizarTarjetaJefa() {
        binding.cardJefa.visibility = View.VISIBLE
        binding.txtNombreJefa.text = "$nombreJefa (Jefa de Cocina · Tú)"
        binding.txtRolJefa.text = "Coordinación PAE (Toca para opciones de preparación)"

        if (!componenteJefa.isNullOrBlank()) {
            binding.txtComponenteJefa.text = "Tu componente: $componenteJefa"
            binding.txtComponenteJefa.setBackgroundResource(R.drawable.bg_pill_green)
            binding.txtComponenteJefa.setTextColor(Color.parseColor("#176B35"))

            val estadoJ = PreparacionEstadoManager.obtenerEstado(this, nombreJefa, componenteJefa, correoJefa)
            configurarBadgeEstado(binding.txtEstadoJefa, estadoJ)
        } else {
            binding.txtComponenteJefa.text = "Toca para asignarte un componente"
            binding.txtComponenteJefa.setBackgroundResource(R.drawable.bg_pill_yellow)
            binding.txtComponenteJefa.setTextColor(Color.parseColor("#855B00"))

            binding.txtEstadoJefa.text = "Asignar ›"
            binding.txtEstadoJefa.setBackgroundResource(R.drawable.bg_estado_disponible)
            binding.txtEstadoJefa.setTextColor(Color.parseColor("#1B3520"))
        }
    }

    private fun actualizarTarjeta(posicion: Int) {
        when (posicion) {
            0 -> {
                if (manipuladoras.isNotEmpty()) {
                    val persona = manipuladoras[0]
                    binding.cardManipuladora1.visibility = View.VISIBLE
                    binding.txtNombreManipuladora1.text = persona.nombre
                    binding.txtRolManipuladora1.text = persona.rol

                    if (!persona.componenteAsignado.isNullOrBlank()) {
                        binding.txtComponenteManipuladora1.text = "Componente: ${persona.componenteAsignado}"
                        binding.txtComponenteManipuladora1.setBackgroundResource(R.drawable.bg_pill_green)
                        binding.txtComponenteManipuladora1.setTextColor(Color.parseColor("#176B35"))

                        val estadoPrep = PreparacionEstadoManager.obtenerEstado(this, persona.nombre, persona.componenteAsignado, persona.correo)
                        configurarBadgeEstado(binding.txtEstadoManipuladora1, estadoPrep)
                    } else {
                        binding.txtComponenteManipuladora1.text = "Toca para asignar componente"
                        binding.txtComponenteManipuladora1.setBackgroundResource(R.drawable.bg_pill_yellow)
                        binding.txtComponenteManipuladora1.setTextColor(Color.parseColor("#855B00"))
                        binding.txtEstadoManipuladora1.text = "Asignar ›"
                        binding.txtEstadoManipuladora1.setBackgroundResource(R.drawable.bg_estado_disponible)
                        binding.txtEstadoManipuladora1.setTextColor(Color.parseColor("#1B3520"))
                    }
                } else {
                    binding.cardManipuladora1.visibility = View.GONE
                }
            }
            1 -> {
                if (manipuladoras.size > 1) {
                    val persona = manipuladoras[1]
                    binding.cardManipuladora2.visibility = View.VISIBLE
                    binding.txtNombreManipuladora2.text = persona.nombre
                    binding.txtRolManipuladora2.text = persona.rol

                    if (!persona.componenteAsignado.isNullOrBlank()) {
                        binding.txtComponenteManipuladora2.text = "Componente: ${persona.componenteAsignado}"
                        binding.txtComponenteManipuladora2.setBackgroundResource(R.drawable.bg_pill_green)
                        binding.txtComponenteManipuladora2.setTextColor(Color.parseColor("#176B35"))

                        val estadoPrep = PreparacionEstadoManager.obtenerEstado(this, persona.nombre, persona.componenteAsignado, persona.correo)
                        configurarBadgeEstado(binding.txtEstadoManipuladora2, estadoPrep)
                    } else {
                        binding.txtComponenteManipuladora2.text = "Toca para asignar componente"
                        binding.txtComponenteManipuladora2.setBackgroundResource(R.drawable.bg_pill_yellow)
                        binding.txtComponenteManipuladora2.setTextColor(Color.parseColor("#855B00"))
                        binding.txtEstadoManipuladora2.text = "Asignar ›"
                        binding.txtEstadoManipuladora2.setBackgroundResource(R.drawable.bg_estado_disponible)
                        binding.txtEstadoManipuladora2.setTextColor(Color.parseColor("#1B3520"))
                    }
                } else {
                    binding.cardManipuladora2.visibility = View.GONE
                }
            }
            2 -> {
                if (manipuladoras.size > 2) {
                    val persona = manipuladoras[2]
                    binding.cardManipuladora3.visibility = View.VISIBLE
                    binding.txtNombreManipuladora3.text = persona.nombre
                    binding.txtRolManipuladora3.text = persona.rol

                    if (!persona.componenteAsignado.isNullOrBlank()) {
                        binding.txtComponenteManipuladora3.text = "Componente: ${persona.componenteAsignado}"
                        binding.txtComponenteManipuladora3.setBackgroundResource(R.drawable.bg_pill_green)
                        binding.txtComponenteManipuladora3.setTextColor(Color.parseColor("#176B35"))

                        val estadoPrep = PreparacionEstadoManager.obtenerEstado(this, persona.nombre, persona.componenteAsignado, persona.correo)
                        configurarBadgeEstado(binding.txtEstadoManipuladora3, estadoPrep)
                    } else {
                        binding.txtComponenteManipuladora3.text = "Toca para asignar componente"
                        binding.txtComponenteManipuladora3.setBackgroundResource(R.drawable.bg_pill_yellow)
                        binding.txtComponenteManipuladora3.setTextColor(Color.parseColor("#855B00"))
                        binding.txtEstadoManipuladora3.text = "Asignar ›"
                        binding.txtEstadoManipuladora3.setBackgroundResource(R.drawable.bg_estado_disponible)
                        binding.txtEstadoManipuladora3.setTextColor(Color.parseColor("#1B3520"))
                    }
                } else {
                    binding.cardManipuladora3.visibility = View.GONE
                }
            }
        }
    }

    private fun configurarBadgeEstado(textView: TextView, estado: String) {
        when (estado) {
            PreparacionEstadoManager.ESTADO_POR_HACER -> {
                textView.text = "Por hacer"
                textView.setBackgroundResource(R.drawable.bg_tag_plato)
                textView.setTextColor(Color.parseColor("#1B3317"))
            }
            PreparacionEstadoManager.ESTADO_EN_PROCESO -> {
                textView.text = "En proceso"
                textView.setBackgroundResource(R.drawable.bg_pill_yellow)
                textView.setTextColor(Color.parseColor("#855B00"))
            }
            PreparacionEstadoManager.ESTADO_FINALIZADA -> {
                textView.text = "Finalizada"
                textView.setBackgroundResource(R.drawable.bg_pill_green)
                textView.setTextColor(Color.parseColor("#176B35"))
            }
            else -> {
                textView.text = estado
                textView.setBackgroundResource(R.drawable.bg_estado_disponible)
                textView.setTextColor(Color.parseColor("#1B3520"))
            }
        }
    }

    private fun mostrarOpcionesJefa() {
        if (componenteJefa.isNullOrBlank()) {
            mostrarDialogoAsignarComponenteJefa()
            return
        }

        val estadoActual = PreparacionEstadoManager.obtenerEstado(this, nombreJefa, componenteJefa, correoJefa)
        val opciones = listOf(
            "Ir a preparar mi componente (Ver pasos e insumos)",
            "Cambiar estado (Actual: $estadoActual)",
            "Cambiar componente asignado",
            "Quitar mi asignación"
        )

        mostrarDialogoOpcionesCustom("Opciones Jefa: $nombreJefa", opciones) { which ->
            when (which) {
                0 -> {
                    val intent = Intent(this, IniciarPreparacionActivity::class.java).apply {
                        putExtra("nombre_menu", nombreMenu)
                        putExtra("jornada", jornada)
                    }
                    startActivity(intent)
                }
                1 -> mostrarDialogoCambiarEstado(nombreJefa, componenteJefa, correoJefa)
                2 -> mostrarDialogoAsignarComponenteJefa()
                3 -> asignarComponenteJefa(null)
            }
        }
    }

    private fun mostrarOpcionesManipuladora(manipuladora: ManipuladoraLocal) {
        if (manipuladora.componenteAsignado.isNullOrBlank()) {
            mostrarDialogoAsignarComponente(manipuladora)
            return
        }

        val estadoActual = PreparacionEstadoManager.obtenerEstado(this, manipuladora.nombre, manipuladora.componenteAsignado, manipuladora.correo)
        val opciones = listOf(
            "Cambiar estado (Actual: $estadoActual)",
            "Cambiar componente asignado",
            "Quitar componente asignado"
        )

        mostrarDialogoOpcionesCustom("Opciones: ${manipuladora.nombre}", opciones) { which ->
            when (which) {
                0 -> mostrarDialogoCambiarEstado(manipuladora.nombre, manipuladora.componenteAsignado, manipuladora.correo)
                1 -> mostrarDialogoAsignarComponente(manipuladora)
                2 -> asignarComponente(manipuladora, null)
            }
        }
    }

    private fun mostrarDialogoCambiarEstado(persona: String, componente: String?, correo: String?) {
        val opciones = listOf(
            "Por hacer / Pendiente",
            "En proceso / En preparación",
            "Finalizada / Completada"
        )

        mostrarDialogoOpcionesCustom("Actualizar estado de $persona", opciones) { which ->
            val nuevoEstado = when (which) {
                0 -> PreparacionEstadoManager.ESTADO_POR_HACER
                1 -> PreparacionEstadoManager.ESTADO_EN_PROCESO
                2 -> PreparacionEstadoManager.ESTADO_FINALIZADA
                else -> PreparacionEstadoManager.ESTADO_POR_HACER
            }
            PreparacionEstadoManager.guardarEstado(this, persona, componente, nuevoEstado, correo)
            actualizarInterfaz()

            // Sincronizar de inmediato a la nube
            lifecycleScope.launch {
                val sessionMgr = SessionManager(this@ManipuladorasAsignadasActivity)
                val uid = when {
                    persona == nombreJefa -> sessionMgr.getUserId().let { if (it > 0) it else 19 }
                    else -> {
                        val manip = manipuladoras.firstOrNull { it.nombre.equals(persona, true) || it.correo.equals(correo, true) }
                        if (manip != null && manip.idUsuario > 0) manip.idUsuario else {
                            val real = ManipuladorasManager.obtenerManipuladoras().firstOrNull {
                                it.correo.equals(correo, true) || it.nombre.equals(persona, true)
                            }
                            real?.idUsuario ?: real?.id ?: -1
                        }
                    }
                }
                if (uid > 0 && !componente.isNullOrBlank()) {
                    com.example.movilmanupuladora.utils.PreparacionSyncManager.guardarAsignacionEnBackend(
                        idUsuario = uid,
                        componente = componente,
                        estado = nuevoEstado
                    )
                }
            }

            Toast.makeText(this, "Estado de $persona cambiado a: $nuevoEstado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun obtenerEmojiComponente(c: String): String {
        return ""
    }

    private fun mostrarDialogoAsignarComponenteJefa() {
        val componentes = obtenerComponentesPlato(nombreMenu).toMutableList()
        val opciones = mutableListOf<String>()

        for (c in componentes) {
            val otra = manipuladoras.firstOrNull { it.componenteAsignado?.equals(c, ignoreCase = true) == true }
            if (otra != null) {
                opciones.add("$c  (Ocupado por ${otra.nombre})")
            } else if (componenteJefa?.equals(c, ignoreCase = true) == true) {
                opciones.add("$c  (Asignado a ti)")
            } else {
                opciones.add(c)
            }
        }

        opciones.add("Ingresar otro componente personalizado...")
        if (!componenteJefa.isNullOrBlank()) {
            opciones.add("Quitar componente asignado")
        }

        mostrarDialogoOpcionesCustom("Asignarme componente para preparar", opciones) { posicion ->
            when {
                posicion < componentes.size -> {
                    val compElegido = componentes[posicion]
                    val otra = manipuladoras.firstOrNull { it.componenteAsignado?.equals(compElegido, ignoreCase = true) == true }
                    if (otra != null) {
                        mostrarDialogoConfirmacionCustom(
                            "Componente ya asignado",
                            "El componente '$compElegido' ya se encuentra asignado a ${otra.nombre}.\n\nNo se permite duplicar componentes en el equipo.\n\n¿Deseas transferirlo a ti misma?",
                            "Sí, transferir a mí"
                        ) {
                            otra.componenteAsignado = null
                            asignarComponenteJefa(compElegido)
                        }
                    } else {
                        asignarComponenteJefa(compElegido)
                    }
                }
                posicion == componentes.size -> {
                    mostrarDialogoComponentePersonalizadoJefa()
                }
                else -> {
                    asignarComponenteJefa(null)
                }
            }
        }
    }

    private fun mostrarDialogoComponentePersonalizadoJefa() {
        mostrarDialogoInputCustom(
            "Componente personalizado (Jefa)",
            "Escribe el componente que prepararás:"
        ) { nuevoComp ->
            val otra = manipuladoras.firstOrNull { it.componenteAsignado?.equals(nuevoComp, ignoreCase = true) == true }
            if (otra != null) {
                mostrarDialogoConfirmacionCustom(
                    "Componente ya asignado",
                    "El componente '$nuevoComp' ya está asignado a ${otra.nombre}.\n\n¿Deseas transferirlo a ti misma?",
                    "Sí, transferir"
                ) {
                    otra.componenteAsignado = null
                    asignarComponenteJefa(nuevoComp)
                }
            } else {
                asignarComponenteJefa(nuevoComp)
            }
        }
    }

    private fun asignarComponenteJefa(componente: String?) {
        if (componente != null) {
            val yaAsignada = manipuladoras.firstOrNull { it.componenteAsignado?.equals(componente, ignoreCase = true) == true }
            if (yaAsignada != null) {
                Toast.makeText(
                    this,
                    "No se puede asignar: '$componente' ya está asignado a ${yaAsignada.nombre}",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        componenteJefa = componente
        actualizarInterfaz()

        if (componente != null) {
            Toast.makeText(
                this,
                "'$componente' asignado a la Jefa de Cocina",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Componente desasignado de la Jefa",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarDialogoAsignarComponente(manipuladora: ManipuladoraLocal) {
        val componentes = obtenerComponentesPlato(nombreMenu).toMutableList()
        val opciones = mutableListOf<String>()

        for (c in componentes) {
            val emoji = obtenerEmojiComponente(c)

            // Validar si la Jefa o alguna otra manipuladora ya tiene este componente
            val otra = manipuladoras.firstOrNull { it != manipuladora && it.componenteAsignado?.equals(c, ignoreCase = true) == true }
            val ocupadoPorJefa = componenteJefa?.equals(c, ignoreCase = true) == true

            when {
                ocupadoPorJefa -> opciones.add("$c  (Ocupado por la Jefa)")
                otra != null -> opciones.add("$c  (Ocupado por ${otra.nombre})")
                manipuladora.componenteAsignado?.equals(c, ignoreCase = true) == true -> opciones.add("$c  (Asignado)")
                else -> opciones.add(c)
            }
        }

        opciones.add("Ingresar otro componente personalizado...")
        if (!manipuladora.componenteAsignado.isNullOrBlank()) {
            opciones.add("Quitar componente asignado")
        }

        mostrarDialogoOpcionesCustom("Asignar componente a ${manipuladora.nombre}", opciones) { posicion ->
            when {
                posicion < componentes.size -> {
                    val compElegido = componentes[posicion]
                    val otra = manipuladoras.firstOrNull { it != manipuladora && it.componenteAsignado?.equals(compElegido, ignoreCase = true) == true }
                    val ocupadoPorJefa = componenteJefa?.equals(compElegido, ignoreCase = true) == true

                    when {
                        ocupadoPorJefa -> {
                            mostrarDialogoConfirmacionCustom(
                                "Componente asignado a la Jefa",
                                "El componente '$compElegido' está asignado a ti (Jefa de Cocina).\n\n¿Deseas transferirlo a ${manipuladora.nombre}?",
                                "Sí, transferir"
                            ) {
                                componenteJefa = null
                                asignarComponente(manipuladora, compElegido)
                            }
                        }
                        otra != null -> {
                            mostrarDialogoConfirmacionCustom(
                                "Componente ya asignado",
                                "El componente '$compElegido' ya se encuentra asignado a ${otra.nombre}.\n\nNo se permite que dos personas tengan el mismo componente.\n\n¿Deseas transferirlo a ${manipuladora.nombre}?",
                                "Sí, transferir"
                            ) {
                                otra.componenteAsignado = null
                                asignarComponente(manipuladora, compElegido)
                            }
                        }
                        else -> {
                            asignarComponente(manipuladora, compElegido)
                        }
                    }
                }
                posicion == componentes.size -> {
                    mostrarDialogoComponentePersonalizado(manipuladora)
                }
                else -> {
                    asignarComponente(manipuladora, null)
                }
            }
        }
    }

    private fun mostrarDialogoComponentePersonalizado(manipuladora: ManipuladoraLocal) {
        mostrarDialogoInputCustom(
            "Componente personalizado",
            "Escribe el componente que preparará ${manipuladora.nombre}:"
        ) { nuevoComp ->
            val otra = manipuladoras.firstOrNull { it != manipuladora && it.componenteAsignado?.equals(nuevoComp, ignoreCase = true) == true }
            val ocupadoPorJefa = componenteJefa?.equals(nuevoComp, ignoreCase = true) == true

            when {
                ocupadoPorJefa -> {
                    mostrarDialogoConfirmacionCustom(
                        "Componente asignado a la Jefa",
                        "El componente '$nuevoComp' está asignado a ti (Jefa de Cocina).\n\n¿Deseas transferirlo a ${manipuladora.nombre}?",
                        "Sí, transferir"
                    ) {
                        componenteJefa = null
                        asignarComponente(manipuladora, nuevoComp)
                    }
                }
                otra != null -> {
                    mostrarDialogoConfirmacionCustom(
                        "Componente ya asignado",
                        "El componente '$nuevoComp' ya está asignado a ${otra.nombre}.\n\n¿Deseas transferirlo a ${manipuladora.nombre}?",
                        "Sí, transferir"
                    ) {
                        otra.componenteAsignado = null
                        asignarComponente(manipuladora, nuevoComp)
                    }
                }
                else -> {
                    asignarComponente(manipuladora, nuevoComp)
                }
            }
        }
    }

    private fun asignarComponente(manipuladora: ManipuladoraLocal, componente: String?) {
        if (componente != null) {
            val yaAsignada = manipuladoras.firstOrNull { it != manipuladora && it.componenteAsignado?.equals(componente, ignoreCase = true) == true }
            if (yaAsignada != null) {
                Toast.makeText(
                    this,
                    "No se puede asignar: '$componente' ya está asignado a ${yaAsignada.nombre}",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            if (componenteJefa?.equals(componente, ignoreCase = true) == true) {
                Toast.makeText(
                    this,
                    "No se puede asignar: '$componente' ya está asignado a la Jefa",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        manipuladora.componenteAsignado = componente
        actualizarInterfaz()

        if (componente != null) {
            Toast.makeText(
                this,
                "'$componente' asignado a ${manipuladora.nombre}",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Componente desasignado de ${manipuladora.nombre}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarDialogoAgregar() {
        if (manipuladoras.size >= 3) {
            Toast.makeText(this, "Máximo 3 manipuladoras", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener manipuladoras del Manager que no estén ya en la lista
        val disponibles: List<String> = ManipuladorasManager.obtenerManipuladoras()
            .filter { m -> manipuladoras.none { it.correo == m.correo && it.nombre == m.nombre } }
            .map { it.nombre }

        if (disponibles.isEmpty()) {
            Toast.makeText(this, "No hay más manipuladoras disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        mostrarDialogoOpcionesCustom("Seleccionar manipuladora", disponibles) { posicion ->
            val nombreSeleccionado = disponibles[posicion]
            val managerData = ManipuladorasManager.obtenerManipuladoras()
                .firstOrNull { it.nombre == nombreSeleccionado }
            val nueva = ManipuladoraLocal(
                nombre = nombreSeleccionado,
                correo = managerData?.correo ?: "",
                idUsuario = managerData?.idUsuario ?: managerData?.id ?: -1
            )
            manipuladoras.add(nueva)
            actualizarInterfaz()
            Toast.makeText(this, "${nueva.nombre} agregada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmarAsignacion() {
        if (manipuladoras.isEmpty() && componenteJefa.isNullOrBlank()) {
            Toast.makeText(this, "Debes tener al menos un integrante asignado para preparar", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificación estricta de no duplicados antes de confirmar (incluyendo a la Jefa)
        val componentesAsignados = mutableListOf<String>()
        if (!componenteJefa.isNullOrBlank()) {
            componentesAsignados.add(componenteJefa!!.lowercase().trim())
        }
        for (m in manipuladoras) {
            if (!m.componenteAsignado.isNullOrBlank()) {
                componentesAsignados.add(m.componenteAsignado!!.lowercase().trim())
            }
        }

        if (componentesAsignados.size != componentesAsignados.toSet().size) {
            Toast.makeText(
                this,
                "Error: No se permite asignar el mismo componente a más de una persona.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val resumen = StringBuilder()
        if (!componenteJefa.isNullOrBlank()) {
            val estJ = PreparacionEstadoManager.obtenerEstado(this, nombreJefa, componenteJefa, correoJefa)
            resumen.append("• Jefa ($nombreJefa): $componenteJefa ($estJ)\n")
        }
        for (m in manipuladoras) {
            val comp = m.componenteAsignado ?: "Sin componente asignado"
            val estM = PreparacionEstadoManager.obtenerEstado(this, m.nombre, m.componenteAsignado, m.correo)
            resumen.append("• ${m.nombre}: $comp ($estM)\n")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar Asignación de Componentes")
            .setMessage("Plato: $nombreMenu ($jornada)\n\nDistribución:\n$resumen")
            .setPositiveButton("Confirmar y Guardar") { _, _ ->
                guardarAsignacion()
            }
            .setNegativeButton("Seguir editando", null)
            .show()
    }

    private fun guardarAsignacion() {
        val preferencias = getSharedPreferences("ManipuladoraJefe", MODE_PRIVATE)
        val editor = preferencias.edit()
        editor.putString("jornada", jornada)
        editor.putString("plato_actual", nombreMenu)
        editor.putInt("cantidad_manipuladoras", manipuladoras.size)

        // Guardar componente de la Jefa de Cocina
        editor.putString("comp_jefa", componenteJefa ?: "")
        val jefaSinTilde = Normalizer.normalize(nombreJefa, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .trim().replace(" ", "_").lowercase()
        editor.putString("comp_$jefaSinTilde", componenteJefa ?: "")
        if (correoJefa.isNotBlank()) {
            val keyCorreoJefa =
                "comp_correo_" + correoJefa.trim().replace("@", "_").replace(".", "_").lowercase()
            editor.putString(keyCorreoJefa, componenteJefa ?: "")
        }

        for (m in manipuladoras) {
            val comp = m.componenteAsignado ?: ""
            // Guardar con clave por nombre normal
            val keyNombre = "comp_" + m.nombre.trim().replace(" ", "_").lowercase()
            editor.putString(keyNombre, comp)

            // Guardar con clave sin tildes (ej: sanchez vs sánchez)
            val nombreSinTilde = Normalizer.normalize(m.nombre, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .trim().replace(" ", "_").lowercase()
            editor.putString("comp_$nombreSinTilde", comp)

            // Guardar con clave por primer nombre (ej: andrea)
            val primerNombre = m.nombre.trim().split(" ").firstOrNull()?.lowercase() ?: ""
            if (primerNombre.isNotBlank()) {
                editor.putString("comp_$primerNombre", comp)
            }

            if (m.correo.isNotBlank()) {
                val keyCorreo =
                    "comp_correo_" + m.correo.trim().replace("@", "_").replace(".", "_").lowercase()
                editor.putString(keyCorreo, comp)
            }

            // Guardar nombre y clave por UID de la manipuladora para referencia
            editor.putString("manip_nombre_${m.correo}", m.nombre)
            if (m.idUsuario > 0) {
                editor.putString("comp_uid_${m.idUsuario}", comp)
                editor.putString("manip_nombre_uid_${m.idUsuario}", m.nombre)
                editor.putString("manip_correo_uid_${m.idUsuario}", m.correo)
            }
        }
        editor.putBoolean("componentes_asignados_guardados", true)
        editor.apply()

        val prefTurno = getSharedPreferences("TURNO_PAE", MODE_PRIVATE)
        prefTurno.edit()
            .putString("plato_nombre", nombreMenu)
            .putString("plato_jornada", jornada)
            .putInt("plato_id", idPlato)
            .apply()

        SessionManager(this).savePlatoSeleccionado(nombreMenu, idPlato)

        val btnGuardar = binding.btnConfirmarAsignacion
        btnGuardar.isEnabled = false
        btnGuardar.text = "Sincronizando con el equipo..."

        val listaItems = mutableListOf<PreparacionSyncManager.AsignacionItem>()

        val sessionMgr = SessionManager(this)
        val idJefa = sessionMgr.getUserId().let { if (it > 0) it else 19 }
        val estadoJefa = PreparacionEstadoManager.obtenerEstado(
            this, nombreJefa, componenteJefa, correoJefa
        )
        listaItems.add(
            PreparacionSyncManager.AsignacionItem(
                idUsuario = idJefa,
                nombre = nombreJefa,
                correo = correoJefa,
                componente = componenteJefa,
                estado = estadoJefa
            )
        )

        for (m in manipuladoras) {
            val uid = if (m.idUsuario > 0) m.idUsuario else {
                val real = ManipuladorasManager.obtenerManipuladoras().firstOrNull {
                    it.correo.equals(m.correo, ignoreCase = true) || it.nombre.equals(
                        m.nombre,
                        ignoreCase = true
                    )
                }
                real?.idUsuario ?: real?.id ?: -1
            }
            val est = PreparacionEstadoManager.obtenerEstado(
                this, m.nombre, m.componenteAsignado, m.correo
            )
            listaItems.add(
                PreparacionSyncManager.AsignacionItem(
                    idUsuario = uid,
                    nombre = m.nombre,
                    correo = m.correo,
                    componente = m.componenteAsignado,
                    estado = est
                )
            )
        }

        // Ejecutar en CoroutineScope(Dispatchers.Main) asegurando que no se cancele por el ciclo de vida
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withTimeoutOrNull(6000) {
                    PreparacionSyncManager.guardarPlatoYAsignacionesCompleto(
                        context = applicationContext,
                        nombreMenu = nombreMenu,
                        idPlato = idPlato,
                        jornada = jornada,
                        asignaciones = listaItems
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "ManipuladorasAsignadas",
                    "Error guardando asignaciones: ${e.message}"
                )
            }

            Toast.makeText(
                this@ManipuladorasAsignadasActivity,
                "¡Asignación de componentes y plato confirmados exitosamente!",
                Toast.LENGTH_LONG
            ).show()

            setResult(RESULT_OK)
            finish()
        }
    }
    private fun mostrarDialogoOpcionesCustom(titulo: String, opciones: List<String>, onOptionSelected: (Int) -> Unit) {
        val dialogBinding = com.example.movilmanupuladora.databinding.DialogOpcionesJefaBinding.inflate(layoutInflater)
        dialogBinding.txtTituloOpciones.text = titulo
        dialogBinding.contenedorBotonesOpciones.removeAllViews()

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        for ((index, opcion) in opciones.withIndex()) {
            val btn = com.google.android.material.button.MaterialButton(this).apply {
                text = opcion
                isAllCaps = false
                setTextColor(Color.parseColor("#1E293B"))
                textSize = 14f
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                strokeWidth = (1 * resources.displayMetrics.density).toInt()
                cornerRadius = (12 * resources.displayMetrics.density).toInt()
                gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }

                setOnClickListener {
                    dialog.dismiss()
                    onOptionSelected(index)
                }
            }
            dialogBinding.contenedorBotonesOpciones.addView(btn)
        }

        dialogBinding.btnCancelarOpciones.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun mostrarDialogoInputCustom(titulo: String, mensaje: String, onAsignar: (String) -> Unit) {
        val dialogBinding = com.example.movilmanupuladora.databinding.DialogInputPersonalizadoBinding.inflate(layoutInflater)
        dialogBinding.txtTituloInputModal.text = titulo
        dialogBinding.txtMensajeInputModal.text = mensaje

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnAsignarInput.setOnClickListener {
            val texto = dialogBinding.edtInputPersonalizado.text.toString().trim()
            if (texto.isNotEmpty()) {
                dialog.dismiss()
                onAsignar(texto)
            } else {
                dialogBinding.edtInputPersonalizado.error = "Escribe un componente válido"
            }
        }

        dialogBinding.btnCancelarInput.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun mostrarDialogoConfirmacionCustom(titulo: String, mensaje: String, textoPositivo: String, onConfirmar: () -> Unit) {
        val dialogBinding = com.example.movilmanupuladora.databinding.DialogConfirmarAsignacionBinding.inflate(layoutInflater)
        dialogBinding.txtTituloConfirmacion.text = titulo
        dialogBinding.txtMensajeConfirmacionAsignacion.text = mensaje
        dialogBinding.btnConfirmarGuardarAsignacion.text = textoPositivo

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnConfirmarGuardarAsignacion.setOnClickListener {
            dialog.dismiss()
            onConfirmar()
        }

        dialogBinding.btnSeguirEditandoAsignacion.text = "Cancelar"
        dialogBinding.btnSeguirEditandoAsignacion.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density).toInt()

    private fun confirmarCancelar() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Volver")
            .setMessage("¿Deseas volver a la pantalla de preparación?")
            .setPositiveButton("Sí, volver") { _, _ ->
                finish()
            }
            .setNegativeButton("Continuar aquí", null)
            .show()
    }
}
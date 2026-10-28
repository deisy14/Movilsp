package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.MenuPaeCatalog
import com.example.movilmanupuladora.databinding.ActivityInicioJefaBinding
import com.example.movilmanupuladora.utils.NavigationHelperJefa
import com.example.movilmanupuladora.utils.PreparacionSyncManager
import com.example.movilmanupuladora.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class InicioJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInicioJefaBinding
    private lateinit var sessionManager: SessionManager

    private var diaSeleccionado: Calendar = Calendar.getInstance()
    private val diasSemanaActual = mutableListOf<Calendar>()

    data class Plato(
        val id: Int,
        val nombre: String,
        val componente: String,
        val imagen: Int
    )

    companion object {
        val imagenesPlatos = listOf(
            R.drawable.plato1,
            R.drawable.plato2,
            R.drawable.plato3,
            R.drawable.plato4,
            R.drawable.plato5,
            R.drawable.plato6,
            R.drawable.plato7,
            R.drawable.plato8,
            R.drawable.plato9,
            R.drawable.plato10,
            R.drawable.plato11,
            R.drawable.plato14,
            R.drawable.plato15
        )

        fun obtenerImagenPlato(id: Int? = null, index: Int = 0): Int {
            if (id != null && id in 1..20) {
                return MenuPaeCatalog.obtenerMenuPorId(id).imagenRes
            }
            val semilla = Math.abs(id ?: index)
            return imagenesPlatos[semilla % imagenesPlatos.size]
        }

        fun obtenerImagenPlatoPorNombre(nombre: String?): Int {
            return MenuPaeCatalog.obtenerImagenPorNombre(nombre)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityInicioJefaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        sessionManager = SessionManager(this)

        configurarHeaderJefa()
        configurarSemanaActual()
        configurarEventos()
        configurarNavegacionInferior()

        actualizarResumen()
        cargarAsistenciaDelBackend()
    }

    override fun onResume() {
        super.onResume()
        actualizarResumen()
        cargarAsistenciaDelBackend()
        lifecycleScope.launch {
            try {
                PreparacionSyncManager.sincronizarDesdeBackend(this@InicioJefaActivity)
                actualizarPlatosVisuales()
            } catch (_: Exception) { }
        }
    }

    // =========================================================
    // CONFIGURACIÓN DINÁMICA DE LA SEMANA (LUN - VIE)
    // =========================================================

    private fun configurarSemanaActual() {
        diasSemanaActual.clear()

        // Calcular el lunes de la semana actual
        val calLunes = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        for (i in 0 until 5) {
            diasSemanaActual.add(calLunes.clone() as Calendar)
            calLunes.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Subtítulo con rango de semana: e.g. "Menús programados · 26 – 30 de octubre"
        val primerDia = diasSemanaActual.first()
        val ultimoDia = diasSemanaActual.last()
        val mesNombre = SimpleDateFormat("MMMM", Locale.forLanguageTag("es-CO")).format(primerDia.time)
        val rangoTexto = "Menús programados · ${primerDia.get(Calendar.DAY_OF_MONTH)} – ${ultimoDia.get(Calendar.DAY_OF_MONTH)} de $mesNombre"
        binding.txtRangoSemana.text = rangoTexto

        // Asignar los números de cada día
        binding.txtNumDiaLun.text = diasSemanaActual[0].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaMar.text = diasSemanaActual[1].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaMie.text = diasSemanaActual[2].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaJue.text = diasSemanaActual[3].get(Calendar.DAY_OF_MONTH).toString()
        binding.txtNumDiaVie.text = diasSemanaActual[4].get(Calendar.DAY_OF_MONTH).toString()

        // Por defecto seleccionar el día de hoy si está dentro de la semana
        val hoy = Calendar.getInstance()
        val indiceHoy = diasSemanaActual.indexOfFirst { esMismoDia(it, hoy) }
        val diaInicial = if (indiceHoy != -1) diasSemanaActual[indiceHoy] else diasSemanaActual.first()

        seleccionarDia(diaInicial)
    }

    private fun seleccionarDia(cal: Calendar) {
        diaSeleccionado = cal.clone() as Calendar

        // Actualizar visual de los 5 botones
        actualizarBotonDia(binding.btnDiaLun, binding.txtNomDiaLun, binding.txtNumDiaLun, esMismoDia(diasSemanaActual[0], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaMar, binding.txtNomDiaMar, binding.txtNumDiaMar, esMismoDia(diasSemanaActual[1], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaMie, binding.txtNomDiaMie, binding.txtNumDiaMie, esMismoDia(diasSemanaActual[2], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaJue, binding.txtNomDiaJue, binding.txtNumDiaJue, esMismoDia(diasSemanaActual[3], diaSeleccionado))
        actualizarBotonDia(binding.btnDiaVie, binding.txtNomDiaVie, binding.txtNumDiaVie, esMismoDia(diasSemanaActual[4], diaSeleccionado))

        // Formatear texto de fecha: "Lun, 26 de octubre"
        val formato = SimpleDateFormat("EEE, d 'de' MMMM", Locale.forLanguageTag("es-CO"))
        val textoFecha = formato.format(diaSeleccionado.time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        binding.txtFechaSeleccionadaBanner.text = textoFecha
        binding.txtFechaResumenDia.text = textoFecha

        actualizarPlatosVisuales()
    }

    private fun actualizarBotonDia(
        container: LinearLayout,
        txtNombre: TextView,
        txtNumero: TextView,
        activo: Boolean
    ) {
        if (activo) {
            container.setBackgroundResource(R.drawable.bg_dia_semana_activo)
            container.elevation = 4f * resources.displayMetrics.density
            txtNombre.setTextColor(Color.parseColor("#1E293B"))
            txtNumero.setTextColor(Color.parseColor("#1E293B"))
        } else {
            container.setBackgroundResource(R.drawable.bg_dia_semana_inactivo)
            container.elevation = 0f
            txtNombre.setTextColor(Color.parseColor("#64748B"))
            txtNumero.setTextColor(Color.parseColor("#1E293B"))
        }
    }

    private fun esMismoDia(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    // =========================================================
    // ACTUALIZACIÓN DE PLATOS DEL DÍA SELECCIONADO
    // =========================================================

    private fun actualizarPlatosVisuales() {
        val menuDia = MenuPaeCatalog.obtenerMenuDiaCompleto(diaSeleccionado)

        binding.imgDesayuno.setImageResource(menuDia.desayuno.imagenRes)
        binding.txtNombreDesayuno.text = menuDia.desayuno.nombre
        binding.txtComponenteDesayuno.text = menuDia.desayuno.componente

        binding.imgAlmuerzo.setImageResource(menuDia.almuerzo.imagenRes)
        binding.txtNombreAlmuerzo.text = menuDia.almuerzo.nombre
        binding.txtComponenteAlmuerzo.text = menuDia.almuerzo.componente

        binding.imgMerienda.setImageResource(menuDia.merienda.imagenRes)
        binding.txtNombreMerienda.text = menuDia.merienda.nombre
        binding.txtComponenteMerienda.text = menuDia.merienda.componente
    }

    // =========================================================
    // EVENTOS Y ACCIONES
    // =========================================================

    private fun configurarEventos() {
        // Clic en los días de la semana
        binding.btnDiaLun.setOnClickListener { if (diasSemanaActual.isNotEmpty()) seleccionarDia(diasSemanaActual[0]) }
        binding.btnDiaMar.setOnClickListener { if (diasSemanaActual.size > 1) seleccionarDia(diasSemanaActual[1]) }
        binding.btnDiaMie.setOnClickListener { if (diasSemanaActual.size > 2) seleccionarDia(diasSemanaActual[2]) }
        binding.btnDiaJue.setOnClickListener { if (diasSemanaActual.size > 3) seleccionarDia(diasSemanaActual[3]) }
        binding.btnDiaVie.setOnClickListener { if (diasSemanaActual.size > 4) seleccionarDia(diasSemanaActual[4]) }

        // Clic en la flecha amarilla del recuadro del calendario -> Abre Pantalla 2 (MenuDelDiaMesActivity)
        binding.bannerCalendarioMes.setOnClickListener {
            val intent = Intent(this, MenuDelDiaMesActivity::class.java).apply {
                putExtra("fecha_milisegundos", diaSeleccionado.timeInMillis)
            }
            startActivity(intent)
        }

        // Clic en "Registra la asistencia" -> Abre AsistenciaJefaActivity
        binding.bannerRegistraAsistencia.setOnClickListener {
            val intent = Intent(this, AsistenciaJefaActivity::class.java)
            startActivity(intent)
        }

        // Clic en "Resumen del día" -> Abre AsistenciaJefaActivity
        binding.cardResumenDia.setOnClickListener {
            val intent = Intent(this, AsistenciaJefaActivity::class.java)
            startActivity(intent)
        }

        // Clic en tarjetas de comida -> Permite ver el plato en pantalla completa
        binding.cardDesayuno.setOnClickListener {
            val intent = Intent(this, MenuDelDiaMesActivity::class.java).apply {
                putExtra("fecha_milisegundos", diaSeleccionado.timeInMillis)
            }
            startActivity(intent)
        }

        binding.cardAlmuerzo.setOnClickListener {
            val intent = Intent(this, MenuDelDiaMesActivity::class.java).apply {
                putExtra("fecha_milisegundos", diaSeleccionado.timeInMillis)
            }
            startActivity(intent)
        }

        binding.cardMerienda.setOnClickListener {
            val intent = Intent(this, MenuDelDiaMesActivity::class.java).apply {
                putExtra("fecha_milisegundos", diaSeleccionado.timeInMillis)
            }
            startActivity(intent)
        }
    }

    private fun configurarHeaderJefa() {
        val nombreUser = sessionManager.getUserName() ?: "Jefa de Cocina"
        binding.txtSaludoJefa.text = "Hola, $nombreUser"
        val iniciales = nombreUser.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "JF" }
        binding.txtInicialesJefa.text = iniciales

        binding.btnPerfilHeader.setOnClickListener {
            val intent = Intent(this, PerfilJefaActivity::class.java)
            startActivity(intent)
        }

        binding.btnNotificacionesJefa.setOnClickListener {
            val popupBinding = com.example.movilmanupuladora.databinding.PopupNotificacionesBinding.inflate(layoutInflater)
            popupBinding.txtCantidadNotificaciones.text = "3"

            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(popupBinding.root)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            popupBinding.notificacionPreparacion.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, HistorialPreparacionesActivity::class.java))
            }

            popupBinding.notificacionInventario.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, InventarioJefaActivity::class.java))
            }

            popupBinding.notificacionAsistencia.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, AsistenciaJefaActivity::class.java))
            }

            popupBinding.btnVerTodasNotificaciones.setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, com.example.movilmanupuladora.ui.manipuladora.AvisosActivity::class.java))
            }

            dialog.show()
        }
    }

    private fun actualizarResumen() {
        val p = AsistenciaManager.obtenerPrimaria(this)
        val s = AsistenciaManager.obtenerSecundaria(this)
        val suma = p + s
        val guardados = if (suma > 0) {
            suma
        } else {
            AsistenciaManager.obtenerManana(this)
        }

        val totalRaciones = if (guardados > 0) guardados else 124
        binding.txtRacionesTotales.text = totalRaciones.toString()
        binding.txtMenusActivos.text = "3"

        val cantManipuladoras = ManipuladorasManager.obtenerManipuladoras().size
        binding.txtCantManipuladoras.text = if (cantManipuladoras > 0) cantManipuladoras.toString() else "8"
    }

    private fun cargarAsistenciaDelBackend() {
        lifecycleScope.launch {
            try {
                sessionManager.fetchAuthToken()
                val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val asistenciasRes = RetrofitClient.apiService.obtenerAsistenciaDiaria()
                if (asistenciasRes.isSuccessful && !asistenciasRes.body().isNullOrEmpty()) {
                    val lista = asistenciasRes.body()!!
                    var asistenciasHoy = lista.filter { it.fecha.trim().startsWith(fechaHoy) }
                    if (asistenciasHoy.isEmpty()) {
                        val fechaMasReciente = lista.maxByOrNull { it.idAsistencia ?: 0 }?.fecha?.trim()?.take(10)
                        if (!fechaMasReciente.isNullOrEmpty()) {
                            asistenciasHoy = lista.filter { it.fecha.trim().startsWith(fechaMasReciente) }
                        }
                    }

                    val regPrimaria = asistenciasHoy.filter { it.idGrado == 16 }.maxByOrNull { it.idAsistencia ?: 0 }
                    val regSecundaria = asistenciasHoy.filter { it.idGrado == 19 }.maxByOrNull { it.idAsistencia ?: 0 }

                    var p = regPrimaria?.ninosPresentes ?: AsistenciaManager.obtenerPrimaria(this@InicioJefaActivity)
                    var s = regSecundaria?.ninosPresentes ?: AsistenciaManager.obtenerSecundaria(this@InicioJefaActivity)

                    if (regPrimaria == null && regSecundaria == null && asistenciasHoy.isNotEmpty()) {
                        val regLegacy = asistenciasHoy.maxByOrNull { it.idAsistencia ?: 0 }!!
                        val tot = regLegacy.ninosPresentes
                        p = (tot * 0.6).toInt()
                        s = tot - p
                    }

                    val total = p + s
                    if (total > 0) {
                        AsistenciaManager.guardarPrimaria(this@InicioJefaActivity, p)
                        AsistenciaManager.guardarSecundaria(this@InicioJefaActivity, s)
                        AsistenciaManager.guardarManana(this@InicioJefaActivity, total)
                    }

                    actualizarResumen()
                }
            } catch (_: Exception) { }
        }
    }

    private fun configurarNavegacionInferior() {
        NavigationHelperJefa.setupBarraNavegacionJefa(
            this,
            binding.barraNavegacionJefa,
            NavigationHelperJefa.TabJefa.INICIO
        )
    }
}
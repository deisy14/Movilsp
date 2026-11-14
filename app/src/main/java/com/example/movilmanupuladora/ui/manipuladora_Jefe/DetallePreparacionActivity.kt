package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.databinding.ActivityDetallePreparacionBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DetallePreparacionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetallePreparacionBinding

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding =
            ActivityDetallePreparacionBinding.inflate(layoutInflater)

        setContentView(binding.root)

        configurarInsets()

        configurarFecha()

        cargarDatos()

        configurarBotones()
    }


    // =========================================================
    // INSETS
    // =========================================================

    private fun configurarInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { v, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }


    // =========================================================
    // FECHA
    // =========================================================

    private fun configurarFecha() {
        val fechaIntent = intent.getStringExtra("fecha")
        if (fechaIntent.isNullOrEmpty()) {
            val formato = SimpleDateFormat(
                "EEEE, dd 'de' MMMM",
                Locale("es", "ES")
            )
            val fecha = formato.format(Date())
            binding.txtFecha.text =
                fecha.replaceFirstChar {
                    it.uppercase()
                }
        } else {
            binding.txtFecha.text = fechaIntent
        }
    }


    // =========================================================
    // CARGAR DATOS
    // =========================================================

    private fun cargarDatos() {
        val platoIntent = intent.getStringExtra("plato") ?: "Arroz con pollo"
        val jornadaIntent = intent.getStringExtra("jornada") ?: "Mañana"
        val ninosIntent = intent.getStringExtra("ninos") ?: "35 niños"

        // -----------------------------------------------------
        // ESTADO
        // -----------------------------------------------------

        binding.txtEstado.text =
            "Programada / En curso"


        // -----------------------------------------------------
        // MENÚ
        // -----------------------------------------------------

        binding.txtMenu.text =
            platoIntent


        // -----------------------------------------------------
        // JORNADA
        // -----------------------------------------------------

        binding.txtJornada.text =
            jornadaIntent


        // -----------------------------------------------------
        // NIÑOS
        // -----------------------------------------------------

        binding.txtNinos.text =
            ninosIntent


        // -----------------------------------------------------
        // MANIPULADORAS ASIGNADAS DINÁMICAS
        // -----------------------------------------------------
        val listaManip = ManipuladorasManager.obtenerManipuladoras().filter { it.activa }
        val jefaPrefs = getSharedPreferences("ManipuladoraJefe", MODE_PRIVATE)
        val textoManip = if (listaManip.isNotEmpty()) {
            listaManip.joinToString("\n") { m ->
                val comp = jefaPrefs.getString("comp_uid_${m.idUsuario}", null)
                    ?: jefaPrefs.getString("comp_${m.nombre.trim().replace(" ", "_").lowercase()}", null)
                if (!comp.isNullOrBlank()) "${m.nombre} ($comp)" else m.nombre
            }
        } else {
            val sessionMgr = com.example.movilmanupuladora.utils.SessionManager(this)
            sessionMgr.getUserName() ?: "Personal en turno"
        }
        binding.txtManipuladoras.text = textoManip

        // -----------------------------------------------------
        // ALIMENTOS / INSUMOS DINÁMICOS DE LA RECETA
        // -----------------------------------------------------
        val ninosNum = ninosIntent.filter { it.isDigit() }.toIntOrNull() ?: 50
        cargarIngredientesDinamicos(platoIntent, ninosNum)


        // -----------------------------------------------------
        // HORARIOS
        // -----------------------------------------------------

        binding.txtHoraInicio.text =
            "10:15 a. m."

        binding.txtHoraFin.text =
            "11:05 a. m."

        binding.txtDuracion.text =
            "50 minutos"


        // -----------------------------------------------------
        // OBSERVACIONES
        // -----------------------------------------------------

        binding.txtObservaciones.text =
            "Sin observaciones"
    }


    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        // -----------------------------------------------------
        // BOTÓN SUPERIOR
        // -----------------------------------------------------

        binding.btnCerrar.setOnClickListener {

            finish()
        }


        // -----------------------------------------------------
        // BOTÓN INFERIOR
        // -----------------------------------------------------

        binding.btnVolver.setOnClickListener {

            finish()
        }
    }

    private fun cargarIngredientesDinamicos(nombrePlato: String, ninos: Int) {
        lifecycleScope.launch {
            try {
                val platosRes = RetrofitClient.apiService.obtenerPlatos()
                val platosBackend = platosRes.body() ?: emptyList()
                val platoActual = platosBackend.firstOrNull {
                    it.nombrePlato?.equals(nombrePlato, ignoreCase = true) == true ||
                    (nombrePlato.isNotBlank() && it.nombrePlato?.contains(nombrePlato, ignoreCase = true) == true)
                }

                val compLista = if (!platoActual?.componente.isNullOrBlank()) {
                    platoActual!!.componente!!.split(",", "•", "-", "\n")
                        .map { it.trim() }
                        .filter { it.isNotBlank() && it != "—" }
                } else {
                    com.example.movilmanupuladora.data.model.MenuPaeCatalog.obtenerComponentesPorNombre(nombrePlato)
                }

                val resumenIngredientes = StringBuilder()
                for (comp in compLista) {
                    val cLower = comp.lowercase()
                    val (cant, unidad) = when {
                        cLower.contains("arroz") || cLower.contains("cereal") -> Pair((ninos * 0.08).toInt().coerceAtLeast(4), "kg")
                        cLower.contains("pollo") || cLower.contains("carne") || cLower.contains("huevo") || cLower.contains("prote") -> Pair((ninos * 0.12).toInt().coerceAtLeast(6), "kg")
                        cLower.contains("leche") || cLower.contains("avena") || cLower.contains("bebida") -> Pair((ninos * 0.2).toInt().coerceAtLeast(10), "L")
                        cLower.contains("fruta") || cLower.contains("manzana") || cLower.contains("mango") || cLower.contains("platano") -> Pair(ninos, "unid")
                        cLower.contains("pan") || cLower.contains("arepa") || cLower.contains("galleta") -> Pair(ninos, "unid")
                        else -> Pair((ninos * 0.05).toInt().coerceAtLeast(2), "kg")
                    }
                    val padComp = comp.padEnd(20, ' ')
                    resumenIngredientes.append("$padComp $cant $unidad\n")
                }

                if (resumenIngredientes.isNotBlank()) {
                    binding.txtAlimentos.text = resumenIngredientes.toString().trimEnd()
                }
            } catch (_: Exception) {
                val comps = com.example.movilmanupuladora.data.model.MenuPaeCatalog.obtenerComponentesPorNombre(nombrePlato)
                val sb = StringBuilder()
                for (comp in comps) {
                    val cant = (ninos * 0.08).toInt().coerceAtLeast(3)
                    sb.append("${comp.padEnd(18, ' ')} $cant kg\n")
                }
                binding.txtAlimentos.text = sb.toString().trimEnd()
            }
        }
    }
}
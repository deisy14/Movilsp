package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.DetallePlato
import com.example.movilmanupuladora.data.model.PlatoResponse
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.databinding.ActivityComponentesBinding
import com.example.movilmanupuladora.databinding.DialogRecetaComponenteBinding
import com.example.movilmanupuladora.databinding.ItemComponentePlatoBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ComponentesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComponentesBinding

    private val menuRepository =
        MenuRepository(RetrofitClient.apiService)

    private var platoActual: PlatoResponse? = null
    private var detalleActual: DetallePlato? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityComponentesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->

            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // VOLVER
        binding.btnVolver.setOnClickListener {
            finish()
        }

        // VER PASOS DE PREPARACIÓN
        binding.btnVerIngredientes.setOnClickListener {

            platoActual?.let { plato ->

                val intent =
                    Intent(this, PreparacionActivity::class.java)

                intent.putExtra("id_plato", plato.idPlato)
                intent.putExtra("nombre_plato", plato.nombrePlato)

                startActivity(intent)
            }
        }

        configurarBarraNavegacion()

        val idPlato =
            intent.getIntExtra("id_plato", -1)

        val idSeccion =
            intent.getIntExtra("id_seccion", -1)

        val nombrePlato =
            intent.getStringExtra("nombre_plato")

        cargarDatosBackend(
            idPlato,
            idSeccion,
            nombrePlato
        )
    }

    // ==========================================
    // DATOS DE RESPALDO
    // ==========================================

    private val platosBaseRespaldo = listOf(

        PlatoResponse(
            idPlato = 4,
            idSeccion = 2,
            nombrePlato = "bandeja paisa",
            componente = "carne molida, chicharron, aguacate, arroz, frijol"
        ),

        PlatoResponse(
            idPlato = 1,
            idSeccion = 3,
            nombrePlato = "arroz a la valenciana",
            componente = "proteina"
        ),

        PlatoResponse(
            idPlato = 2,
            idSeccion = 3,
            nombrePlato = "arroz con pollo",
            componente = "pollo"
        ),

        PlatoResponse(
            idPlato = 3,
            idSeccion = 3,
            nombrePlato = "Arroz con Pollo Especial",
            componente = "Principal"
        ),

        PlatoResponse(
            idPlato = 5,
            idSeccion = 4,
            nombrePlato = "cafe con pan",
            componente = "no se"
        )
    )

    // ==========================================
    // CONSUMO BACKEND
    // ==========================================

    private fun cargarDatosBackend(
        idPlatoBuscado: Int,
        idSeccionBuscada: Int,
        nombrePlatoBuscado: String?
    ) {

        lifecycleScope.launch {

            try {

                // SECCIONES
                var seccionNombre: String? = null

                val resSecciones =
                    menuRepository.obtenerSeccionesMenu()

                if (resSecciones.isSuccessful) {

                    val secciones =
                        resSecciones.body() ?: emptyList()

                    val seccionEncontrada =
                        if (idSeccionBuscada != -1) {

                            secciones.firstOrNull {
                                it.idSeccion == idSeccionBuscada
                            }

                        } else {

                            secciones.firstOrNull()
                        }

                    seccionNombre =
                        seccionEncontrada?.nombreSeccion
                }

                // PLATOS
                val resPlatos =
                    menuRepository.obtenerPlatos()

                val listaPlatos =
                    if (
                        resPlatos.isSuccessful &&
                        !resPlatos.body().isNullOrEmpty()
                    ) {

                        resPlatos.body()!!

                    } else {

                        platosBaseRespaldo
                    }

                // SELECCIONAR PLATO
                platoActual = when {

                    idPlatoBuscado != -1 ->
                        listaPlatos.firstOrNull {
                            it.idPlato == idPlatoBuscado
                        }

                    !nombrePlatoBuscado.isNullOrEmpty() ->
                        listaPlatos.firstOrNull {
                            it.nombrePlato.equals(
                                nombrePlatoBuscado,
                                ignoreCase = true
                            )
                        }

                    else -> null
                }
                    ?: listaPlatos.firstOrNull {
                        it.nombrePlato?.contains(
                            "bandeja",
                            ignoreCase = true
                        ) == true
                    }
                            ?: listaPlatos.firstOrNull()

                // MOSTRAR PLATO
                platoActual?.let { plato ->

                    actualizarUiPlato(
                        plato,
                        seccionNombre
                    )

                    mostrarComponentesDelPlato(plato)
                }

                // DETALLE
                val resDetalles =
                    menuRepository.obtenerDetallePlatos()

                if (resDetalles.isSuccessful) {

                    val detalles =
                        resDetalles.body() ?: emptyList()

                    detalleActual =
                        detalles.firstOrNull {
                            it.idPlato == platoActual?.idPlato
                        }

                    detalleActual?.estadoPreparacion?.let {
                        binding.tvBadgeEstado.text = it
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "Backend",
                    "Error de conexión con el backend",
                    e
                )

                platoActual =
                    platosBaseRespaldo.first()

                actualizarUiPlato(
                    platoActual!!,
                    "Almuerzo"
                )

                mostrarComponentesDelPlato(
                    platoActual!!
                )
            }
        }
    }

    // ==========================================
    // ACTUALIZAR PLATO
    // ==========================================

    private fun actualizarUiPlato(
        plato: PlatoResponse,
        seccionNombre: String?
    ) {

        val nombreFormateado =
            plato.nombrePlato
                ?.split(" ")
                ?.joinToString(" ") { palabra ->

                    palabra.replaceFirstChar {
                        if (it.isLowerCase())
                            it.titlecase()
                        else
                            it.toString()
                    }
                }
                ?: "Plato sin nombre"

        binding.tvNombrePlato.text =
            nombreFormateado

        val seccion =
            seccionNombre
                ?: when (plato.idSeccion) {

                    1 -> "Desayuno"
                    2 -> "Almuerzo"
                    3 -> "Merienda"

                    else -> "Almuerzo"
                }

        binding.tvTituloSeccion.text =
            "Componentes del $seccion"

        val imgRes =
            when {

                plato.nombrePlato?.contains(
                    "bandeja",
                    true
                ) == true ->
                    R.drawable.bandeja_paisa

                plato.nombrePlato?.contains(
                    "frijol",
                    true
                ) == true ->
                    R.drawable.frijoles

                plato.nombrePlato?.contains(
                    "chocolate",
                    true
                ) == true ->
                    R.drawable.chocolate

                plato.nombrePlato?.contains(
                    "huevo",
                    true
                ) == true ->
                    R.drawable.huevo_perico

                plato.nombrePlato?.contains(
                    "pollo",
                    true
                ) == true ->
                    R.drawable.apanado

                plato.nombrePlato?.contains(
                    "arroz",
                    true
                ) == true ->
                    R.drawable.arroz_de_leche

                else ->
                    R.drawable.frijoles
            }

        binding.imgPlato.setImageResource(imgRes)
    }

    // ==========================================
    // COMPONENTES
    // ==========================================

    private fun mostrarComponentesDelPlato(
        plato: PlatoResponse
    ) {

        binding.contenedorComponentes.removeAllViews()

        val rawComponentes =
            plato.componente?.trim() ?: ""

        val listaComponentes =

            if (rawComponentes.contains(",")) {

                rawComponentes
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

            } else if (rawComponentes.isNotEmpty()) {

                listOf(rawComponentes)

            } else {

                listOf("Componente principal")
            }

        for (componenteTexto in listaComponentes) {

            val itemBinding =
                ItemComponentePlatoBinding.inflate(
                    layoutInflater,
                    binding.contenedorComponentes,
                    false
                )

            val nombreCapitalizado =
                componenteTexto.replaceFirstChar {

                    if (it.isLowerCase())
                        it.titlecase()
                    else
                        it.toString()
                }

            itemBinding.tvNombreComponente.text =
                nombreCapitalizado

            val clasificacion =
                clasificarComponente(
                    componenteTexto
                )

            itemBinding.tvTipoComponente.text =
                clasificacion

            itemBinding.tvTipoComponente.visibility =
                View.VISIBLE

            itemBinding.imgComponente.setImageResource(
                obtenerIconoComponente(
                    componenteTexto
                )
            )

            val clickListener =
                View.OnClickListener {

                    mostrarDialogoRecetaComponente(
                        componenteTexto,
                        clasificacion,
                        plato
                    )
                }

            itemBinding.root.setOnClickListener(
                clickListener
            )

            itemBinding.tvTipoComponente.setOnClickListener(
                clickListener
            )

            binding.contenedorComponentes.addView(
                itemBinding.root
            )
        }
    }

    // ==========================================
    // CLASIFICACIÓN
    // ==========================================

    private fun clasificarComponente(
        nombre: String
    ): String {

        val n =
            nombre.lowercase().trim()

        return when {

            n.contains("carne") ||
                    n.contains("pollo") ||
                    n.contains("pescado") ||
                    n.contains("huevo") ||
                    n.contains("chicharron") ||
                    n.contains("chicharrón") ||
                    n.contains("proteina") ||
                    n.contains("proteína") ->
                "Proteína"

            n.contains("frijol") ||
                    n.contains("fríjol") ||
                    n.contains("lenteja") ||
                    n.contains("garbanzo") ||
                    n.contains("arveja") ||
                    n.contains("principio") ->
                "Principio"

            n.contains("aguacate") ||
                    n.contains("ensalada") ||
                    n.contains("verdura") ||
                    n.contains("platano") ||
                    n.contains("plátano") ||
                    n.contains("patacon") ||
                    n.contains("patacón") ||
                    n.contains("acompañ") ->
                "Acompañante"

            n.contains("arroz") ||
                    n.contains("pasta") ||
                    n.contains("arepa") ||
                    n.contains("pan") ||
                    n.contains("yuca") ||
                    n.contains("papa") ||
                    n.contains("cereal") ||
                    n.contains("base") ->
                "Cereal / Base"

            n.contains("sopa") ||
                    n.contains("crema") ||
                    n.contains("caldo") ||
                    n.contains("consome") ||
                    n.contains("consomé") ||
                    n.contains("ajiaco") ||
                    n.contains("sancocho") ->
                "Entrada"

            n.contains("jugo") ||
                    n.contains("leche") ||
                    n.contains("chocolate") ||
                    n.contains("cafe") ||
                    n.contains("café") ||
                    n.contains("avena") ||
                    n.contains("colada") ||
                    n.contains("limonada") ->
                "Bebida"

            n.contains("postre") ||
                    n.contains("fruta") ||
                    n.contains("gelatina") ||
                    n.contains("bocadillo") ->
                "Postre"

            else ->
                "Componente"
        }
    }

    // ==========================================
    // ICONOS
    // ==========================================

    private fun obtenerIconoComponente(
        nombre: String
    ): Int {

        val n =
            nombre.lowercase().trim()

        return when {

            n.contains("leche") ->
                R.drawable.ic_leche

            n.contains("chocolate") ->
                R.drawable.ic_chocolate_polvo

            n.contains("azucar") ||
                    n.contains("azúcar") ->
                R.drawable.ic_azucar

            n.contains("canela") ->
                R.drawable.ic_canela

            n.contains("frijol") ||
                    n.contains("fríjol") ||
                    n.contains("lenteja") ->
                R.drawable.frijoles

            n.contains("huevo") ->
                R.drawable.huevo_perico

            n.contains("pollo") ||
                    n.contains("carne") ||
                    n.contains("chicharron") ||
                    n.contains("chicharrón") ->
                R.drawable.apanado

            n.contains("arroz") ->
                R.drawable.arroz_de_leche

            else ->
                R.drawable.ic_plato_asignado
        }
    }

    // ==========================================
    // MODAL
    // ==========================================

    private fun mostrarDialogoRecetaComponente(
        nombreComponente: String,
        tipoComponente: String,
        plato: PlatoResponse
    ) {

        val dialogBinding =
            DialogRecetaComponenteBinding.inflate(
                layoutInflater
            )

        dialogBinding.tvTituloComponenteModal.text =
            nombreComponente.replaceFirstChar {
                if (it.isLowerCase())
                    it.titlecase()
                else
                    it.toString()
            }

        dialogBinding.tvBadgeTipoModal.text =
            tipoComponente

        dialogBinding.tvPlatoPerteneciente.text =
            plato.nombrePlato ?: "Plato escolar"

        dialogBinding.tvIngredientesComponente.text =
            "Ingredientes del componente"

        dialogBinding.tvSazonIngredientes.text =
            "Sazón y condimentos"

        dialogBinding.tvPorcionComponente.text =
            when (tipoComponente) {

                "Proteína" ->
                    "80g - 100g por ración"

                "Principio" ->
                    "90g - 120g por ración"

                "Cereal / Base" ->
                    "80g - 100g por ración"

                "Acompañante" ->
                    "50g - 70g por ración"

                "Bebida" ->
                    "200ml por ración"

                else ->
                    "1 porción institucional"
            }

        dialogBinding.imgComponenteModal.setImageResource(
            obtenerIconoComponente(
                nombreComponente
            )
        )

        val dialog =
            MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.root)
                .create()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialogBinding.btnCerrarModalComponente.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnCerrarRecetaModal.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnVerPasosModal.setOnClickListener {

            dialog.dismiss()

            startActivity(
                Intent(
                    this,
                    PreparacionActivity::class.java
                ).apply {

                    putExtra(
                        "id_plato",
                        plato.idPlato
                    )

                    putExtra(
                        "nombre_plato",
                        plato.nombrePlato
                    )

                    putExtra(
                        "componente_seleccionado",
                        nombreComponente
                    )
                }
            )
        }

        dialogBinding.btnVerIngredientesModal.setOnClickListener {

            dialog.dismiss()

            startActivity(
                Intent(
                    this,
                    IngredientesActivity::class.java
                ).apply {

                    putExtra(
                        "id_plato",
                        plato.idPlato
                    )

                    putExtra(
                        "nombre_plato",
                        plato.nombrePlato
                    )

                    putExtra(
                        "componente_seleccionado",
                        nombreComponente
                    )
                }
            )
        }

        dialog.show()
    }

    // ==========================================
    // BARRA DE NAVEGACIÓN
    // ==========================================

    private fun configurarBarraNavegacion() {

        binding.barraNavegacion.navInicio.setOnClickListener {

            startActivity(
                Intent(this, MainActivity::class.java)
            )

            finish()
        }

        binding.barraNavegacion.navAsignadas.setOnClickListener {
            // Ya estamos en Asignadas
        }

        binding.barraNavegacion.navInventario.setOnClickListener {

            startActivity(
                Intent(this, InventarioActivity::class.java)
            )

            finish()
        }

        binding.barraNavegacion.navAvisos.setOnClickListener {

            startActivity(
                Intent(this, AvisosActivity::class.java)
            )

            finish()
        }

        binding.barraNavegacion.navPerfil.setOnClickListener {

            startActivity(
                Intent(this, PerfilActivity::class.java)
            )

            finish()
        }
    }
}
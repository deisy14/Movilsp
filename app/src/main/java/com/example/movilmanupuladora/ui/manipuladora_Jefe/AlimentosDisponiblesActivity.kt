package com.example.manipuladorajefe

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.manipuladorajefe.databinding.ActivityAlimentosDisponiblesBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlimentosDisponiblesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlimentosDisponiblesBinding

    // =========================================================
    // FILTRO ACTUAL
    // =========================================================

    private var filtroActual = "TODOS"

    // =========================================================
    // ALIMENTOS
    // =========================================================

    private data class Alimento(
        val nombre: String,
        val categoria: String,
        val cantidad: String,
        val itemId: Int
    )

    private val alimentos = listOf(

        Alimento(
            nombre = "Arroz",
            categoria = "GRANOS",
            cantidad = "25 kg",
            itemId = R.id.itemArroz
        ),

        Alimento(
            nombre = "Frijol",
            categoria = "GRANOS",
            cantidad = "18 kg",
            itemId = R.id.itemFrijol
        ),

        Alimento(
            nombre = "Pollo",
            categoria = "PROTEINAS",
            cantidad = "24 kg",
            itemId = R.id.itemPollo
        ),

        Alimento(
            nombre = "Verduras",
            categoria = "OTROS",
            cantidad = "10 kg",
            itemId = R.id.itemVerduras
        ),

        Alimento(
            nombre = "Aceite",
            categoria = "OTROS",
            cantidad = "5 L",
            itemId = R.id.itemAceite
        ),

        Alimento(
            nombre = "Leche",
            categoria = "OTROS",
            cantidad = "20 L",
            itemId = R.id.itemLeche
        )
    )

    // =========================================================
    // ALIMENTOS SELECCIONADOS
    // =========================================================

    private val alimentosSeleccionados =
        mutableSetOf<String>()

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityAlimentosDisponiblesBinding.inflate(layoutInflater)

        setContentView(binding.root)

        configurarFecha()

        configurarVolver()

        configurarBusqueda()

        configurarFiltros()

        configurarAlimentos()

        configurarContinuar()

        actualizarLista()

        actualizarMensaje()
    }

    // =========================================================
    // FECHA
    // =========================================================

    private fun configurarFecha() {

        val formato = SimpleDateFormat(
            "EEEE, dd 'de' MMMM",
            Locale("es", "ES")
        )

        val fecha = formato.format(Date())

        binding.txtFecha.text =
            fecha.lowercase(Locale("es", "ES"))
    }

    // =========================================================
    // VOLVER
    // =========================================================

    private fun configurarVolver() {

        binding.btnVolver.setOnClickListener {

            finish()
        }
    }

    // =========================================================
    // BUSCADOR
    // =========================================================

    private fun configurarBusqueda() {

        binding.edtBuscarAlimento.addTextChangedListener(

            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    actualizarLista()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }

    // =========================================================
    // FILTROS
    // =========================================================

    private fun configurarFiltros() {

        binding.btnFiltroTodos.setOnClickListener {

            seleccionarFiltro("TODOS")
        }

        binding.btnFiltroGranos.setOnClickListener {

            seleccionarFiltro("GRANOS")
        }

        binding.btnFiltroProteinas.setOnClickListener {

            seleccionarFiltro("PROTEINAS")
        }

        binding.btnFiltroOtros.setOnClickListener {

            seleccionarFiltro("OTROS")
        }

        binding.btnFiltro.setOnClickListener {

            Toast.makeText(
                this,
                "Selecciona una categoría para filtrar.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // CAMBIAR FILTRO
    // =========================================================

    private fun seleccionarFiltro(
        filtro: String
    ) {

        filtroActual = filtro

        actualizarBotonesFiltro()

        actualizarLista()
    }

    // =========================================================
    // ESTADO VISUAL DE FILTROS
    // =========================================================

    private fun actualizarBotonesFiltro() {

        val amarillo =
            ContextCompat.getColor(
                this,
                R.color.amarillo_principal
            )

        val gris =
            ContextCompat.getColor(
                this,
                R.color.gris_claro
            )

        val blanco =
            ContextCompat.getColor(
                this,
                R.color.blanco
            )

        val negro =
            ContextCompat.getColor(
                this,
                R.color.negro_principal
            )

        configurarBotonFiltro(
            binding.btnFiltroTodos,
            filtroActual == "TODOS",
            amarillo,
            gris,
            blanco,
            negro
        )

        configurarBotonFiltro(
            binding.btnFiltroGranos,
            filtroActual == "GRANOS",
            amarillo,
            gris,
            blanco,
            negro
        )

        configurarBotonFiltro(
            binding.btnFiltroProteinas,
            filtroActual == "PROTEINAS",
            amarillo,
            gris,
            blanco,
            negro
        )

        configurarBotonFiltro(
            binding.btnFiltroOtros,
            filtroActual == "OTROS",
            amarillo,
            gris,
            blanco,
            negro
        )
    }

    // =========================================================
    // CONFIGURAR BOTÓN DE FILTRO
    // =========================================================

    private fun configurarBotonFiltro(
        boton: android.widget.TextView,
        seleccionado: Boolean,
        amarillo: Int,
        gris: Int,
        blanco: Int,
        negro: Int
    ) {

        if (seleccionado) {

            boton.setBackgroundColor(amarillo)

            boton.setTextColor(blanco)

        } else {

            boton.setBackgroundColor(gris)

            boton.setTextColor(negro)
        }
    }

    // =========================================================
    // CONFIGURAR ALIMENTOS
    // =========================================================

    private fun configurarAlimentos() {

        binding.itemArroz.setOnClickListener {

            seleccionarAlimento("Arroz")
        }

        binding.itemFrijol.setOnClickListener {

            seleccionarAlimento("Frijol")
        }

        binding.itemPollo.setOnClickListener {

            seleccionarAlimento("Pollo")
        }

        binding.itemVerduras.setOnClickListener {

            seleccionarAlimento("Verduras")
        }

        binding.itemAceite.setOnClickListener {

            seleccionarAlimento("Aceite")
        }

        binding.itemLeche.setOnClickListener {

            seleccionarAlimento("Leche")
        }
    }

    // =========================================================
    // SELECCIONAR ALIMENTO
    // =========================================================

    private fun seleccionarAlimento(
        nombre: String
    ) {

        if (alimentosSeleccionados.contains(nombre)) {

            alimentosSeleccionados.remove(nombre)

            Toast.makeText(
                this,
                "$nombre quitado de la selección.",
                Toast.LENGTH_SHORT
            ).show()

        } else {

            alimentosSeleccionados.add(nombre)

            Toast.makeText(
                this,
                "$nombre seleccionado.",
                Toast.LENGTH_SHORT
            ).show()
        }

        actualizarEstadosVisuales()

        actualizarMensaje()
    }

    // =========================================================
    // ACTUALIZAR LISTA
    // =========================================================

    private fun actualizarLista() {

        val textoBusqueda =
            binding.edtBuscarAlimento.text
                .toString()
                .trim()
                .lowercase(Locale("es", "ES"))

        alimentos.forEach { alimento ->

            val coincideCategoria =
                filtroActual == "TODOS" ||
                        alimento.categoria == filtroActual

            val coincideBusqueda =
                textoBusqueda.isEmpty() ||
                        alimento.nombre
                            .lowercase(Locale("es", "ES"))
                            .contains(textoBusqueda)

            val visible =
                coincideCategoria && coincideBusqueda

            findViewById<android.view.View>(
                alimento.itemId
            ).visibility =
                if (visible) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
        }

        actualizarTituloLista()
    }

    // =========================================================
    // TÍTULO DE LISTA
    // =========================================================

    private fun actualizarTituloLista() {

        val cantidadVisibles =
            alimentos.count { alimento ->

                val texto =
                    binding.edtBuscarAlimento.text
                        .toString()
                        .trim()
                        .lowercase(Locale("es", "ES"))

                val categoriaOk =
                    filtroActual == "TODOS" ||
                            alimento.categoria == filtroActual

                val busquedaOk =
                    texto.isEmpty() ||
                            alimento.nombre
                                .lowercase(Locale("es", "ES"))
                                .contains(texto)

                categoriaOk && busquedaOk
            }

        binding.txtTituloLista.text =
            if (cantidadVisibles == 1) {
                "1 producto disponible"
            } else {
                "$cantidadVisibles productos disponibles"
            }
    }

    // =========================================================
    // ESTADOS VISUALES
    // =========================================================

    private fun actualizarEstadosVisuales() {

        actualizarEstado(
            "Arroz",
            binding.txtEstadoArroz
        )

        actualizarEstado(
            "Frijol",
            binding.txtEstadoFrijol
        )

        actualizarEstado(
            "Pollo",
            binding.txtEstadoPollo
        )

        actualizarEstado(
            "Verduras",
            binding.txtEstadoVerduras
        )

        actualizarEstado(
            "Aceite",
            binding.txtEstadoAceite
        )

        actualizarEstado(
            "Leche",
            binding.txtEstadoLeche
        )
    }

    // =========================================================
    // ESTADO INDIVIDUAL
    // =========================================================

    private fun actualizarEstado(
        nombre: String,
        textoEstado: android.widget.TextView
    ) {

        if (alimentosSeleccionados.contains(nombre)) {

            textoEstado.text =
                "Seleccionado"

            textoEstado.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.blanco
                )
            )

            textoEstado.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.amarillo_principal
                )
            )

        } else {

            textoEstado.text =
                "Disponible"

            textoEstado.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.negro_principal
                )
            )

            textoEstado.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.gris_claro
                )
            )
        }
    }

    // =========================================================
    // MENSAJE INFORMATIVO
    // =========================================================

    private fun actualizarMensaje() {

        val cantidad =
            alimentosSeleccionados.size

        binding.txtMensajeSeleccion.text =

            if (cantidad == 0) {

                "Selecciona los alimentos que deseas considerar para generar las opciones de menú."

            } else {

                "$cantidad alimento(s) seleccionado(s). Pulsa Continuar para generar las opciones de menú."
            }
    }

    // =========================================================
    // CONTINUAR
    // =========================================================

    private fun configurarContinuar() {

        binding.btnContinuar.setOnClickListener {

            continuar()
        }
    }

    // =========================================================
    // CONTINUAR A LA SIGUIENTE PANTALLA
    // =========================================================

    private fun continuar() {

        if (alimentosSeleccionados.isEmpty()) {

            Toast.makeText(
                this,
                "Selecciona al menos un alimento.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Guardamos los alimentos seleccionados en el InventarioManager
        for (alimento in alimentosSeleccionados) {
            InventarioManager.guardarCantidad(alimento, 10.0, "MANANA")
        }

        Toast.makeText(
            this,
            "Alimentos guardados en el inventario.",
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent()
        intent.putExtra("ALIMENTOS_SELECCIONADOS", alimentosSeleccionados.joinToString(","))
        setResult(RESULT_OK, intent)
        finish()
    }

    // =========================================================
    // AL VOLVER A LA PANTALLA
    // =========================================================

    override fun onResume() {

        super.onResume()

        actualizarEstadosVisuales()

        actualizarMensaje()
    }
}
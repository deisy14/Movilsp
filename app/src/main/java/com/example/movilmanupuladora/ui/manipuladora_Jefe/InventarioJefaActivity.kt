package com.example.movilmanupuladora.ui.manipuladora_Jefe

import com.example.movilmanupuladora.R

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.movilmanupuladora.databinding.ActivityInventarioJefaBinding

class InventarioJefaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInventarioJefaBinding

    // =========================================================
    // JORNADA
    // =========================================================

    private var jornadaActual = "MANANA"


    // =========================================================
    // ALIMENTOS DEL INVENTARIO
    // =========================================================

    private val alimentos =
        mutableListOf<AlimentoInventario>()


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityInventarioJefaBinding.inflate(layoutInflater)

        setContentView(binding.root)

        cargarInventarioInicial()

        configurarJornada()

        configurarBotones()

        configurarNavegacionInferior()

        actualizarPantalla()
    }


    // =========================================================
    // ON RESUME
    // =========================================================

    override fun onResume() {
        super.onResume()

        /*
         * Cuando regresamos desde RegistrarEntregaActivity,
         * actualizamos nuevamente la información almacenada.
         */

        actualizarDatosDesdeEntrega()

        actualizarPantalla()
    }


    // =========================================================
    // INVENTARIO INICIAL
    // =========================================================

    private fun cargarInventarioInicial() {

        alimentos.clear()

        alimentos.add(
            AlimentoInventario(
                id = 1,
                nombre = "Arroz",
                cantidad = 0.0,
                unidad = "kg"
            )
        )

        alimentos.add(
            AlimentoInventario(
                id = 2,
                nombre = "Pollo",
                cantidad = 0.0,
                unidad = "kg"
            )
        )

        alimentos.add(
            AlimentoInventario(
                id = 3,
                nombre = "Lentejas",
                cantidad = 0.0,
                unidad = "kg"
            )
        )

        alimentos.add(
            AlimentoInventario(
                id = 4,
                nombre = "Pasta",
                cantidad = 0.0,
                unidad = "kg"
            )
        )

        alimentos.add(
            AlimentoInventario(
                id = 5,
                nombre = "Verduras",
                cantidad = 0.0,
                unidad = "kg"
            )
        )
    }


    // =========================================================
    // CONFIGURAR JORNADA
    // =========================================================

    private fun configurarJornada() {

        binding.btnManana.setOnClickListener {

            jornadaActual = "MANANA"

            actualizarPantalla()
        }


        binding.btnTarde.setOnClickListener {

            jornadaActual = "TARDE"

            actualizarPantalla()
        }
    }


    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        // -----------------------------------------------------
        // REGISTRAR ENTREGA
        // -----------------------------------------------------

        binding.btnRegistrarEntrega.setOnClickListener {

            abrirRegistrarEntrega()
        }


        // -----------------------------------------------------
        // SUGERENCIA IA
        // -----------------------------------------------------

        binding.btnGenerarIA.setOnClickListener {

            generarSugerenciaIA()
        }


        // -----------------------------------------------------
        // MOVIMIENTOS
        // -----------------------------------------------------

        binding.btnMovimientos.setOnClickListener {

            mostrarMovimientos()
        }


        // -----------------------------------------------------
        // VOLVER
        // -----------------------------------------------------

        binding.btnVolver.setOnClickListener {

            finish()
        }
    }


    // =========================================================
    // ACTUALIZAR PANTALLA
    // =========================================================

    private fun actualizarPantalla() {

        actualizarBotonesJornada()

        actualizarResumenInventario()
    }


    // =========================================================
    // ACTUALIZAR BOTONES DE JORNADA
    // =========================================================

    private fun actualizarBotonesJornada() {

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


        if (jornadaActual == "MANANA") {

            // MAÑANA SELECCIONADA

            binding.btnManana.setBackgroundColor(
                amarillo
            )

            binding.btnManana.setTextColor(
                blanco
            )


            // TARDE NO SELECCIONADA

            binding.btnTarde.setBackgroundColor(
                gris
            )

            binding.btnTarde.setTextColor(
                negro
            )

        } else {

            // TARDE SELECCIONADA

            binding.btnTarde.setBackgroundColor(
                amarillo
            )

            binding.btnTarde.setTextColor(
                blanco
            )


            // MAÑANA NO SELECCIONADA

            binding.btnManana.setBackgroundColor(
                gris
            )

            binding.btnManana.setTextColor(
                negro
            )
        }
    }


    // =========================================================
    // RESUMEN DEL INVENTARIO
    // =========================================================

    private fun actualizarResumenInventario() {

        val alimentosRegistrados =
            alimentos.count {
                it.cantidad > 0
            }


        binding.txtCantidadAlimentos.text =
            "$alimentosRegistrados alimentos registrados"


        val totalKg =
            alimentos.sumOf {
                it.cantidad
            }


        binding.txtTotalInventario.text =
            String.format(
                "%.1f kg disponibles",
                totalKg
            )


        if (alimentosRegistrados == 0) {

            binding.txtEstadoInventario.text =
                "Pendiente de registrar"

            binding.txtEstadoInventario.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.rojo_principal
                )
            )

        } else {

            binding.txtEstadoInventario.text =
                "Inventario registrado"

            binding.txtEstadoInventario.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.amarillo_principal
                )
            )
        }
    }


    // =========================================================
    // ABRIR REGISTRAR ENTREGA
    // =========================================================

    private fun abrirRegistrarEntrega() {

        val intent =
            Intent(
                this,
                RegistrarEntregaActivity::class.java
            )


        intent.putExtra(
            "jornada",
            jornadaActual
        )


        startActivityForResult(
            intent,
            REQUEST_REGISTRAR_ENTREGA
        )
    }


    // =========================================================
    // RESULTADO DE REGISTRAR ENTREGA
    // =========================================================

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )


        if (
            requestCode ==
            REQUEST_REGISTRAR_ENTREGA
        ) {

            if (
                resultCode ==
                RESULT_OK
            ) {

                actualizarDatosDesdeEntrega()

                actualizarPantalla()


                Toast.makeText(
                    this,
                    "Entrega registrada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // ACTUALIZAR DATOS DESDE ENTREGA
    // =========================================================

    private fun actualizarDatosDesdeEntrega() {

        actualizarCantidad(
            "Arroz",
            InventarioManager.obtenerCantidad("Arroz")
        )


        actualizarCantidad(
            "Pollo",
            InventarioManager.obtenerCantidad("Pollo")
        )


        actualizarCantidad(
            "Lentejas",
            InventarioManager.obtenerCantidad("Lentejas")
        )


        actualizarCantidad(
            "Pasta",
            InventarioManager.obtenerCantidad("Pasta")
        )


        actualizarCantidad(
            "Verduras",
            InventarioManager.obtenerCantidad("Verduras")
        )
    }


    // =========================================================
    // ACTUALIZAR CANTIDAD
    // =========================================================

    private fun actualizarCantidad(
        nombre: String,
        cantidad: Double
    ) {

        val alimento =
            alimentos.find {
                it.nombre == nombre
            }


        alimento?.cantidad =
            cantidad
    }


    // =========================================================
    // OBTENER ASISTENCIA DE LA JORNADA
    // =========================================================

    private fun obtenerAsistenciaActual(): Int {

        return if (
            jornadaActual == "MANANA"
        ) {

            AsistenciaManager.obtenerManana(
                this
            )

        } else {

            AsistenciaManager.obtenerTarde(
                this
            )
        }
    }


    // =========================================================
    // GENERAR SUGERENCIA DE IA
    // =========================================================

    private fun generarSugerenciaIA() {

        // -----------------------------------------------------
        // ASISTENCIA
        // -----------------------------------------------------

        val cantidadNinos =
            obtenerAsistenciaActual()


        // -----------------------------------------------------
        // INVENTARIO
        // -----------------------------------------------------

        val tieneArroz =
            InventarioManager.obtenerCantidad(
                "Arroz"
            ) > 0


        val tienePollo =
            InventarioManager.obtenerCantidad(
                "Pollo"
            ) > 0


        val tieneLentejas =
            InventarioManager.obtenerCantidad(
                "Lentejas"
            ) > 0


        val tienePasta =
            InventarioManager.obtenerCantidad(
                "Pasta"
            ) > 0


        val tieneVerduras =
            InventarioManager.obtenerCantidad(
                "Verduras"
            ) > 0


        // -----------------------------------------------------
        // VALIDAR ASISTENCIA
        // -----------------------------------------------------

        if (cantidadNinos <= 0) {

            AlertDialog.Builder(this)

                .setTitle(
                    "No se puede generar sugerencia"
                )

                .setMessage(
                    "Primero registra la asistencia " +
                            "de la jornada de " +
                            obtenerNombreJornada() +
                            "."
                )

                .setPositiveButton(
                    "Entendido",
                    null
                )

                .show()

            return
        }


        // -----------------------------------------------------
        // GENERAR SUGERENCIA
        // -----------------------------------------------------

        val mensaje: String


        if (
            tieneArroz &&
            tienePollo
        ) {

            mensaje =
                "Con los alimentos disponibles y " +
                        "la asistencia registrada, " +
                        "la IA puede sugerir:\n\n" +
                        "🍗 Arroz con pollo\n\n" +
                        "Asistencia: " +
                        "$cantidadNinos niños."


        } else if (
            tieneLentejas &&
            tieneArroz
        ) {

            mensaje =
                "Con los alimentos disponibles y " +
                        "la asistencia registrada, " +
                        "la IA puede sugerir:\n\n" +
                        "🥣 Lentejas con arroz\n\n" +
                        "Asistencia: " +
                        "$cantidadNinos niños."


        } else if (
            tienePasta &&
            tieneVerduras
        ) {

            mensaje =
                "Con los alimentos disponibles y " +
                        "la asistencia registrada, " +
                        "la IA puede sugerir:\n\n" +
                        "🍝 Pasta con verduras\n\n" +
                        "Asistencia: " +
                        "$cantidadNinos niños."


        } else if (
            tienePasta
        ) {

            mensaje =
                "Con la pasta registrada, la IA " +
                        "puede considerar una preparación " +
                        "con pasta.\n\n" +
                        "Asistencia: " +
                        "$cantidadNinos niños.\n\n" +
                        "Se requieren más alimentos " +
                        "para completar la sugerencia."


        } else {

            mensaje =
                "No hay una combinación suficiente " +
                        "de alimentos registrados para " +
                        "generar una sugerencia.\n\n" +
                        "Asistencia: " +
                        "$cantidadNinos niños."
        }


        // -----------------------------------------------------
        // MOSTRAR RESULTADO
        // -----------------------------------------------------

        AlertDialog.Builder(this)

            .setTitle(
                "Sugerencia de IA"
            )

            .setMessage(
                mensaje
            )

            .setPositiveButton(
                "Entendido",
                null
            )

            .show()
    }


    // =========================================================
    // NOMBRE DE LA JORNADA
    // =========================================================

    private fun obtenerNombreJornada(): String {

        return if (
            jornadaActual == "MANANA"
        ) {

            "mañana"

        } else {

            "tarde"
        }
    }


    // =========================================================
    // MOVIMIENTOS ALIMENTARIOS
    // =========================================================

    private fun mostrarMovimientos() {

        val movimientos =
            InventarioManager.obtenerMovimientos()


        if (movimientos.isEmpty()) {

            AlertDialog.Builder(this)

                .setTitle(
                    "Movimientos alimentarios"
                )

                .setMessage(
                    "Todavía no hay movimientos " +
                            "registrados."
                )

                .setPositiveButton(
                    "Aceptar",
                    null
                )

                .show()

            return
        }


        val texto =
            movimientos.joinToString(
                separator = "\n\n"
            )


        AlertDialog.Builder(this)

            .setTitle(
                "Movimientos alimentarios"
            )

            .setMessage(
                texto
            )

            .setPositiveButton(
                "Cerrar",
                null
            )

            .show()
    }


    // =========================================================
    // COMPANION OBJECT
    // =========================================================

    companion object {

        private const val REQUEST_REGISTRAR_ENTREGA =
            1001
    }
}
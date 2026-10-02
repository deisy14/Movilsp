package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.movilmanupuladora.databinding.ActivityManipuladorasAsignadasBinding

class ManipuladorasAsignadasActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManipuladorasAsignadasBinding

    private val manipuladoras = mutableListOf<Manipuladora>()

    private var jornada = "Mañana"

    data class Manipuladora(
        val nombre: String,
        val rol: String = "Manipuladora de alimentos",
        var estado: String = "Asignada"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // =====================================================
        // VIEW BINDING
        // =====================================================

        binding = ActivityManipuladorasAsignadasBinding.inflate(
            layoutInflater
        )

        setContentView(binding.root)

        // =====================================================
        // INICIALIZAR
        // =====================================================

        obtenerDatos()
        cargarManipuladoras()
        configurarEventos()
        actualizarInterfaz()
    }

    // =========================================================
    // OBTENER DATOS
    // =========================================================

    private fun obtenerDatos() {

        jornada = intent.getStringExtra("jornada")
            ?: "Mañana"

        binding.txtJornada.text =
            "Jornada: $jornada"
    }

    // =========================================================
    // DATOS INICIALES
    // =========================================================

    private fun cargarManipuladoras() {

        manipuladoras.clear()

        manipuladoras.add(
            Manipuladora(
                nombre = "María González"
            )
        )

        manipuladoras.add(
            Manipuladora(
                nombre = "Laura Martínez"
            )
        )

        manipuladoras.add(
            Manipuladora(
                nombre = "Ana Rodríguez"
            )
        )
    }

    // =========================================================
    // EVENTOS
    // =========================================================

    private fun configurarEventos() {

        // VOLVER
        binding.btnCerrar.setOnClickListener {
            finish()
        }

        // AGREGAR
        binding.btnAgregarManipuladora.setOnClickListener {
            mostrarDialogoAgregar()
        }

        // CONFIRMAR
        binding.btnConfirmarAsignacion.setOnClickListener {
            confirmarAsignacion()
        }

        // CANCELAR
        binding.btnCancelar.setOnClickListener {
            confirmarCancelar()
        }

        // MANIPULADORA 1
        binding.cardManipuladora1.setOnClickListener {

            if (manipuladoras.isNotEmpty()) {
                mostrarOpciones(
                    manipuladoras[0]
                )
            }
        }

        // MANIPULADORA 2
        binding.cardManipuladora2.setOnClickListener {

            if (manipuladoras.size > 1) {
                mostrarOpciones(
                    manipuladoras[1]
                )
            }
        }

        // MANIPULADORA 3
        binding.cardManipuladora3.setOnClickListener {

            if (manipuladoras.size > 2) {
                mostrarOpciones(
                    manipuladoras[2]
                )
            }
        }
    }

    // =========================================================
    // ACTUALIZAR INTERFAZ
    // =========================================================

    private fun actualizarInterfaz() {

        binding.txtCantidadManipuladoras.text =
            "${manipuladoras.size} manipuladoras"

        actualizarTarjeta(
            posicion = 0
        )

        actualizarTarjeta(
            posicion = 1
        )

        actualizarTarjeta(
            posicion = 2
        )
    }

    // =========================================================
    // ACTUALIZAR TARJETAS
    // =========================================================

    private fun actualizarTarjeta(
        posicion: Int
    ) {

        when (posicion) {

            0 -> {

                if (manipuladoras.size > 0) {

                    val persona =
                        manipuladoras[0]

                    binding.cardManipuladora1.visibility =
                        android.view.View.VISIBLE

                    binding.txtNombreManipuladora1.text =
                        persona.nombre

                    binding.txtRolManipuladora1.text =
                        persona.rol

                    binding.txtEstadoManipuladora1.text =
                        persona.estado

                } else {

                    binding.cardManipuladora1.visibility =
                        android.view.View.GONE
                }
            }

            1 -> {

                if (manipuladoras.size > 1) {

                    val persona =
                        manipuladoras[1]

                    binding.cardManipuladora2.visibility =
                        android.view.View.VISIBLE

                    binding.txtNombreManipuladora2.text =
                        persona.nombre

                    binding.txtRolManipuladora2.text =
                        persona.rol

                    binding.txtEstadoManipuladora2.text =
                        persona.estado

                } else {

                    binding.cardManipuladora2.visibility =
                        android.view.View.GONE
                }
            }

            2 -> {

                if (manipuladoras.size > 2) {

                    val persona =
                        manipuladoras[2]

                    binding.cardManipuladora3.visibility =
                        android.view.View.VISIBLE

                    binding.txtNombreManipuladora3.text =
                        persona.nombre

                    binding.txtRolManipuladora3.text =
                        persona.rol

                    binding.txtEstadoManipuladora3.text =
                        persona.estado

                } else {

                    binding.cardManipuladora3.visibility =
                        android.view.View.GONE
                }
            }
        }
    }

    // =========================================================
    // AGREGAR MANIPULADORA
    // =========================================================

    private fun mostrarDialogoAgregar() {

        if (manipuladoras.size >= 3) {

            Toast.makeText(
                this,
                "Máximo 3 manipuladoras",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val nombres = arrayOf(
            "Carolina Pérez",
            "Sofía Ramírez",
            "Daniela López",
            "Paula Herrera",
            "Camila Torres"
        )

        AlertDialog.Builder(this)
            .setTitle("Seleccionar manipuladora")
            .setItems(nombres) { _, posicion ->

                val nueva =
                    Manipuladora(
                        nombre = nombres[posicion]
                    )

                manipuladoras.add(nueva)

                actualizarInterfaz()

                Toast.makeText(
                    this,
                    "${nueva.nombre} agregada",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(
                "Cancelar",
                null
            )
            .show()
    }

    // =========================================================
    // OPCIONES
    // =========================================================

    private fun mostrarOpciones(
        manipuladora: Manipuladora
    ) {

        val opciones = arrayOf(
            "Cambiar estado",
            "Eliminar"
        )

        AlertDialog.Builder(this)
            .setTitle(manipuladora.nombre)
            .setItems(opciones) { _, posicion ->

                when (posicion) {

                    0 -> cambiarEstado(
                        manipuladora
                    )

                    1 -> eliminar(
                        manipuladora
                    )
                }
            }
            .show()
    }

    // =========================================================
    // CAMBIAR ESTADO
    // =========================================================

    private fun cambiarEstado(
        manipuladora: Manipuladora
    ) {

        val estados = arrayOf(
            "Asignada",
            "Presente",
            "En preparación",
            "No disponible"
        )

        AlertDialog.Builder(this)
            .setTitle("Cambiar estado")
            .setItems(estados) { _, posicion ->

                manipuladora.estado =
                    estados[posicion]

                actualizarInterfaz()
            }
            .show()
    }

    // =========================================================
    // ELIMINAR
    // =========================================================

    private fun eliminar(
        manipuladora: Manipuladora
    ) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar manipuladora")
            .setMessage(
                "¿Deseas eliminar a ${manipuladora.nombre}?"
            )
            .setPositiveButton("Eliminar") { _, _ ->

                manipuladoras.remove(
                    manipuladora
                )

                actualizarInterfaz()

                Toast.makeText(
                    this,
                    "Manipuladora eliminada",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(
                "Cancelar",
                null
            )
            .show()
    }

    // =========================================================
    // CONFIRMAR ASIGNACIÓN
    // =========================================================

    private fun confirmarAsignacion() {

        if (manipuladoras.isEmpty()) {

            Toast.makeText(
                this,
                "Debes asignar al menos una manipuladora",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar asignación")
            .setMessage(
                "Se asignarán ${manipuladoras.size} " +
                        "manipuladoras para la jornada $jornada."
            )
            .setPositiveButton("Confirmar") { _, _ ->

                guardarAsignacion()
            }
            .setNegativeButton(
                "Revisar",
                null
            )
            .show()
    }

    // =========================================================
    // GUARDAR
    // =========================================================

    private fun guardarAsignacion() {

        val preferencias =
            getSharedPreferences(
                "ManipuladoraJefe",
                MODE_PRIVATE
            )

        preferencias.edit()
            .putString(
                "jornada",
                jornada
            )
            .putInt(
                "cantidad_manipuladoras",
                manipuladoras.size
            )
            .apply()

        Toast.makeText(
            this,
            "Asignación confirmada",
            Toast.LENGTH_LONG
        ).show()

        setResult(RESULT_OK)

        finish()
    }

    // =========================================================
    // CANCELAR
    // =========================================================

    private fun confirmarCancelar() {

        AlertDialog.Builder(this)
            .setTitle("Cancelar")
            .setMessage(
                "¿Seguro que deseas cancelar?"
            )
            .setPositiveButton(
                "Sí, cancelar"
            ) { _, _ ->

                finish()
            }
            .setNegativeButton(
                "Continuar",
                null
            )
            .show()
    }
}
package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.manipuladorajefe.DetallePreparacionActivity
import com.example.movilmanupuladora.databinding.ActivityHistorialPreparacionesBinding
import com.example.movilmanupuladora.R

class HistorialPreparacionesActivity : AppCompatActivity() {

    // =========================================================
    // BINDING
    // =========================================================

    private lateinit var binding: ActivityHistorialPreparacionesBinding


    // =========================================================
    // FILTRO ACTUAL
    // =========================================================

    private var filtroActual = "TODAS"


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityHistorialPreparacionesBinding.inflate(layoutInflater)

        setContentView(binding.root)

        configurarInsets()

        configurarBotones()

        configurarNavegacionInferior()

        actualizarInterfaz()
    }


    // =========================================================
    // INSETS
    // =========================================================

    private fun configurarInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { vista, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                )

            vista.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }


    // =========================================================
    // CONFIGURAR BOTONES
    // =========================================================

    private fun configurarBotones() {

        // -----------------------------------------------------
        // VOLVER
        // -----------------------------------------------------

        binding.btnCerrar.setOnClickListener {

            finish()
        }


        // -----------------------------------------------------
        // TODAS
        // -----------------------------------------------------

        binding.btnTodas.setOnClickListener {

            filtroActual = "TODAS"

            actualizarInterfaz()
        }


        // -----------------------------------------------------
        // MAÑANA
        // -----------------------------------------------------

        binding.btnManana.setOnClickListener {

            filtroActual = "MANANA"

            actualizarInterfaz()
        }


        // -----------------------------------------------------
        // TARDE
        // -----------------------------------------------------

        binding.btnTarde.setOnClickListener {

            filtroActual = "TARDE"

            actualizarInterfaz()
        }


        // -----------------------------------------------------
        // VER DETALLE 1
        // -----------------------------------------------------

        binding.btnVer1.setOnClickListener {

            abrirDetalle(
                fecha = binding.txtFecha1.text.toString(),
                plato = binding.txtPlato1.text.toString(),
                jornada = binding.txtJornada1.text.toString(),
                ninos = binding.txtNinos1.text.toString()
            )
        }


        // -----------------------------------------------------
        // VER DETALLE 2
        // -----------------------------------------------------

        binding.btnVer2.setOnClickListener {

            abrirDetalle(
                fecha = binding.txtFecha2.text.toString(),
                plato = binding.txtPlato2.text.toString(),
                jornada = binding.txtJornada2.text.toString(),
                ninos = binding.txtNinos2.text.toString()
            )
        }
    }


    // =========================================================
    // ACTUALIZAR INTERFAZ
    // =========================================================

    private fun actualizarInterfaz() {

        actualizarBotonesFiltro()

        aplicarFiltro()
    }


    // =========================================================
    // BOTONES DE FILTRO
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


        // =====================================================
        // TODAS
        // =====================================================

        if (filtroActual == "TODAS") {

            binding.btnTodas.backgroundTintList =
                ColorStateList.valueOf(amarillo)

            binding.btnTodas.setTextColor(
                blanco
            )

        } else {

            binding.btnTodas.backgroundTintList =
                ColorStateList.valueOf(gris)

            binding.btnTodas.setTextColor(
                negro
            )
        }


        // =====================================================
        // MAÑANA
        // =====================================================

        if (filtroActual == "MANANA") {

            binding.btnManana.backgroundTintList =
                ColorStateList.valueOf(amarillo)

            binding.btnManana.setTextColor(
                blanco
            )

        } else {

            binding.btnManana.backgroundTintList =
                ColorStateList.valueOf(gris)

            binding.btnManana.setTextColor(
                negro
            )
        }


        // =====================================================
        // TARDE
        // =====================================================

        if (filtroActual == "TARDE") {

            binding.btnTarde.backgroundTintList =
                ColorStateList.valueOf(amarillo)

            binding.btnTarde.setTextColor(
                blanco
            )

        } else {

            binding.btnTarde.backgroundTintList =
                ColorStateList.valueOf(gris)

            binding.btnTarde.setTextColor(
                negro
            )
        }
    }


    // =========================================================
    // APLICAR FILTRO
    // =========================================================

    private fun aplicarFiltro() {

        when (filtroActual) {

            "TODAS" -> {

                binding.cardPreparacion1.visibility =
                    android.view.View.VISIBLE

                binding.cardPreparacion2.visibility =
                    android.view.View.VISIBLE

                binding.contenedorVacio.visibility =
                    android.view.View.GONE
            }


            "MANANA" -> {

                binding.cardPreparacion1.visibility =
                    android.view.View.VISIBLE

                binding.cardPreparacion2.visibility =
                    android.view.View.GONE

                binding.contenedorVacio.visibility =
                    android.view.View.GONE
            }


            "TARDE" -> {

                binding.cardPreparacion1.visibility =
                    android.view.View.GONE

                binding.cardPreparacion2.visibility =
                    android.view.View.VISIBLE

                binding.contenedorVacio.visibility =
                    android.view.View.GONE
            }
        }
    }


    // =========================================================
    // ABRIR DETALLE
    // =========================================================

    private fun abrirDetalle(
        fecha: String,
        plato: String,
        jornada: String,
        ninos: String
    ) {

        val intent =
            Intent(
                this,
                DetallePreparacionActivity::class.java
            )


        intent.putExtra(
            "fecha",
            fecha
        )

        intent.putExtra(
            "plato",
            plato
        )

        intent.putExtra(
            "jornada",
            jornada
        )

        intent.putExtra(
            "ninos",
            ninos
        )


        startActivity(intent)
    }
}
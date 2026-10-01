package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.manipuladorajefe.InventarioManager
import com.example.movilmanupuladora.databinding.ActivityRegistrarEntregaBinding


class RegistrarEntregaActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityRegistrarEntregaBinding

    private var jornadaActual =
        "MANANA"

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityRegistrarEntregaBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        jornadaActual =
            intent.getStringExtra(
                "jornada"
            ) ?: "MANANA"

        configurarPantalla()

        configurarBotones()
    }

    // =========================================================
    // PANTALLA
    // =========================================================

    private fun configurarPantalla() {

        binding.txtJornada.text =
            if (jornadaActual == "MANANA") {
                "Jornada: Mañana"
            } else {
                "Jornada: Tarde"
            }
    }

    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        binding.btnCancelar.setOnClickListener {

            finish()
        }

        binding.btnGuardar.setOnClickListener {

            guardarEntrega()
        }
    }

    // =========================================================
    // GUARDAR ENTREGA
    // =========================================================

    private fun guardarEntrega() {

        val arroz =
            obtenerNumero(
                binding.edtArroz.text.toString()
            )

        val pollo =
            obtenerNumero(
                binding.edtPollo.text.toString()
            )

        val lentejas =
            obtenerNumero(
                binding.edtLentejas.text.toString()
            )

        val pasta =
            obtenerNumero(
                binding.edtPasta.text.toString()
            )

        val verduras =
            obtenerNumero(
                binding.edtVerduras.text.toString()
            )

        val hayAlimento =
            arroz > 0 ||
                    pollo > 0 ||
                    lentejas > 0 ||
                    pasta > 0 ||
                    verduras > 0

        if (!hayAlimento) {

            Toast.makeText(
                this,
                "Registra al menos un alimento.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        InventarioManager.guardarCantidad(
            alimento = "Arroz",
            cantidad = arroz,
            jornada = jornadaActual
        )

        InventarioManager.guardarCantidad(
            alimento = "Pollo",
            cantidad = pollo,
            jornada = jornadaActual
        )

        InventarioManager.guardarCantidad(
            alimento = "Lentejas",
            cantidad = lentejas,
            jornada = jornadaActual
        )

        InventarioManager.guardarCantidad(
            alimento = "Pasta",
            cantidad = pasta,
            jornada = jornadaActual
        )

        InventarioManager.guardarCantidad(
            alimento = "Verduras",
            cantidad = verduras,
            jornada = jornadaActual
        )

        Toast.makeText(
            this,
            "Entrega registrada correctamente.",
            Toast.LENGTH_SHORT
        ).show()

        setResult(
            RESULT_OK
        )

        finish()
    }

    // =========================================================
    // CONVERTIR TEXTO A NUMERO
    // =========================================================

    private fun obtenerNumero(
        texto: String
    ): Double {

        return texto
            .replace(",", ".")
            .toDoubleOrNull()
            ?: 0.0
    }
}
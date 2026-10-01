package com.example.movilmanupuladora.ui.manipuladora_Jefe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IniciarPreparacionActivity : AppCompatActivity() {

    private lateinit var txtEstadoPreparacion: TextView
    private lateinit var txtCantidadNinos: TextView
    private lateinit var txtCantidadManipuladoras: TextView
    private lateinit var edtObservaciones: EditText
    private lateinit var btnIniciarPreparacion: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnCerrar: ImageButton
    private lateinit var btnVerManipuladoras: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_preparacion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardEstadoPreparacion)) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                v.paddingLeft,
                systemBars.top,
                v.paddingRight,
                v.paddingBottom
            )

            insets
        }

        inicializarVistas()
        cargarDatos()
        configurarBotones()
    }

    private fun inicializarVistas() {

        txtEstadoPreparacion =
            findViewById(R.id.txtEstadoPreparacion)

        txtCantidadNinos =
            findViewById(R.id.txtCantidadNinos)

        txtCantidadManipuladoras =
            findViewById(R.id.txtCantidadManipuladoras)

        edtObservaciones =
            findViewById(R.id.edtObservaciones)

        btnIniciarPreparacion =
            findViewById(R.id.btnIniciarPreparacion)

        btnCancelar =
            findViewById(R.id.btnCancelar)

        btnCerrar =
            findViewById(R.id.btnCerrar)

        btnVerManipuladoras =
            findViewById(R.id.btnVerManipuladoras)
    }

    private fun cargarDatos() {

        /*
         * Datos iniciales.
         *
         * Posteriormente estos datos pueden venir
         * de una base de datos.
         */

        val cantidadNinos =
            intent.getIntExtra("cantidad_ninos", 35)

        val cantidadManipuladoras =
            intent.getIntExtra("cantidad_manipuladoras", 2)

        val nombreMenu =
            intent.getStringExtra("nombre_menu")
                ?: "Arroz con pollo"

        val descripcionMenu =
            intent.getStringExtra("descripcion_menu")
                ?: "Preparación principal de la jornada"

        val jornada =
            intent.getStringExtra("jornada")
                ?: "Mañana"

        findViewById<TextView>(R.id.txtJornada).text =
            "Jornada: $jornada"

        findViewById<TextView>(R.id.txtNombreMenu).text =
            nombreMenu

        findViewById<TextView>(R.id.txtDescripcionMenu).text =
            descripcionMenu

        txtCantidadNinos.text =
            "$cantidadNinos niños"

        txtCantidadManipuladoras.text =
            "$cantidadManipuladoras manipuladoras"

        txtEstadoPreparacion.text =
            "Pendiente"
    }

    private fun configurarBotones() {

        // BOTÓN VOLVER
        btnCerrar.setOnClickListener {
            finish()
        }

        // CANCELAR
        btnCancelar.setOnClickListener {
            finish()
        }

        // VER MANIPULADORAS
        btnVerManipuladoras.setOnClickListener {

            try {

                val intent = Intent(
                    this,
                    ManipuladorasAsignadasActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "No se pudo abrir la lista de manipuladoras",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // INICIAR PREPARACIÓN
        btnIniciarPreparacion.setOnClickListener {
            iniciarPreparacion()
        }
    }

    private fun iniciarPreparacion() {

        // Evitamos iniciar dos veces
        if (txtEstadoPreparacion.text.toString() == "En preparación") {

            Toast.makeText(
                this,
                "La preparación ya está en curso",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /*
         * Guardamos la hora en la que comenzó
         * la preparación.
         */

        val horaInicio = obtenerHoraActual()

        /*
         * Guardamos las observaciones escritas
         * por la manipuladora.
         */

        val observaciones =
            edtObservaciones.text.toString().trim()

        /*
         * Cambiamos el estado visual.
         */

        txtEstadoPreparacion.text =
            "En preparación"

        /*
         * Deshabilitamos el botón para evitar
         * iniciar varias veces la misma preparación.
         */

        btnIniciarPreparacion.isEnabled = false

        btnIniciarPreparacion.text =
            "Preparación iniciada"

        Toast.makeText(
            this,
            "Preparación iniciada a las $horaInicio",
            Toast.LENGTH_SHORT
        ).show()

        /*
         * Aquí dejamos preparados los datos
         * para enviarlos posteriormente a la base
         * de datos.
         */

        guardarPreparacion(
            horaInicio,
            observaciones
        )
    }

    private fun guardarPreparacion(
        horaInicio: String,
        observaciones: String
    ) {

        /*
         * Por ahora guardamos los datos localmente
         * utilizando SharedPreferences.
         *
         * Después podemos reemplazar esto por
         * SQLite, Room o Firebase.
         */

        val preferencias =
            getSharedPreferences(
                "preparaciones",
                MODE_PRIVATE
            )

        preferencias.edit()
            .putString(
                "estado_actual",
                "En preparación"
            )
            .putString(
                "hora_inicio",
                horaInicio
            )
            .putString(
                "observaciones",
                observaciones
            )
            .apply()
    }

    private fun obtenerHoraActual(): String {

        val formato =
            SimpleDateFormat(
                "hh:mm a",
                Locale("es", "CO")
            )

        return formato.format(Date())
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
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
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.repository.MenuRepository
import com.example.movilmanupuladora.ui.manipuladora.MainActivity
import kotlinx.coroutines.launch

class IniciarPreparacionActivity : AppCompatActivity() {

    private lateinit var txtEstadoPreparacion: TextView
    private lateinit var txtCantidadNinos: TextView
    private lateinit var txtCantidadManipuladoras: TextView
    private lateinit var edtObservaciones: EditText
    private lateinit var btnIniciarPreparacion: Button
    private lateinit var btnCancelar: Button
    private lateinit var btnCerrar: ImageButton

    private val menuRepository = MenuRepository(RetrofitClient.apiService)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_preparacion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardMenuDia)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        inicializarVistas()
        cargarDatos()
        configurarBotones()
    }

    private fun inicializarVistas() {
        txtEstadoPreparacion = findViewById(R.id.txtEstadoPreparacion)
        txtCantidadNinos = findViewById(R.id.txtCantidadNinos)
        txtCantidadManipuladoras = findViewById(R.id.txtCantidadManipuladoras)
        edtObservaciones = findViewById(R.id.edtObservaciones)
        btnIniciarPreparacion = findViewById(R.id.btnIniciarPreparacion)
        btnCancelar = findViewById(R.id.btnCancelar)
        btnCerrar = findViewById(R.id.btnCerrar)
    }

    private fun cargarDatos() {
        val nombreMenuIntent = intent.getStringExtra("nombre_menu") ?: "Arroz con Pollo Criollo"
        findViewById<TextView>(R.id.txtNombreMenu)?.text = nombreMenuIntent

        val jornadaIntent = intent.getStringExtra("jornada") ?: "Mañana"
        findViewById<TextView>(R.id.txtJornada)?.text = "Jornada: $jornadaIntent"

        val p = AsistenciaManager.obtenerPrimaria(this)
        val s = AsistenciaManager.obtenerSecundaria(this)
        val suma = p + s
        val guardados = if (suma > 0) {
            suma
        } else if (jornadaIntent.contains("Mañana", ignoreCase = true)) {
            AsistenciaManager.obtenerManana(this)
        } else {
            AsistenciaManager.obtenerTarde(this)
        }

        val ninos = if (guardados > 0) guardados else 0
        txtCantidadNinos.text = "$ninos niños"
        txtCantidadManipuladoras.text = "2 operarias"
    }

    private fun configurarBotones() {
        btnCerrar.setOnClickListener { finish() }
        btnCancelar.setOnClickListener { finish() }

        btnIniciarPreparacion.setOnClickListener {
            val obs = edtObservaciones.text.toString().trim()
            val preferencias = getSharedPreferences("SIRAE", MODE_PRIVATE)
            preferencias.edit().putString("observaciones_preparacion", obs).apply()
            preferencias.edit().putString("estado_plato", "EN_PREPARACION").apply()

            Toast.makeText(this, "¡Preparación del menú iniciada exitosamente!", Toast.LENGTH_SHORT).show()

            val intentMain = Intent(this, MainActivity::class.java)
            intentMain.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intentMain)
            finish()
        }
    }
}
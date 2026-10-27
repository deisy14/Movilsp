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

    private var nombreMenuIntent: String = "Arroz con Pollo Criollo"
    private var jornadaIntent: String = "Mañana"
    private var idPlatoIntent: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_iniciar_preparacion)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cardMenuDia)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        inicializarVistas()
        cargarDatos()
        configurarBotones()
    }

    override fun onResume() {
        super.onResume()
        actualizarInfoEquipo()
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
        nombreMenuIntent = intent.getStringExtra("nombre_menu") ?: "Arroz con Pollo Criollo"
        findViewById<TextView>(R.id.txtNombreMenu)?.text = nombreMenuIntent

        jornadaIntent = intent.getStringExtra("jornada") ?: "Mañana"
        idPlatoIntent = intent.getIntExtra("id_plato", -1)
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
        actualizarInfoEquipo()
    }

    private fun actualizarInfoEquipo() {
        val prefs = getSharedPreferences("ManipuladoraJefe", MODE_PRIVATE)
        val cant = prefs.getInt("cantidad_manipuladoras", 3)
        txtCantidadManipuladoras.text = "$cant operarias"
    }

    private fun configurarBotones() {
        btnCerrar.setOnClickListener { finish() }
        btnCancelar.setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.cardEquipo)?.setOnClickListener {
            val intentEquipo = Intent(this, ManipuladorasAsignadasActivity::class.java).apply {
                putExtra("nombre_menu", nombreMenuIntent)
                putExtra("id_plato", idPlatoIntent)
                putExtra("jornada", jornadaIntent)
            }
            startActivity(intentEquipo)
        }

        btnIniciarPreparacion.setOnClickListener {
            val obs = edtObservaciones.text.toString().trim()
            val preferencias = getSharedPreferences("SIRAE_JEFA", MODE_PRIVATE)
            preferencias.edit().putString("observaciones_preparacion", obs).apply()
            preferencias.edit().putString("estado_plato", "EN_PREPARACION").apply()

            // Guardar plato aprobado para que las manipuladoras lo vean al ingresar
            val jefaPrefs = getSharedPreferences("ManipuladoraJefe", MODE_PRIVATE)
            jefaPrefs.edit().putString("plato_actual", nombreMenuIntent).apply()

            val prefTurno = getSharedPreferences("TURNO_PAE", MODE_PRIVATE)
            prefTurno.edit()
                .putString("plato_nombre", nombreMenuIntent)
                .putString("plato_jornada", jornadaIntent)
                .putInt("plato_id", idPlatoIntent)
                .apply()

            com.example.movilmanupuladora.utils.SessionManager(this)
                .savePlatoSeleccionado(nombreMenuIntent, idPlatoIntent)

            lifecycleScope.launch {
                com.example.movilmanupuladora.utils.PreparacionSyncManager.guardarPlatoSeleccionado(
                    this@IniciarPreparacionActivity,
                    nombreMenuIntent,
                    idPlatoIntent,
                    jornadaIntent
                )
            }

            Toast.makeText(this, "¡Preparación del menú iniciada exitosamente!", Toast.LENGTH_SHORT).show()

            val intentJefa = Intent(this, InicioJefaActivity::class.java)
            intentJefa.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intentJefa)
        }
    }
}
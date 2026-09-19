package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.MainActivity
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.data.api.RetrofitClient
import com.example.movilmanupuladora.data.model.pasos_preparacion
import kotlinx.coroutines.launch

class PreparacionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_preparacion)

        val btnVolver = findViewById<ImageView>(R.id.btnVolver)
        btnVolver?.setOnClickListener {
            finish()
        }

        val btnMarcarPreparado = findViewById<Button>(R.id.btnMarcarPreparado)
        val preferencias = getSharedPreferences("SIRAE", MODE_PRIVATE)
        val preparado = preferencias.getBoolean("plato_preparado", false)

        if (preparado && btnMarcarPreparado != null) {
            btnMarcarPreparado.text = "✓   Preparado"
            btnMarcarPreparado.isEnabled = false
            btnMarcarPreparado.alpha = 0.6f
        }

        btnMarcarPreparado?.setOnClickListener {
            preferencias.edit()
                .putBoolean("plato_preparado", true)
                .apply()

            btnMarcarPreparado.text = "✓   Preparado"
            btnMarcarPreparado.isEnabled = false
            btnMarcarPreparado.alpha = 0.6f

            Toast.makeText(this, "¡Plato marcado como preparado!", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.navInicio)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navAsignadas)?.setOnClickListener {
            startActivity(Intent(this, ComponentesActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navInventario)?.setOnClickListener {
            startActivity(Intent(this, InventarioActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navAvisos)?.setOnClickListener {
            startActivity(Intent(this, AvisosActivity::class.java))
            finish()
        }

        findViewById<LinearLayout>(R.id.navPerfil)?.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
            finish()
        }

        cargarPasos()
    }

    private fun cargarPasos() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.obtenerPasosPreparacion()

                if (response.isSuccessful && response.body() != null) {
                    val listaPasos: List<pasos_preparacion> = response.body()!!

                    Toast.makeText(
                        this@PreparacionActivity,
                        "Pasos recibidos desde SIRAE: ${listaPasos.size}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@PreparacionActivity,
                        "Error HTTP: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PreparacionActivity,
                    "Error de conexión: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
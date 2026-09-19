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
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.MainActivity

class PreparacionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_preparacion)

        // ==========================================
        // INSETS DE LA PANTALLA
        // ==========================================

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->

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


        // ==========================================
        // BOTÓN VOLVER
        // ==========================================

        val btnVolver =
            findViewById<ImageView>(R.id.btnVolver)

        btnVolver.setOnClickListener {

            finish()
        }


        // ==========================================
        // BOTÓN MARCAR COMO PREPARADO
        // ==========================================

        val btnMarcarPreparado =
            findViewById<Button>(R.id.btnMarcarPreparado)


        // ==========================================
        // CARGAR ESTADO GUARDADO
        // ==========================================

        val preferencias =
            getSharedPreferences("SIRAE", MODE_PRIVATE)

        val preparado =
            preferencias.getBoolean("plato_preparado", false)


        if (preparado) {

            btnMarcarPreparado.text = "✓   Preparado"
            btnMarcarPreparado.isEnabled = false

            btnMarcarPreparado.alpha = 0.6f
        }


        // ==========================================
        // MARCAR PREPARADO
        // ==========================================

        btnMarcarPreparado.setOnClickListener {

            // Guardar estado
            preferencias.edit()
                .putBoolean("plato_preparado", true)
                .apply()


            // Cambiar apariencia
            btnMarcarPreparado.text = "✓   Preparado"

            btnMarcarPreparado.isEnabled = false

            btnMarcarPreparado.alpha = 0.6f


            // Mostrar mensaje
            Toast.makeText(
                this,
                "¡Plato marcado como preparado!",
                Toast.LENGTH_SHORT
            ).show()
        }


        // ==========================================
        // BARRA DE NAVEGACIÓN
        // ==========================================

        val navInicio =
            findViewById<LinearLayout>(R.id.navInicio)

        val navAsignadas =
            findViewById<LinearLayout>(R.id.navAsignadas)

        val navInventario =
            findViewById<LinearLayout>(R.id.navInventario)

        val navAvisos =
            findViewById<LinearLayout>(R.id.navAvisos)

        val navPerfil =
            findViewById<LinearLayout>(R.id.navPerfil)


        // ==========================================
        // INICIO
        // ==========================================

        navInicio.setOnClickListener {

            val intent =
                Intent(this, MainActivity::class.java)

            startActivity(intent)
            finish()
        }


        // ==========================================
        // ASIGNADAS
        // ==========================================

        navAsignadas.setOnClickListener {

            val intent =
                Intent(this, ComponentesActivity::class.java)

            startActivity(intent)
            finish()
        }


        // ==========================================
        // INVENTARIO
        // ==========================================

        navInventario.setOnClickListener {

            val intent =
                Intent(this, InventarioActivity::class.java)

            startActivity(intent)
            finish()
        }


        // ==========================================
        // AVISOS
        // ==========================================

        navAvisos.setOnClickListener {

            val intent =
                Intent(this, AvisosActivity::class.java)

            startActivity(intent)
            finish()
        }


        // ==========================================
        // PERFIL
        // ==========================================

        navPerfil.setOnClickListener {

            val intent =
                Intent(this, PerfilActivity::class.java)

            startActivity(intent)
            finish()
        }
    }
}
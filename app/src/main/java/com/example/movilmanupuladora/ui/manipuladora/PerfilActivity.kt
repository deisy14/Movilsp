package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.ui.auth.LoginActivity

class PerfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_perfil)

        // ==========================================
        // INSETS DE LA PANTALLA
        // ==========================================

        val main = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->

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
        // BOTONES DEL PERFIL
        // ==========================================

        val btnEditarPerfil =
            findViewById<LinearLayout>(R.id.btnEditarPerfil)

        val btnNotificaciones =
            findViewById<LinearLayout>(R.id.btnNotificaciones)

        val btnAyuda =
            findViewById<LinearLayout>(R.id.btnAyuda)

        val btnCerrarSesion =
            findViewById<Button>(R.id.btnCerrarSesion)

        // ==========================================
        // EDITAR PERFIL
        // ==========================================

        btnEditarPerfil.setOnClickListener {

            startActivity(
                Intent(this, EditarPerfilActivity::class.java)
            )
        }

        // ==========================================
        // NOTIFICACIONES
        // ==========================================

        btnNotificaciones.setOnClickListener {

            startActivity(
                Intent(this, AvisosActivity::class.java)
            )
        }

        // ==========================================
        // AYUDA Y SOPORTE
        // ==========================================

        btnAyuda.setOnClickListener {

            startActivity(
                Intent(this, AyudaActivity::class.java)
            )
        }

        // ==========================================
        // CERRAR SESIÓN
        // ==========================================

        btnCerrarSesion.setOnClickListener {

            val intent = Intent(
                this,
                LoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
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

            startActivity(
                Intent(this, MainActivity::class.java)
            )

            finish()
        }

        // ==========================================
        // ASIGNADAS
        // ==========================================

        navAsignadas.setOnClickListener {

            startActivity(
                Intent(this, AsignadasActivity::class.java)
            )

            finish()
        }

        // ==========================================
        // INVENTARIO
        // ==========================================

        navInventario.setOnClickListener {

            startActivity(
                Intent(this, InventarioActivity::class.java)
            )

            finish()
        }

        // ==========================================
        // AVISOS
        // ==========================================

        navAvisos.setOnClickListener {

            startActivity(
                Intent(this, AvisosActivity::class.java)
            )

            finish()
        }

        // ==========================================
        // PERFIL
        // ==========================================

        navPerfil.setOnClickListener {

            // Ya estamos en Perfil.
        }
    }
}


package com.example.psirae

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class activity_perfil : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_perfil)

        // ==========================================
        // INSETS DE LA PANTALLA
        // ==========================================

        val main = findViewById<android.view.View>(R.id.main)

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

            val intent = Intent(
                this,
                btnEditarPerfil::class.java
            )

            startActivity(intent)
        }


        // ==========================================
        // NOTIFICACIONES
        // ==========================================

        btnNotificaciones.setOnClickListener {

            val intent = Intent(
                this,
                avisos_Activity::class.java
            )

            startActivity(intent)
        }


        // ==========================================
        // AYUDA Y SOPORTE
        // ==========================================

        btnAyuda.setOnClickListener {

            val intent = Intent(
                this,
                btnAyuda::class.java
            )

            startActivity(intent)
        }


        // ==========================================
        // CERRAR SESIÓN
        // ==========================================

        btnCerrarSesion.setOnClickListener {

            val intent = Intent(
                this,
                activity_login::class.java
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

            val intent = Intent(
                this,
                MainActivity::class.java
            )

            startActivity(intent)
            finish()
        }


        // ==========================================
        // ASIGNADAS
        // ==========================================

        navAsignadas.setOnClickListener {

            val intent = Intent(
                this,
                activity_componentes::class.java
            )

            startActivity(intent)
            finish()
        }


        // ==========================================
        // INVENTARIO
        // ==========================================

        navInventario.setOnClickListener {

            val intent = Intent(
                this,
                inventario_activity::class.java
            )

            startActivity(intent)
            finish()
        }


        // ==========================================
        // AVISOS
        // ==========================================

        navAvisos.setOnClickListener {

            val intent = Intent(
                this,
                avisos_Activity::class.java
            )

            startActivity(intent)
            finish()
        }


        // ==========================================
        // PERFIL
        // ==========================================

        navPerfil.setOnClickListener {

            // Ya estamos en la pantalla de perfil.

        }
    }
}


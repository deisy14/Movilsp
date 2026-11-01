package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.DialogEditarPerfilBinding
import com.example.movilmanupuladora.ui.auth.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView

class PerfilActivity : AppCompatActivity() {

    private val sessionManager by lazy { com.example.movilmanupuladora.utils.SessionManager(this) }

    private val selectorFotoPerfil =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val imgFoto = findViewById<ShapeableImageView>(R.id.imgFotoPerfil)
                imgFoto?.setImageURI(uri)
                imgFoto?.setPadding(0, 0, 0, 0)
                Toast.makeText(this, "Foto de perfil actualizada correctamente", Toast.LENGTH_SHORT).show()
            }
        }

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
                0
            )

            insets
        }

        // ==========================================
        // CARGAR DATOS DEL USUARIO LOGUEADO
        // ==========================================

        cargarDatosUsuario()

        // ==========================================
        // BOTONES DEL PERFIL
        // ==========================================

        val btnCambiarFoto =
            findViewById<View>(R.id.btnCambiarFotoPerfil)

        val btnEditarPerfil =
            findViewById<LinearLayout>(R.id.btnEditarPerfil)

        val btnNotificaciones =
            findViewById<LinearLayout>(R.id.btnNotificaciones)

        val btnAyuda =
            findViewById<LinearLayout>(R.id.btnAyuda)

        val btnCerrarSesion =
            findViewById<Button>(R.id.btnCerrarSesion)

        // ==========================================
        // CAMBIAR FOTO DE PERFIL
        // ==========================================

        btnCambiarFoto?.setOnClickListener {
            selectorFotoPerfil.launch("image/*")
        }

        // ==========================================
        // EDITAR PERFIL (VENTANA EMERGENTE)
        // ==========================================

        btnEditarPerfil.setOnClickListener {
            mostrarVentanaEmergenteEditarPerfil()
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
            sessionManager.clearSession()
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

        val barraView = findViewById<android.view.View>(R.id.barraNavegacion)
        if (barraView != null) {
            val barraBinding = com.example.movilmanupuladora.databinding.ActivityBarraNavegacionBinding.bind(barraView)
            com.example.movilmanupuladora.utils.NavigationHelper.setupBarraNavegacion(
                this,
                barraBinding,
                com.example.movilmanupuladora.utils.NavigationHelper.Tab.PERFIL,
            )
        }
    }

    private fun cargarDatosUsuario() {
        val nombre = sessionManager.getUserName() ?: "María José Rojas"
        val cargo = sessionManager.getUserRole() ?: "Manipuladora · Turno mañana"

        findViewById<android.widget.TextView>(R.id.txtNombreUsuario)?.text = nombre
        findViewById<android.widget.TextView>(R.id.txtCargoUsuario)?.text = "$cargo · Sede Institucional"
    }

    private fun mostrarVentanaEmergenteEditarPerfil() {
        try {
            val dialogBinding = DialogEditarPerfilBinding.inflate(layoutInflater)

            val nombreGuardado = sessionManager.getUserName() ?: "Manipuladora PAE"
            val correoGuardado = sessionManager.getUserEmail() ?: "manipuladora@sirae.gov.co"
            val telefonoGuardado = sessionManager.getUserPhone() ?: "310 450 8920"

            dialogBinding.edtNombre.setText(nombreGuardado)
            dialogBinding.edtCorreo.setText(correoGuardado)
            dialogBinding.edtTelefono.setText(telefonoGuardado)

            val dialog = MaterialAlertDialogBuilder(this)
                .setView(dialogBinding.root)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            dialogBinding.btnGuardarCambios.setOnClickListener {
                val passNueva = dialogBinding.edtPasswordNueva.text.toString().trim()
                val passConfirmar = dialogBinding.edtPasswordConfirmar.text.toString().trim()
                val telefono = dialogBinding.edtTelefono.text.toString().trim()

                if (passNueva.isNotEmpty() || passConfirmar.isNotEmpty()) {
                    if (passNueva.length < 6) {
                        dialogBinding.edtPasswordNueva.error = "La contraseña debe tener al menos 6 caracteres"
                        dialogBinding.edtPasswordNueva.requestFocus()
                        return@setOnClickListener
                    }

                    if (passNueva != passConfirmar) {
                        dialogBinding.edtPasswordConfirmar.error = "Las contraseñas no coinciden"
                        dialogBinding.edtPasswordConfirmar.requestFocus()
                        return@setOnClickListener
                    }

                    val correoUsuario = sessionManager.getUserEmail() ?: correoGuardado
                    if (correoUsuario.isNotBlank()) {
                        val localAuth = getSharedPreferences("SIRAE_LOCAL_AUTH", MODE_PRIVATE)
                        localAuth.edit().putString("pwd_${correoUsuario.lowercase().trim()}", passNueva).apply()
                    }
                }

                if (telefono.isNotEmpty()) {
                    sessionManager.saveUserPhone(telefono)
                }

                Toast.makeText(
                    this,
                    "Contraseña y datos de perfil actualizados correctamente",
                    Toast.LENGTH_LONG
                ).show()

                cargarDatosUsuario()
                dialog.dismiss()
            }

            dialogBinding.btnCancelar.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.90).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        } catch (e: Exception) {
            e.printStackTrace()
            startActivity(Intent(this, EditarPerfilActivity::class.java))
        }
    }
}
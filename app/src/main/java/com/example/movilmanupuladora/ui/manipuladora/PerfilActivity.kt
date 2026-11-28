package com.example.movilmanupuladora.ui.manipuladora

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.DialogEditarPerfilBinding
import com.example.movilmanupuladora.ui.auth.LoginActivity
import com.example.movilmanupuladora.utils.NavigationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch

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
        if (main != null) {
            ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                insets
            }
        }

        NavigationHelper.setupStickyTopBar(this, main ?: window.decorView.rootView, lifecycleScope)

        // ==========================================
        // CARGAR DATOS DEL USUARIO LOGUEADO
        // ==========================================

        cargarDatosUsuario()

        lifecycleScope.launch {
            try {
                com.example.movilmanupuladora.utils.SedeContratoManager.sincronizarSeccionesContrato(
                    this@PerfilActivity,
                    sessionManager.getSedeId()
                )
                val contrato = com.example.movilmanupuladora.utils.SedeContratoManager.obtenerNumeroContrato(this@PerfilActivity)
                findViewById<android.widget.TextView>(R.id.txtContratoPerfil)?.text =
                    "Contrato: ${contrato ?: "Sin asignar"}"
            } catch (_: Exception) { }
        }

        // ==========================================
        // BOTONES Y ACCIONES DEL PERFIL
        // ==========================================

        val btnCambiarFoto = findViewById<View>(R.id.btnCambiarFotoPerfil)
        val btnEditarPerfil = findViewById<View>(R.id.btnEditarPerfilCard) ?: findViewById<View>(R.id.btnEditarPerfil)
        val btnNotificaciones = findViewById<View>(R.id.btnNotificacionesCard) ?: findViewById<View>(R.id.btnNotificaciones)
        val btnAyuda = findViewById<View>(R.id.btnAyuda)
        val btnCerrarSesion = findViewById<View>(R.id.btnCerrarSesion)

        // CAMBIAR FOTO DE PERFIL
        btnCambiarFoto?.setOnClickListener {
            selectorFotoPerfil.launch("image/*")
        }

        // EDITAR PERFIL (VENTANA EMERGENTE)
        btnEditarPerfil?.setOnClickListener {
            mostrarVentanaEmergenteEditarPerfil()
        }

        // AVISOS Y NOTIFICACIONES
        btnNotificaciones?.setOnClickListener {
            startActivity(Intent(this, AvisosActivity::class.java))
        }

        // NOTIFICACIONES DESDE LA BARRA STICKY SUPERIOR
        findViewById<View>(R.id.btnNotificaciones)?.setOnClickListener {
            startActivity(Intent(this, AvisosActivity::class.java))
        }

        // RECONOCIMIENTO FACIAL
        val btnBiometriaFacialCard = findViewById<View>(R.id.btnBiometriaFacialCard)
        btnBiometriaFacialCard?.setOnClickListener {
            val intent = Intent(this, com.example.movilmanupuladora.ui.auth.ReconocimientoFacialActivity::class.java).apply {
                putExtra(
                    com.example.movilmanupuladora.ui.auth.ReconocimientoFacialActivity.EXTRA_MODO,
                    com.example.movilmanupuladora.ui.auth.ReconocimientoFacialActivity.MODO_REGISTRO
                )
                putExtra(
                    com.example.movilmanupuladora.ui.auth.ReconocimientoFacialActivity.EXTRA_CORREO,
                    sessionManager.getUserEmail()
                )
            }
            startActivity(intent)
        }

        // AYUDA Y SOPORTE INSTITUCIONAL
        btnAyuda?.setOnClickListener {
            startActivity(Intent(this, AyudaActivity::class.java))
        }

        // CERRAR SESIÓN
        btnCerrarSesion?.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        // ==========================================
        // BARRA DE NAVEGACIÓN INFERIOR
        // ==========================================

        val barraView = findViewById<View>(R.id.barraNavegacion) ?: findViewById<View>(R.id.barra_navegacion)
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
        val nombre = sessionManager.getUserName() ?: "Manipuladora"
        val cargo = sessionManager.getUserRole() ?: "Manipuladora de Alimentos"
        val contrato = com.example.movilmanupuladora.utils.SedeContratoManager.obtenerNumeroContrato(this)
        findViewById<android.widget.TextView>(R.id.txtContratoPerfil)?.text =
            "Contrato: ${contrato ?: "Sin asignar"}"
        val ubicacion = sessionManager.getUbicacionCompleta()

        findViewById<android.widget.TextView>(R.id.txtNombreUsuario)?.text = nombre
        findViewById<android.widget.TextView>(R.id.txtCargoUsuario)?.text = "$cargo · $ubicacion"
        findViewById<android.widget.TextView>(R.id.tvInstitucion)?.text = ubicacion

        val partes = nombre.trim().split("\\s+".toRegex())
        val iniciales = if (partes.size >= 2) {
            "${partes[0].firstOrNull()?.uppercaseChar() ?: ""}${partes[1].firstOrNull()?.uppercaseChar() ?: ""}"
        } else {
            partes.firstOrNull()?.take(2)?.uppercase() ?: "PAE"
        }
        findViewById<android.widget.TextView>(R.id.txtInicialesAvatar)?.text = iniciales
        val raciones = com.example.movilmanupuladora.ui.manipuladora_Jefe.AsistenciaManager.obtenerManana(this)
        val sede = sessionManager.getSedeNombre()?.takeIf { it.isNotBlank() } ?: "Sede asignada"
        findViewById<android.widget.TextView>(R.id.txtHorarioTurno)?.text = if (raciones > 0) {
            "$sede · $raciones raciones hoy"
        } else {
            "$sede · Jornada activa"
        }
    }

    private fun mostrarVentanaEmergenteEditarPerfil() {
        try {
            val dialogBinding = DialogEditarPerfilBinding.inflate(layoutInflater)

            val nombreGuardado = sessionManager.getUserName() ?: "Manipuladora"
            val correoGuardado = sessionManager.getUserEmail() ?: ""
            val telefonoGuardado = sessionManager.getUserPhone() ?: ""

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

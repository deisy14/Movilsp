package com.example.movilmanupuladora.utils

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityBarraNavegacionBinding
import com.example.movilmanupuladora.databinding.PopupNotificacionesBinding
import com.example.movilmanupuladora.ui.manipuladora.AsignadasActivity
import com.example.movilmanupuladora.ui.manipuladora.AvisosActivity
import com.example.movilmanupuladora.ui.manipuladora.InventarioActivity
import com.example.movilmanupuladora.ui.manipuladora.MainActivity
import com.example.movilmanupuladora.ui.manipuladora.PerfilActivity
import com.example.movilmanupuladora.ui.manipuladora.PreparacionActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

object NavigationHelper {

    enum class Tab {
        INICIO,
        ASIGNADAS,
        INVENTARIO,
        AVISOS,
        PERFIL
    }

    private const val COLOR_ACTIVO_HEX = "#2B5424"
    private const val COLOR_INACTIVO_HEX = "#64748B"

    fun setupBarraNavegacion(
        activity: Activity,
        binding: ActivityBarraNavegacionBinding,
        tabActual: Tab
    ) {
        val colorActivo = Color.parseColor(COLOR_ACTIVO_HEX)
        val colorInactivo = Color.parseColor(COLOR_INACTIVO_HEX)

        binding.tvNavInicio.setTextColor(colorInactivo)
        binding.tvNavInicio.typeface = Typeface.DEFAULT
        binding.icNavInicio.imageTintList = ColorStateList.valueOf(colorInactivo)

        binding.tvNavAsignadas.setTextColor(colorInactivo)
        binding.tvNavAsignadas.typeface = Typeface.DEFAULT
        binding.icNavAsignadas.imageTintList = ColorStateList.valueOf(colorInactivo)

        binding.tvNavInventario.setTextColor(colorInactivo)
        binding.tvNavInventario.typeface = Typeface.DEFAULT
        binding.icNavInventario.imageTintList = ColorStateList.valueOf(colorInactivo)

        binding.tvNavAvisos.setTextColor(colorInactivo)
        binding.tvNavAvisos.typeface = Typeface.DEFAULT
        binding.icNavAvisos.imageTintList = ColorStateList.valueOf(colorInactivo)

        binding.tvNavPerfil.setTextColor(colorInactivo)
        binding.tvNavPerfil.typeface = Typeface.DEFAULT
        binding.icNavPerfil.imageTintList = ColorStateList.valueOf(colorInactivo)

        fun selectTab(icon: android.widget.ImageView?, text: android.widget.TextView?) {
            icon?.imageTintList = ColorStateList.valueOf(colorActivo)
            text?.setTextColor(colorActivo)
            text?.typeface = Typeface.DEFAULT_BOLD
        }

        when (tabActual) {
            Tab.INICIO -> selectTab(binding.icNavInicio, binding.tvNavInicio)
            Tab.ASIGNADAS -> selectTab(binding.icNavAsignadas, binding.tvNavAsignadas)
            Tab.INVENTARIO -> selectTab(binding.icNavInventario, binding.tvNavInventario)
            Tab.AVISOS -> selectTab(binding.icNavAvisos, binding.tvNavAvisos)
            Tab.PERFIL -> selectTab(binding.icNavPerfil, binding.tvNavPerfil)
        }

        val bottomBarBg = binding.root.findViewById<com.example.movilmanupuladora.ui.manipuladora.BottomBarBackground>(R.id.bottomBarBg)
        bottomBarBg?.accentColor = colorActivo
        bottomBarBg?.post {
            val width = bottomBarBg.width.toFloat()
            if (width > 0) {
                bottomBarBg.drawNotch = true
                val targetX = when (tabActual) {
                    Tab.INICIO -> width * 0.10f
                    Tab.ASIGNADAS -> width * 0.30f
                    Tab.INVENTARIO -> width * 0.50f
                    Tab.AVISOS -> width * 0.70f
                    Tab.PERFIL -> width * 0.90f
                }
                bottomBarBg.notchX = targetX
            }
        }

        binding.navInicio.setOnClickListener {
            if (tabActual != Tab.INICIO) {
                val intent = Intent(activity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        binding.navAsignadas.setOnClickListener {
            if (tabActual != Tab.ASIGNADAS) {
                val intent = Intent(activity, AsignadasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        binding.navInventario.setOnClickListener {
            if (tabActual != Tab.INVENTARIO) {
                val intent = Intent(activity, InventarioActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        binding.navAvisos.setOnClickListener {
            if (tabActual != Tab.AVISOS) {
                val intent = Intent(activity, AvisosActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        binding.navPerfil.setOnClickListener {
            if (tabActual != Tab.PERFIL) {
                val intent = Intent(activity, PerfilActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }
    }

    fun showNotificationPopup(activity: Activity) {
        val popupBinding = PopupNotificacionesBinding.inflate(activity.layoutInflater)
        popupBinding.txtCantidadNotificaciones.text = "0"
        popupBinding.contenedorSinNotificaciones.visibility = View.GONE
        popupBinding.contenedorNotificaciones.visibility = View.VISIBLE

        if (activity is androidx.lifecycle.LifecycleOwner) {
            activity.lifecycleScope.launch {
                try {
                    val response = com.example.movilmanupuladora.data.api.RetrofitClient.apiService.obtenerNotificaciones()
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val notifs = response.body()!!
                        val noLeidas = notifs.count { it.leido == false }
                        popupBinding.txtCantidadNotificaciones.text = noLeidas.toString()
                        if (notifs.isEmpty()) {
                            popupBinding.contenedorNotificaciones.visibility = View.GONE
                            popupBinding.contenedorSinNotificaciones.visibility = View.VISIBLE
                        } else {
                            popupBinding.contenedorNotificaciones.visibility = View.VISIBLE
                            popupBinding.contenedorSinNotificaciones.visibility = View.GONE
                        }
                    } else {
                        popupBinding.txtCantidadNotificaciones.text = "0"
                        popupBinding.contenedorNotificaciones.visibility = View.GONE
                        popupBinding.contenedorSinNotificaciones.visibility = View.VISIBLE
                    }
                } catch (_: Exception) {
                    popupBinding.txtCantidadNotificaciones.text = "0"
                    popupBinding.contenedorNotificaciones.visibility = View.GONE
                    popupBinding.contenedorSinNotificaciones.visibility = View.VISIBLE
                }
            }
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(popupBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        popupBinding.notificacionPreparacion.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(activity, PreparacionActivity::class.java)
            activity.startActivity(intent)
        }

        popupBinding.notificacionInventario.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(activity, InventarioActivity::class.java)
            activity.startActivity(intent)
        }

        popupBinding.notificacionAsistencia.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(activity, AsignadasActivity::class.java)
            activity.startActivity(intent)
        }

        popupBinding.btnVerTodasNotificaciones.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(activity, AvisosActivity::class.java)
            activity.startActivity(intent)
        }

        dialog.show()
    }

    fun setupStickyTopBar(
        activity: Activity,
        rootView: View,
        lifecycleScope: LifecycleCoroutineScope? = null,
        onNotificationClick: (() -> Unit)? = null
    ) {
        val sessionMgr = SessionManager(activity)
        val nombreUser = sessionMgr.getUserName() ?: "Manipuladora"
        val sedeNombre = sessionMgr.getUbicacionCompleta()

        val tvNombre = rootView.findViewById<android.widget.TextView>(R.id.tvNombreUsuarioSticky)
        val tvInst = rootView.findViewById<android.widget.TextView>(R.id.tvInstitucion)
        val tvIniciales = rootView.findViewById<android.widget.TextView>(R.id.tvAvatarIniciales)
        val btnNotif = rootView.findViewById<View>(R.id.btnNotificaciones)
        val badge = rootView.findViewById<android.widget.TextView>(R.id.badgeNotificacionesSticky)

        tvNombre?.text = "Hola, $nombreUser"
        tvInst?.text = if (sedeNombre.isNotBlank()) sedeNombre else "Sede asignada"
        val iniciales = nombreUser.split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "PA" }
        tvIniciales?.text = iniciales

        btnNotif?.setOnClickListener {
            if (onNotificationClick != null) {
                onNotificationClick()
            } else if (activity !is AvisosActivity) {
                val intent = Intent(activity, AvisosActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
            }
        }

        if (lifecycleScope != null) {
            lifecycleScope.launch {
                try {
                    val response = com.example.movilmanupuladora.data.api.RetrofitClient.apiService.obtenerNotificaciones()
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        val noLeidas = response.body()!!.count { it.leido == false }
                        badge?.visibility = if (noLeidas > 0) View.VISIBLE else View.GONE
                        badge?.text = if (noLeidas > 99) "99+" else noLeidas.toString()
                    } else {
                        badge?.visibility = View.GONE
                    }
                } catch (_: Exception) {
                    badge?.visibility = View.GONE
                }
            }
        }
    }
}

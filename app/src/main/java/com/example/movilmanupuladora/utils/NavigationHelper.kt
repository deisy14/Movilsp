package com.example.movilmanupuladora.utils

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.ActivityBarraNavegacionBinding
import com.example.movilmanupuladora.ui.manipuladora.AsignadasActivity
import com.example.movilmanupuladora.ui.manipuladora.AvisosActivity
import com.example.movilmanupuladora.ui.manipuladora.InventarioActivity
import com.example.movilmanupuladora.ui.manipuladora.MainActivity
import com.example.movilmanupuladora.ui.manipuladora.PerfilActivity

object NavigationHelper {

    enum class Tab {
        INICIO,
        ASIGNADAS,
        INVENTARIO,
        AVISOS,
        PERFIL
    }

    private const val COLOR_ACTIVO_HEX = "#F4B41F"
    private const val COLOR_INACTIVO_HEX = "#000000"

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
            Tab.PERFIL -> {
                // Perfil sin marcar abajo
            }
        }

        val bottomBarBg = binding.root.findViewById<com.example.movilmanupuladora.ui.manipuladora.BottomBarBackground>(R.id.bottomBarBg)
        bottomBarBg?.post {
            val width = bottomBarBg.width.toFloat()
            if (width > 0) {
                if (tabActual == Tab.PERFIL) {
                    bottomBarBg.drawNotch = false
                    bottomBarBg.notchX = -1f
                } else {
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
        }

        binding.navInicio.setOnClickListener {
            if (tabActual != Tab.INICIO) {
                val intent = Intent(activity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        binding.navAsignadas.setOnClickListener {
            if (tabActual != Tab.ASIGNADAS) {
                val intent = Intent(activity, AsignadasActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        binding.navInventario.setOnClickListener {
            if (tabActual != Tab.INVENTARIO) {
                val intent = Intent(activity, InventarioActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        binding.navAvisos.setOnClickListener {
            if (tabActual != Tab.AVISOS) {
                val intent = Intent(activity, AvisosActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        binding.navPerfil.setOnClickListener {
            if (tabActual != Tab.PERFIL) {
                val intent = Intent(activity, PerfilActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }
    }
}

package com.example.movilmanupuladora.utils

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
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

    private const val COLOR_ACTIVO_HEX = "#D4A017"
    private const val COLOR_INACTIVO_HEX = "#667085"

    fun setupBarraNavegacion(
        activity: Activity,
        binding: ActivityBarraNavegacionBinding,
        tabActual: Tab
    ) {
        val colorActivo = Color.parseColor(COLOR_ACTIVO_HEX)
        val colorInactivo = Color.parseColor(COLOR_INACTIVO_HEX)

        // 1. Resetear todos los tabs a inactivo
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

        // 2. Resaltar tab activo con su color y negrita
        when (tabActual) {
            Tab.INICIO -> {
                binding.tvNavInicio.setTextColor(colorActivo)
                binding.tvNavInicio.typeface = Typeface.DEFAULT_BOLD
                binding.icNavInicio.imageTintList = ColorStateList.valueOf(colorActivo)
            }
            Tab.ASIGNADAS -> {
                binding.tvNavAsignadas.setTextColor(colorActivo)
                binding.tvNavAsignadas.typeface = Typeface.DEFAULT_BOLD
                binding.icNavAsignadas.imageTintList = ColorStateList.valueOf(colorActivo)
            }
            Tab.INVENTARIO -> {
                binding.tvNavInventario.setTextColor(colorActivo)
                binding.tvNavInventario.typeface = Typeface.DEFAULT_BOLD
                binding.icNavInventario.imageTintList = ColorStateList.valueOf(colorActivo)
            }
            Tab.AVISOS -> {
                binding.tvNavAvisos.setTextColor(colorActivo)
                binding.tvNavAvisos.typeface = Typeface.DEFAULT_BOLD
                binding.icNavAvisos.imageTintList = ColorStateList.valueOf(colorActivo)
            }
            Tab.PERFIL -> {
                binding.tvNavPerfil.setTextColor(colorActivo)
                binding.tvNavPerfil.typeface = Typeface.DEFAULT_BOLD
                binding.icNavPerfil.imageTintList = ColorStateList.valueOf(colorActivo)
            }
        }

        // 3. Configurar clics
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

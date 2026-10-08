package com.example.movilmanupuladora.utils

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import com.example.movilmanupuladora.databinding.BarraNavegacionJefaBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefe.HistorialPreparacionesActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InicioJefaActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.InventarioJefaActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.ManipuladorasJefaActivity
import com.example.movilmanupuladora.ui.manipuladora_Jefe.PerfilJefaActivity

object NavigationHelperJefa {

    enum class TabJefa {
        INICIO,
        INVENTARIO,
        PREPARACIONES,
        MANIPULADORAS,
        PERFIL
    }

    private const val COLOR_ACTIVO_HEX = "#1B3317"
    private const val COLOR_INACTIVO_HEX = "#94A3B8"

    fun setupBarraNavegacionJefa(
        activity: Activity,
        binding: BarraNavegacionJefaBinding,
        tabActual: TabJefa
    ) {
        val colorActivo = Color.parseColor(COLOR_ACTIVO_HEX)
        val colorInactivo = Color.parseColor(COLOR_INACTIVO_HEX)

        binding.txtNavInicio.setTextColor(colorInactivo)
        binding.iconNavInicio.imageTintList = ColorStateList.valueOf(colorInactivo)

        binding.txtNavInventario.setTextColor(colorInactivo)
        binding.iconNavInventario.imageTintList = ColorStateList.valueOf(colorInactivo)

        binding.txtNavPreparaciones.setTextColor(colorInactivo)
        binding.iconNavPreparaciones.imageTintList = ColorStateList.valueOf(colorInactivo)

        binding.txtNavManipuladoras.setTextColor(colorInactivo)
        binding.iconNavManipuladoras.imageTintList = ColorStateList.valueOf(colorInactivo)

        binding.txtNavPerfil.setTextColor(colorInactivo)
        binding.iconNavPerfil.imageTintList = ColorStateList.valueOf(colorInactivo)

        when (tabActual) {
            TabJefa.INICIO -> {
                binding.txtNavInicio.setTextColor(colorActivo)
                binding.txtNavInicio.typeface = Typeface.DEFAULT_BOLD
                binding.iconNavInicio.imageTintList = ColorStateList.valueOf(colorActivo)
            }
            TabJefa.INVENTARIO -> {
                binding.txtNavInventario.setTextColor(colorActivo)
                binding.txtNavInventario.typeface = Typeface.DEFAULT_BOLD
                binding.iconNavInventario.imageTintList = ColorStateList.valueOf(colorActivo)
            }
            TabJefa.PREPARACIONES -> {
                binding.txtNavPreparaciones.setTextColor(colorActivo)
                binding.txtNavPreparaciones.typeface = Typeface.DEFAULT_BOLD
                binding.iconNavPreparaciones.imageTintList = ColorStateList.valueOf(colorActivo)
            }
            TabJefa.MANIPULADORAS -> {
                binding.txtNavManipuladoras.setTextColor(colorActivo)
                binding.txtNavManipuladoras.typeface = Typeface.DEFAULT_BOLD
                binding.iconNavManipuladoras.imageTintList = ColorStateList.valueOf(colorActivo)
            }
            TabJefa.PERFIL -> {
                binding.txtNavPerfil.setTextColor(colorActivo)
                binding.txtNavPerfil.typeface = Typeface.DEFAULT_BOLD
                binding.iconNavPerfil.imageTintList = ColorStateList.valueOf(colorActivo)
            }
        }

        binding.navInicio.setOnClickListener {
            if (tabActual != TabJefa.INICIO) {
                val intent = Intent(activity, InicioJefaActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        binding.navInventario.setOnClickListener {
            if (tabActual != TabJefa.INVENTARIO) {
                val intent = Intent(activity, InventarioJefaActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        binding.navPreparaciones.setOnClickListener {
            if (tabActual != TabJefa.PREPARACIONES) {
                val intent = Intent(activity, HistorialPreparacionesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        binding.navManipuladoras.setOnClickListener {
            if (tabActual != TabJefa.MANIPULADORAS) {
                val intent = Intent(activity, ManipuladorasJefaActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }

        binding.navPerfil.setOnClickListener {
            if (tabActual != TabJefa.PERFIL) {
                val intent = Intent(activity, PerfilJefaActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.finish()
            }
        }
    }
}
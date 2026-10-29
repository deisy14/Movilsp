package com.example.movilmanupuladora.utils

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.movilmanupuladora.R
import com.example.movilmanupuladora.databinding.BarraNavegacionJefaBinding
import com.example.movilmanupuladora.databinding.ViewBottomBarBinding
import com.example.movilmanupuladora.ui.manipuladora_Jefa.SugerenciaIaActivity

object NavigationHelperJefa {

    enum class TabJefa {
        INICIO,
        INVENTARIO,
        PREPARACIONES,
        MANIPULADORAS,
        PERFIL
    }

    private const val COLOR_ACTIVO_HEX = "#F4B41F"
    private const val COLOR_INACTIVO_HEX = "#000000"

    fun setupBarraNavegacionJefa(
        activity: Activity,
        binding: BarraNavegacionJefaBinding,
        tabActual: TabJefa
    ) {
        setupBarraNavegacionJefaView(activity, binding.root, tabActual)
    }

    fun setupBarraNavegacionJefa(
        activity: Activity,
        binding: ViewBottomBarBinding,
        tabActual: TabJefa
    ) {
        setupBarraNavegacionJefaView(activity, binding.root, tabActual)
    }

    fun setupBarraNavegacionJefaView(
        activity: Activity,
        rootView: View,
        tabActual: TabJefa
    ) {
        val colorActivo = Color.parseColor(COLOR_ACTIVO_HEX)
        val colorInactivo = Color.parseColor(COLOR_INACTIVO_HEX)

        val navInicio = rootView.findViewById<View>(R.id.navInicio)
        val iconNavInicio = rootView.findViewById<ImageView>(R.id.iconNavInicio)
        val txtNavInicio = rootView.findViewById<TextView>(R.id.txtNavInicio)

        val navInventario = rootView.findViewById<View>(R.id.navInventario)
        val iconNavInventario = rootView.findViewById<ImageView>(R.id.iconNavInventario)
        val txtNavInventario = rootView.findViewById<TextView>(R.id.txtNavInventario)

        val navPreparaciones = rootView.findViewById<View>(R.id.navPreparaciones)
        val iconNavPreparaciones = rootView.findViewById<ImageView>(R.id.iconNavPreparaciones)
        val txtNavPreparaciones = rootView.findViewById<TextView>(R.id.txtNavPreparaciones)

        val navEquipo = rootView.findViewById<View>(R.id.navEquipo) ?: rootView.findViewById<View>(R.id.navManipuladoras)
        val iconNavEquipo = rootView.findViewById<ImageView>(R.id.iconNavEquipo) ?: rootView.findViewById<ImageView>(R.id.iconNavManipuladoras)
        val txtNavEquipo = rootView.findViewById<TextView>(R.id.txtNavEquipo) ?: rootView.findViewById<TextView>(R.id.txtNavManipuladoras)

        val fabAdd = rootView.findViewById<View>(R.id.fabAdd)

        fun resetTab(icon: ImageView?, text: TextView?) {
            icon?.imageTintList = ColorStateList.valueOf(colorInactivo)
            text?.setTextColor(colorInactivo)
            text?.typeface = Typeface.DEFAULT
        }

        fun selectTab(icon: ImageView?, text: TextView?) {
            icon?.imageTintList = ColorStateList.valueOf(colorActivo)
            text?.setTextColor(colorActivo)
            text?.typeface = Typeface.DEFAULT_BOLD
        }

        resetTab(iconNavInicio, txtNavInicio)
        resetTab(iconNavInventario, txtNavInventario)
        resetTab(iconNavPreparaciones, txtNavPreparaciones)
        resetTab(iconNavEquipo, txtNavEquipo)

        when (tabActual) {
            TabJefa.INICIO -> selectTab(iconNavInicio, txtNavInicio)
            TabJefa.INVENTARIO -> selectTab(iconNavInventario, txtNavInventario)
            TabJefa.PREPARACIONES -> selectTab(iconNavPreparaciones, txtNavPreparaciones)
            TabJefa.MANIPULADORAS -> selectTab(iconNavEquipo, txtNavEquipo)
            TabJefa.PERFIL -> {
                // Perfil no marca ninguna pestaña abajo
            }
        }

        val bottomBarBg = rootView.findViewById<com.example.movilmanupuladora.ui.manipuladora.BottomBarBackground>(R.id.bottomBarBg)
        bottomBarBg?.post {
            val width = bottomBarBg.width.toFloat()
            if (width > 0) {
                if (tabActual == TabJefa.PERFIL) {
                    bottomBarBg.drawNotch = false
                    bottomBarBg.notchX = -1f
                } else {
                    bottomBarBg.drawNotch = true
                    val targetX = when (tabActual) {
                        TabJefa.INICIO -> width * 0.10f
                        TabJefa.INVENTARIO -> width * 0.30f
                        TabJefa.PREPARACIONES -> width * 0.70f
                        TabJefa.MANIPULADORAS -> width * 0.90f
                        else -> width * 0.10f
                    }
                    bottomBarBg.notchX = targetX
                }
            }
        }

        navInicio?.setOnClickListener {
            if (tabActual != TabJefa.INICIO) {
                val intent = Intent().setClassName(activity, "com.example.movilmanupuladora.ui.manipuladora_Jefa.InicioJefaActivity")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        navInventario?.setOnClickListener {
            if (tabActual != TabJefa.INVENTARIO) {
                val intent = Intent().setClassName(activity, "com.example.movilmanupuladora.ui.manipuladora_Jefa.InventarioJefaActivity")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        navPreparaciones?.setOnClickListener {
            if (tabActual != TabJefa.PREPARACIONES) {
                val intent = Intent().setClassName(activity, "com.example.movilmanupuladora.ui.manipuladora_Jefa.PreparacionesJefaActivity")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        navEquipo?.setOnClickListener {
            if (tabActual != TabJefa.MANIPULADORAS) {
                val intent = Intent().setClassName(activity, "com.example.movilmanupuladora.ui.manipuladora_Jefa.ManipuladorasJefaActivity")
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finish()
            }
        }

        fabAdd?.setOnClickListener {
            val intent = Intent(activity, SugerenciaIaActivity::class.java)
            activity.startActivity(intent)
        }
    }
}

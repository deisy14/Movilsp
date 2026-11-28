package com.example.movilmanupuladora.utils

import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

object CalendarDotHelper {

    /**
     * Dibuja un puntito del mismo color del número justo debajo de él,
     * dentro del circulito del día, para indicar que la fecha tiene menús pendientes de aprobación.
     */
    fun aplicarPuntitoPendiente(cell: FrameLayout, txtDia: TextView, colorNumero: Int) {
        val d = txtDia.resources.displayMetrics.density
        txtDia.setPadding(0, 0, 0, (4 * d).toInt())

        val sizePx = (3.5f * d).toInt().coerceAtLeast(3)
        val cellH = cell.layoutParams?.height ?: (40 * d).toInt()
        val txtH = txtDia.layoutParams?.height ?: (34 * d).toInt()
        val bottomInsideCircle = ((cellH - txtH) / 2).coerceAtLeast(0) + (4.5f * d).toInt()

        val dot = View(txtDia.context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorNumero)
            }
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx).apply {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                bottomMargin = bottomInsideCircle
            }
        }
        cell.addView(dot)
    }
}

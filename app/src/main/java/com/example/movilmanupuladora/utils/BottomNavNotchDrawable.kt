package com.example.movilmanupuladora.utils

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class BottomNavNotchDrawable : Drawable() {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#F4B41F")
        strokeWidth = 2.5f // Borde elegante y delgado tal como en la imagen
    }

    private val path = Path()

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val density = Resources.getSystem().displayMetrics.density
        val cornerRadius = 34f * density

        path.reset()

        val cx = width / 2f
        val notchRadius = 38f * density // Ancho de la muesca/joroba central
        val notchHeight = 16f * density // Altura del arco superior

        // Esquina superior izquierda
        path.moveTo(cornerRadius, 0f)

        // Línea superior izquierda hasta el inicio de la muesca
        path.lineTo(cx - notchRadius - 10f * density, 0f)

        // Curva superior (hump / muesca) que abraza el botón central (+)
        path.cubicTo(
            cx - notchRadius, 0f,
            cx - notchRadius + 4f * density, -notchHeight,
            cx, -notchHeight
        )
        path.cubicTo(
            cx + notchRadius - 4f * density, -notchHeight,
            cx + notchRadius, 0f,
            cx + notchRadius + 10f * density, 0f
        )

        // Línea superior derecha
        path.lineTo(width - cornerRadius, 0f)
        // Esquina superior derecha
        path.quadTo(width, 0f, width, cornerRadius)
        // Lado derecho
        path.lineTo(width, height - cornerRadius)
        // Esquina inferior derecha
        path.quadTo(width, height, width - cornerRadius, height)
        // Lado inferior
        path.lineTo(cornerRadius, height)
        // Esquina inferior izquierda
        path.quadTo(0f, height, 0f, height - cornerRadius)
        // Lado izquierdo
        path.lineTo(0f, cornerRadius)
        // Esquina superior izquierda
        path.quadTo(0f, 0f, cornerRadius, 0f)

        path.close()

        canvas.drawPath(path, bgPaint)
        canvas.drawPath(path, strokePaint)
    }

    override fun setAlpha(alpha: Int) {
        bgPaint.alpha = alpha
        strokePaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bgPaint.colorFilter = colorFilter
        strokePaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

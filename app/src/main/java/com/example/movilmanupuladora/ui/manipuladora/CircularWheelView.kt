package com.example.movilmanupuladora.ui.manipuladora

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CircularWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Colores de los sectores de la ruleta (tal cual en la imagen):
    // Desayuno (Izquierda): Amarillo Dorado (#E5AC2B)
    // Almuerzo (Centro): Verde Oscuro (#2D5235)
    // Merienda (Derecha): Verde Claro / Sabio (#9BB386)
    private val paintYellow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFE5AC2B.toInt()
    }

    private val paintDarkGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF2D5235.toInt()
    }

    private val paintLightGreen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF9BB386.toInt()
    }

    // Líneas divisoras blancas entre sectores
    private val paintDivider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 6f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
    }

    // Borde exterior blanco de la ruleta
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 4f * resources.displayMetrics.density
    }

    // Centro / Pivote (Hub) semicircular blanco con punto verde oscuro
    private val paintHubBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    private val paintHubBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFE2E8F0.toInt()
        strokeWidth = 2f * resources.displayMetrics.density
    }

    private val paintHubDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF2D5235.toInt()
    }

    private val rectF = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h // Centro inferior del semicírculo

        val radius = min(w / 2f, h) * 0.96f

        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        // Dibujar los 3 sectores del semicírculo superior (de 180° a 360°):
        // Sector 1: [180°, 240°] -> Desayuno (Amarillo)
        // Sector 2: [240°, 300°] -> Almuerzo (Verde Oscuro)
        // Sector 3: [300°, 360°] -> Merienda (Verde Claro)
        canvas.drawArc(rectF, 180f, 60f, true, paintYellow)
        canvas.drawArc(rectF, 240f, 60f, true, paintDarkGreen)
        canvas.drawArc(rectF, 300f, 60f, true, paintLightGreen)

        // Dibujar líneas divisoras blancas en los límites radiales (180°, 240°, 300°, 360°)
        val angles = floatArrayOf(180f, 240f, 300f, 360f)
        for (angle in angles) {
            val rad = Math.toRadians(angle.toDouble())
            val stopX = cx + (radius * cos(rad)).toFloat()
            val stopY = cy + (radius * sin(rad)).toFloat()
            canvas.drawLine(cx, cy, stopX, stopY, paintDivider)
        }

        // Borde exterior blanco
        canvas.drawArc(rectF, 180f, 180f, false, paintBorder)
        canvas.drawLine(cx - radius, cy, cx + radius, cy, paintBorder)

        // Centro / Pivote (Hub) semicircular
        val hubRadius = radius * 0.22f
        val dotRadius = radius * 0.07f

        canvas.drawCircle(cx, cy, hubRadius, paintHubBg)
        canvas.drawCircle(cx, cy, hubRadius, paintHubBorder)
        canvas.drawCircle(cx, cy, dotRadius, paintHubDot)
    }
}

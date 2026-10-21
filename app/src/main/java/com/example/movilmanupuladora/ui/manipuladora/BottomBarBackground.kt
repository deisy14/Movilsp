package com.example.movilmanupuladora.ui.manipuladora

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BottomBarBackground @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE // Fondo BLANCO
        style = Paint.Style.FILL
        // SIN SOMBRA
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E5B838") // Borde dorado
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E5B838")
        style = Paint.Style.FILL
    }

    private val path = Path()

    // Animatable notch X position
    var drawNotch: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var notchX: Float = -1f
        set(value) {
            field = value
            invalidate()
        }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val topMargin = 12f
        val cornerRadius = 36f
        val notchRadius = 30f
        val notchDepth = 30f

        if (!drawNotch || notchX < 0) {
            path.reset()
            path.moveTo(0f, topMargin + cornerRadius)
            path.arcTo(RectF(0f, topMargin, cornerRadius * 2, topMargin + cornerRadius * 2), 180f, 90f)
            path.lineTo(width - cornerRadius, topMargin)
            path.arcTo(RectF(width - cornerRadius * 2, topMargin, width, topMargin + cornerRadius * 2), 270f, 90f)
            path.lineTo(width, height - cornerRadius)
            path.arcTo(RectF(width - cornerRadius * 2, height - cornerRadius * 2, width, height), 0f, 90f)
            path.lineTo(cornerRadius, height)
            path.arcTo(RectF(0f, height - cornerRadius * 2, cornerRadius * 2, height), 90f, 90f)
            path.close()

            canvas.drawPath(path, bgPaint)
            canvas.drawPath(path, strokePaint)
            return
        }

        path.reset()
        val currentNotchX = notchX

        // Start top-left corner
        path.moveTo(0f, topMargin + cornerRadius)
        path.arcTo(RectF(0f, topMargin, cornerRadius * 2, topMargin + cornerRadius * 2), 180f, 90f)

        // Top edge leading to notch
        val notchStart = currentNotchX - notchRadius - 10f
        val notchEnd = currentNotchX + notchRadius + 10f

        if (notchStart > cornerRadius) {
            path.lineTo(notchStart, topMargin)
        }

        // Deep top notch curve
        path.cubicTo(
            currentNotchX - notchRadius, topMargin,
            currentNotchX - notchRadius, topMargin + notchDepth,
            currentNotchX, topMargin + notchDepth
        )
        path.cubicTo(
            currentNotchX + notchRadius, topMargin + notchDepth,
            currentNotchX + notchRadius, topMargin,
            notchEnd, topMargin
        )

        // Top-right corner
        path.lineTo(width - cornerRadius, topMargin)
        path.arcTo(RectF(width - cornerRadius * 2, topMargin, width, topMargin + cornerRadius * 2), 270f, 90f)

        // Right edge down to bottom-right corner
        path.lineTo(width, height - cornerRadius)
        path.arcTo(RectF(width - cornerRadius * 2, height - cornerRadius * 2, width, height), 0f, 90f)

        // Bottom edge
        path.lineTo(cornerRadius, height)
        path.arcTo(RectF(0f, height - cornerRadius * 2, cornerRadius * 2, height), 90f, 90f)

        path.close()

        canvas.drawPath(path, bgPaint)
        canvas.drawPath(path, strokePaint)

        // Draw golden dot inside the deep notch
        canvas.drawCircle(currentNotchX, topMargin + 4f, 13f, dotPaint)
    }
}

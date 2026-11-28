package com.example.movilmanupuladora.ui.manipuladora

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Gráfica tipo anillo (Donut) para visualizar el aporte nutricional por menú en el Dashboard PAE.
 */
class NutricionChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class Slice(val label: String, val value: Float, val color: Int)

    private var slices: List<Slice> = listOf(
        Slice("Proteínas", 24f, 0xFF2B5424.toInt()),
        Slice("Carbohidratos", 52f, 0xFFF5B81F.toInt()),
        Slice("Grasas", 18f, 0xFF4D7C0F.toInt()),
        Slice("Fibra y Min.", 6f, 0xFFD97706.toInt())
    )
    private var centerTitle = "480"
    private var centerSubtitle = "kcal"

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val bgRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFF1F5F9.toInt()
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E293B.toInt()
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF64748B.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    fun setData(kcal: Int, proteinasG: Float, carbosG: Float, grasasG: Float, mineralesPct: Float = 8f) {
        centerTitle = kcal.toString()
        centerSubtitle = "kcal / menú"
        val p = proteinasG.coerceAtLeast(1f)
        val c = carbosG.coerceAtLeast(1f)
        val g = grasasG.coerceAtLeast(1f)
        val m = mineralesPct.coerceAtLeast(2f)
        slices = listOf(
            Slice("Proteínas", p, 0xFF2B5424.toInt()),
            Slice("Carbohidratos", c, 0xFFF5B81F.toInt()),
            Slice("Grasas", g, 0xFF65A30D.toInt()),
            Slice("Fibra/Min.", m, 0xFFD97706.toInt())
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = minOf(width, height).toFloat()
        if (size <= 0f) return

        val stroke = size * 0.14f
        arcPaint.strokeWidth = stroke
        bgRingPaint.strokeWidth = stroke

        val pad = stroke / 2f + 6f
        val left = (width - size) / 2f + pad
        val top = (height - size) / 2f + pad
        val right = left + size - pad * 2f
        val bottom = top + size - pad * 2f
        rect.set(left, top, right, bottom)

        canvas.drawOval(rect, bgRingPaint)

        val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
        var startAngle = -90f
        val gap = 4f
        for (slice in slices) {
            val sweep = (slice.value / total) * 360f - gap
            if (sweep > 0f) {
                arcPaint.color = slice.color
                canvas.drawArc(rect, startAngle + gap / 2f, sweep, false, arcPaint)
            }
            startAngle += (slice.value / total) * 360f
        }

        titlePaint.textSize = size * 0.21f
        subPaint.textSize = size * 0.095f
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawText(centerTitle, cx, cy + titlePaint.textSize * 0.15f, titlePaint)
        canvas.drawText(centerSubtitle, cx, cy + titlePaint.textSize * 0.65f, subPaint)
    }
}

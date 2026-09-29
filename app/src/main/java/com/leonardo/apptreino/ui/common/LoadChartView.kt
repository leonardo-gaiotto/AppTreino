package com.leonardo.apptreino.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.leonardo.apptreino.R

/**
 * Gráfico de linha simples, desenhado à mão com Canvas (sem bibliotecas externas).
 * Mostra a evolução de um exercício sessão a sessão: carga, repetições ou tempo.
 *
 * - Modo compacto (padrão): só a linha, usado como "sparkline" nos cards.
 * - Modo com grade ([showGrid] = true): linhas de referência com os valores à esquerda.
 */
class LoadChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var values: List<Float> = emptyList()

    /** Desenha as linhas de referência com os valores (versão grande do gráfico). */
    var showGrid: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    /** Formata os valores exibidos na grade (ex.: "42 kg"). */
    var labelFormatter: (Float) -> String = { it.toString() }

    private val accent = ContextCompat.getColor(context, R.color.color_accent)
    private val density = resources.displayMetrics.density

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        style = Paint.Style.STROKE
        strokeWidth = LINE_WIDTH_DP * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    private val pointHolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_surface)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_outline)
        strokeWidth = density
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.color_text_secondary)
        textSize = resources.getDimension(R.dimen.text_label)
    }

    private val linePath = Path()
    private val fillPath = Path()

    fun setValues(newValues: List<Double>) {
        values = newValues.map { it.toFloat() }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Degradê da cor de destaque até transparente, embaixo da linha.
        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            ColorUtils.setAlphaComponent(accent, FILL_ALPHA_TOP),
            ColorUtils.setAlphaComponent(accent, 0),
            Shader.TileMode.CLAMP,
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        // Faixa de valores (com uma folga para a linha não encostar nas bordas)
        var min = values.min()
        var max = values.max()
        if (max - min < MIN_RANGE) {
            min -= MIN_RANGE / 2
            max += MIN_RANGE / 2
        }

        val pointRadius = POINT_RADIUS_DP * density
        val labelWidth = if (showGrid) {
            listOf(min, max).maxOf { labelPaint.measureText(labelFormatter(it)) } + LABEL_GAP_DP * density
        } else {
            0f
        }
        val left = paddingLeft + labelWidth + pointRadius
        val right = width - paddingRight - pointRadius
        val top = paddingTop + pointRadius + if (showGrid) labelPaint.textSize / 2 else 0f
        val bottom = height - paddingBottom - pointRadius - if (showGrid) labelPaint.textSize / 2 else 0f

        fun x(index: Int) = if (values.size == 1) (left + right) / 2 else left + (right - left) * index / (values.size - 1)
        fun y(value: Float) = top + (1 - (value - min) / (max - min)) * (bottom - top)

        if (showGrid) drawGrid(canvas, min, max, left, right, ::y)

        // Linha e área preenchida
        linePath.reset()
        fillPath.reset()
        values.forEachIndexed { index, value ->
            if (index == 0) {
                linePath.moveTo(x(index), y(value))
                fillPath.moveTo(x(index), bottom)
                fillPath.lineTo(x(index), y(value))
            } else {
                linePath.lineTo(x(index), y(value))
                fillPath.lineTo(x(index), y(value))
            }
        }
        fillPath.lineTo(x(values.lastIndex), bottom)
        fillPath.close()
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)

        // Pontos: o último (sessão mais recente) fica destacado
        values.forEachIndexed { index, value ->
            val isLast = index == values.lastIndex
            val radius = if (isLast) pointRadius * LAST_POINT_SCALE else pointRadius
            canvas.drawCircle(x(index), y(value), radius, pointPaint)
            if (!isLast) canvas.drawCircle(x(index), y(value), radius / 2, pointHolePaint)
        }
    }

    private fun drawGrid(canvas: Canvas, min: Float, max: Float, left: Float, right: Float, y: (Float) -> Float) {
        val steps = GRID_LINES - 1
        for (i in 0..steps) {
            val value = min + (max - min) * i / steps
            val lineY = y(value)
            canvas.drawLine(left, lineY, right, lineY, gridPaint)
            canvas.drawText(labelFormatter(value), paddingLeft.toFloat(), lineY + labelPaint.textSize / 3, labelPaint)
        }
    }

    private companion object {
        const val LINE_WIDTH_DP = 2.5f
        const val POINT_RADIUS_DP = 3.5f
        const val LAST_POINT_SCALE = 1.6f
        const val LABEL_GAP_DP = 8f
        const val FILL_ALPHA_TOP = 90
        const val MIN_RANGE = 2f
        const val GRID_LINES = 3
    }
}

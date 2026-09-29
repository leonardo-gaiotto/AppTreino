package com.leonardo.apptreino.ui.common

import android.content.res.Resources
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.leonardo.apptreino.R
import com.leonardo.apptreino.domain.EvolutionMetric
import com.leonardo.apptreino.domain.Trend
import kotlin.math.abs
import kotlin.math.roundToInt

/** Textos e cores da evolução (carga, repetições ou tempo), compartilhados pelas telas do coach. */
object EvolutionFormat {

    /** Nome da métrica. Ex.: "Carga máxima". */
    fun metricLabel(res: Resources, metric: EvolutionMetric): String = res.getString(
        when (metric) {
            EvolutionMetric.LOAD -> R.string.metric_load
            EvolutionMetric.REPS -> R.string.metric_reps
            EvolutionMetric.SECONDS -> R.string.metric_seconds
        },
    )

    /** Valor da métrica. Ex.: "42,5 kg", "12 reps" ou "45 s". */
    fun value(res: Resources, metric: EvolutionMetric, value: Double): String = when (metric) {
        EvolutionMetric.LOAD -> Formatters.kilograms(res, value)
        EvolutionMetric.REPS -> value.roundToInt().let { res.getQuantityString(R.plurals.reps_value, it, it) }
        EvolutionMetric.SECONDS -> Formatters.duration(res, value.roundToInt())
    }

    /** Variação com sinal. Ex.: "+5 kg", "−2 reps" ou "0 kg". */
    fun delta(res: Resources, metric: EvolutionMetric, delta: Double): String {
        val amount = value(res, metric, abs(delta))
        return when {
            delta > 0 -> res.getString(R.string.delta_positive, amount)
            delta < 0 -> res.getString(R.string.delta_negative, amount)
            else -> res.getString(R.string.delta_none)
        }
    }

    /** Pinta o texto e o ícone de tendência: verde-limão (subiu), laranja (caiu) ou neutro. */
    fun applyTrend(view: TextView, trend: Trend) {
        val context = view.context
        val (colorRes, iconRes) = when (trend) {
            Trend.UP -> R.color.color_accent to R.drawable.ic_trending_up
            Trend.DOWN -> R.color.color_warning to R.drawable.ic_trending_down
            Trend.STABLE -> R.color.color_text_secondary to R.drawable.ic_trending_flat
        }
        view.setTextColor(ContextCompat.getColor(context, colorRes))
        // Ícone no tamanho pequeno (16dp), proporcional ao texto
        val size = context.resources.getDimensionPixelSize(R.dimen.icon_small)
        val icon = ContextCompat.getDrawable(context, iconRes)?.mutate()?.apply { setBounds(0, 0, size, size) }
        view.setCompoundDrawablesRelative(icon, null, null, null)
        TextViewCompat.setCompoundDrawableTintList(view, ContextCompat.getColorStateList(context, colorRes))
    }
}

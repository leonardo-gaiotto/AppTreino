package com.leonardo.apptreino.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.ItemEvolutionBinding
import com.leonardo.apptreino.domain.ExerciseEvolution
import com.leonardo.apptreino.ui.common.EvolutionFormat

/** Um card por exercício com valor atual, variação e minigráfico (ListAdapter + DiffUtil). */
class EvolutionAdapter(
    private val onClick: (ExerciseEvolution) -> Unit,
) : ListAdapter<ExerciseEvolution, EvolutionAdapter.EvolutionViewHolder>(EvolutionDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EvolutionViewHolder =
        EvolutionViewHolder(ItemEvolutionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: EvolutionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EvolutionViewHolder(private val binding: ItemEvolutionBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cardEvolution.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onClick(getItem(it)) }
            }
        }

        fun bind(evolution: ExerciseEvolution) {
            val res = binding.root.resources
            val metric = evolution.metric
            val name = res.getString(evolution.exercise.nameRes)
            val change = EvolutionFormat.delta(res, metric, evolution.totalChange)

            binding.textName.text = name
            binding.textMetric.text = res.getQuantityString(
                R.plurals.evolution_metric_sessions,
                evolution.sessions.size,
                EvolutionFormat.metricLabel(res, metric),
                evolution.sessions.size,
            )
            binding.textCurrent.text = EvolutionFormat.value(res, metric, evolution.latest.value)
            binding.textChange.text = change
            EvolutionFormat.applyTrend(binding.textChange, evolution.trend)

            binding.chart.setValues(evolution.sessions.map { it.value })
            binding.chart.contentDescription = res.getString(R.string.cd_evolution_chart, name, change)
        }
    }
}

object EvolutionDiffCallback : DiffUtil.ItemCallback<ExerciseEvolution>() {
    override fun areItemsTheSame(oldItem: ExerciseEvolution, newItem: ExerciseEvolution): Boolean =
        oldItem.exercise.id == newItem.exercise.id

    override fun areContentsTheSame(oldItem: ExerciseEvolution, newItem: ExerciseEvolution): Boolean = oldItem == newItem
}

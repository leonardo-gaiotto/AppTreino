package com.leonardo.apptreino.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.ItemHistorySessionBinding
import com.leonardo.apptreino.ui.common.EvolutionFormat
import com.leonardo.apptreino.ui.common.Formatters

/** Sessões do histórico: data, séries (repetições × carga) e tendência (ListAdapter + DiffUtil). */
class SessionAdapter : ListAdapter<SessionRow, SessionAdapter.SessionViewHolder>(SessionDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder =
        SessionViewHolder(ItemHistorySessionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(private val binding: ItemHistorySessionBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: SessionRow) {
            val res = binding.root.resources
            val session = row.session

            binding.textDate.text = Formatters.shortDate(res, session.date)
            binding.textWeekday.text = Formatters.dayNameShort(res, session.date.dayOfWeek)
            binding.textSets.text = session.sets.joinToString(res.getString(R.string.separator_dot)) {
                Formatters.setDone(res, it, row.isIsometric)
            }
            binding.textValue.text = EvolutionFormat.value(res, row.metric, session.value)
            EvolutionFormat.applyTrend(binding.textValue, row.trend)
        }
    }
}

object SessionDiffCallback : DiffUtil.ItemCallback<SessionRow>() {
    override fun areItemsTheSame(oldItem: SessionRow, newItem: SessionRow): Boolean =
        oldItem.session.date == newItem.session.date

    override fun areContentsTheSame(oldItem: SessionRow, newItem: SessionRow): Boolean = oldItem == newItem
}

package com.leonardo.apptreino.ui.week

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.ItemWeekDayBinding
import com.leonardo.apptreino.ui.common.Formatters
import com.leonardo.apptreino.ui.main.DayStatus
import com.leonardo.apptreino.ui.main.WeekDayItem

/** Adapter dos 7 dias da semana, também com ListAdapter + DiffUtil. */
class WeekDayAdapter(
    private val onDayClick: (WeekDayItem) -> Unit,
) : ListAdapter<WeekDayItem, WeekDayAdapter.WeekDayViewHolder>(WeekDayDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekDayViewHolder {
        val binding = ItemWeekDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WeekDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeekDayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WeekDayViewHolder(
        private val binding: ItemWeekDayBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cardDay.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { onDayClick(getItem(it)) }
            }
        }

        fun bind(item: WeekDayItem) {
            val context = binding.root.context
            val res = context.resources

            // Coluna do dia: o dia de hoje ganha um círculo verde-limão.
            binding.textDayShort.text = Formatters.dayNameShort(res, item.date.dayOfWeek)
            binding.textDayNumber.text = Formatters.integer(res, item.date.dayOfMonth)
            binding.textDayNumber.setBackgroundResource(if (item.isToday) R.drawable.bg_circle_accent else 0)
            binding.textDayNumber.setTextColor(
                ContextCompat.getColor(context, if (item.isToday) R.color.color_on_accent else R.color.color_text_primary),
            )

            // Treino do dia ou descanso.
            val workout = item.workout
            val letter = item.letter
            if (workout != null && letter != null) {
                binding.textDayTitle.text = res.getString(
                    R.string.week_day_workout_title, Formatters.workoutTitle(res, letter), workout.name.resolve(res),
                )
                binding.textDayDetail.text = Formatters.workoutMeta(res, workout)
                binding.progressDay.setProgressCompat(item.progress?.percent ?: 0, false)
            } else {
                binding.textDayTitle.setText(R.string.week_day_rest_title)
                binding.textDayDetail.setText(R.string.week_day_rest_detail)
            }
            binding.progressDay.isVisible = workout != null
            binding.spaceProgress.isVisible = workout != null

            bindStatus(item)

            // Dia selecionado (exibido na aba "Hoje") ganha borda verde-limão.
            binding.cardDay.strokeColor = ContextCompat.getColor(
                context, if (item.isSelected) R.color.brand_lime else R.color.color_outline,
            )
            binding.cardDay.strokeWidth = res.getDimensionPixelSize(
                if (item.isSelected) R.dimen.stroke_selected else R.dimen.stroke_thin,
            )
        }

        private fun bindStatus(item: WeekDayItem) {
            val context = binding.root.context
            val res = context.resources
            val done = item.progress?.doneItems ?: 0
            val total = item.progress?.totalItems ?: 0
            val (text, highlighted) = when (item.status) {
                DayStatus.DONE -> res.getString(R.string.week_status_done) to true
                DayStatus.IN_PROGRESS ->
                    res.getQuantityString(R.plurals.week_status_in_progress, done, done, total) to true
                DayStatus.TODAY -> res.getString(R.string.week_status_today) to true
                DayStatus.UPCOMING -> res.getString(R.string.week_status_upcoming) to false
                DayStatus.MISSED -> res.getString(R.string.week_status_missed) to false
                DayStatus.REST -> res.getString(R.string.week_status_rest) to false
            }
            val colorRes = if (highlighted) R.color.color_accent else R.color.color_text_secondary
            binding.textStatus.text = text
            binding.textStatus.setTextColor(ContextCompat.getColor(context, colorRes))
            binding.textStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (item.status == DayStatus.DONE) R.drawable.ic_check_small else 0, 0, 0, 0,
            )
            TextViewCompat.setCompoundDrawableTintList(binding.textStatus, ContextCompat.getColorStateList(context, colorRes))
        }
    }
}

object WeekDayDiffCallback : DiffUtil.ItemCallback<WeekDayItem>() {
    override fun areItemsTheSame(oldItem: WeekDayItem, newItem: WeekDayItem): Boolean =
        oldItem.date == newItem.date

    override fun areContentsTheSame(oldItem: WeekDayItem, newItem: WeekDayItem): Boolean =
        oldItem == newItem
}

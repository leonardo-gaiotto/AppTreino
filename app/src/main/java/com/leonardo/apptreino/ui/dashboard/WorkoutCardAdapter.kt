package com.leonardo.apptreino.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.ItemWorkoutCardBinding
import com.leonardo.apptreino.ui.common.Formatters

/** Treinos do aluno no painel do coach (ListAdapter + DiffUtil). */
class WorkoutCardAdapter(
    private val onEdit: (WorkoutCardItem) -> Unit,
    private val onDelete: (WorkoutCardItem) -> Unit,
) : ListAdapter<WorkoutCardItem, WorkoutCardAdapter.WorkoutViewHolder>(WorkoutCardDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder =
        WorkoutViewHolder(ItemWorkoutCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WorkoutViewHolder(private val binding: ItemWorkoutCardBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cardWorkout.setOnClickListener { current()?.let(onEdit) }
            binding.buttonEdit.setOnClickListener { current()?.let(onEdit) }
            binding.buttonDelete.setOnClickListener { current()?.let(onDelete) }
        }

        fun bind(item: WorkoutCardItem) {
            val res = binding.root.resources
            val title = Formatters.workoutTitle(res, item.letter)

            binding.textLetter.text = item.letter.toString()
            binding.textTitle.text = title
            binding.textName.text = item.workout.name.resolve(res)
            binding.textDays.text = Formatters.days(res, item.workout.days)
            binding.textMeta.text = Formatters.workoutMeta(res, item.workout)
            binding.buttonEdit.contentDescription = res.getString(R.string.cd_edit_workout, title)
            binding.buttonDelete.contentDescription = res.getString(R.string.cd_delete_workout, title)
        }

        private fun current(): WorkoutCardItem? =
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(::getItem)
    }
}

object WorkoutCardDiffCallback : DiffUtil.ItemCallback<WorkoutCardItem>() {
    override fun areItemsTheSame(oldItem: WorkoutCardItem, newItem: WorkoutCardItem): Boolean =
        oldItem.workout.id == newItem.workout.id

    override fun areContentsTheSame(oldItem: WorkoutCardItem, newItem: WorkoutCardItem): Boolean = oldItem == newItem
}

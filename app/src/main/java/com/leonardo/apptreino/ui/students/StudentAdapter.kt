package com.leonardo.apptreino.ui.students

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.ItemStudentBinding
import com.leonardo.apptreino.ui.common.Formatters
import com.leonardo.apptreino.ui.common.bindAvatar

/** Lista de alunos do coach (ListAdapter + DiffUtil). */
class StudentAdapter(
    private val onStudentClick: (StudentListItem) -> Unit,
) : ListAdapter<StudentListItem, StudentAdapter.StudentViewHolder>(StudentDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder =
        StudentViewHolder(ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StudentViewHolder(private val binding: ItemStudentBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.cardStudent.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { onStudentClick(getItem(it)) }
            }
        }

        fun bind(item: StudentListItem) {
            val res = binding.root.resources

            binding.textAvatar.bindAvatar(item.name, item.colorIndex)
            binding.textName.text = item.name
            binding.textGoal.setText(item.goal.labelRes)

            // Treino de hoje
            val workout = item.todayWorkout
            val letter = item.todayLetter
            binding.textToday.text = if (workout != null && letter != null) {
                res.getString(
                    R.string.student_today_workout,
                    Formatters.workoutTitle(res, letter),
                    item.todayProgress?.doneItems ?: 0,
                    item.todayProgress?.totalItems ?: 0,
                )
            } else {
                res.getString(R.string.student_today_rest)
            }

            // Progresso da semana
            binding.textWeek.text = res.getQuantityString(
                R.plurals.student_week_progress, item.week.planned, item.week.completed, item.week.planned,
            )
            binding.progressWeek.setProgressCompat(item.week.percent, false)
        }
    }
}

object StudentDiffCallback : DiffUtil.ItemCallback<StudentListItem>() {
    override fun areItemsTheSame(oldItem: StudentListItem, newItem: StudentListItem): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: StudentListItem, newItem: StudentListItem): Boolean = oldItem == newItem
}

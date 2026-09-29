package com.leonardo.apptreino.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.FragmentStudentSummaryBinding
import com.leonardo.apptreino.databinding.ViewInfoRowBinding
import com.leonardo.apptreino.databinding.ViewStatTileBinding
import com.leonardo.apptreino.ui.common.Formatters
import kotlinx.coroutines.launch

/** Aba "Resumo" do painel: dados do aluno, objetivo, semana e sequência de treinos. */
class StudentSummaryFragment : Fragment() {

    private var _binding: FragmentStudentSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentDashboardViewModel by activityViewModels { StudentDashboardViewModel.Factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> state?.let(::render) }
            }
        }
    }

    private fun render(state: DashboardUiState) {
        val res = resources
        val student = state.student

        // Semana, sequência e total
        binding.statWeek.bind(
            getString(R.string.fraction, state.week.completed, state.week.planned),
            getString(R.string.summary_stat_week),
        )
        binding.statStreak.bind(Formatters.integer(res, state.streak), getString(R.string.summary_stat_streak))
        binding.statTotal.bind(Formatters.integer(res, state.completedWorkouts), getString(R.string.summary_stat_total))

        // Hoje
        val today = state.today
        val letter = state.todayLetter
        if (today != null && letter != null) {
            binding.textTodayTitle.text = getString(
                R.string.week_day_workout_title, Formatters.workoutTitle(res, letter), today.workout.name.resolve(res),
            )
            binding.textTodayDetail.text = res.getQuantityString(
                R.plurals.today_progress_count, today.totalItems, today.doneItems, today.totalItems,
            )
            binding.progressToday.setProgressCompat(today.percent, true)
        } else {
            binding.textTodayTitle.setText(R.string.rest_day_title)
            binding.textTodayDetail.setText(R.string.summary_rest_detail)
        }
        binding.progressToday.isVisible = today != null
        binding.spaceTodayProgress.isVisible = today != null

        // Dados físicos
        binding.rowGoal.bind(R.string.field_goal, getString(student.goal.labelRes))
        binding.rowAge.bind(R.string.field_age, res.getQuantityString(R.plurals.value_years, student.age, student.age))
        binding.rowWeight.bind(R.string.field_weight, Formatters.kilograms(res, student.weightKg))
        binding.rowHeight.bind(R.string.field_height, getString(R.string.value_cm, student.heightCm))
        binding.rowBmi.bind(R.string.field_bmi, Formatters.decimal(res, student.bmi))

        // Observações
        binding.layoutNotes.isVisible = student.notes != null
        binding.textNotes.text = student.notes?.resolve(res)
    }

    private fun ViewStatTileBinding.bind(value: String, label: String) {
        textValue.text = value
        textLabel.text = label
    }

    private fun ViewInfoRowBinding.bind(labelRes: Int, value: String) {
        textRowLabel.setText(labelRes)
        textRowValue.text = value
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

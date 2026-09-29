package com.leonardo.apptreino.ui.profile

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.leonardo.apptreino.AppTreinoApplication
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.AppMode
import com.leonardo.apptreino.databinding.FragmentProfileBinding
import com.leonardo.apptreino.databinding.ViewInfoRowBinding
import com.leonardo.apptreino.databinding.ViewStatTileBinding
import com.leonardo.apptreino.ui.common.Formatters
import com.leonardo.apptreino.ui.common.applySystemBarsPadding
import com.leonardo.apptreino.ui.main.MainActivity
import com.leonardo.apptreino.ui.main.MainViewModel
import com.leonardo.apptreino.ui.main.ProfileUiState
import kotlinx.coroutines.launch

/**
 * Aba "Perfil": troca entre os modos Aluno e Coach, estatísticas de cada modo e preferências.
 */
class ProfileFragment : Fragment(), MainActivity.ScrollableToTop {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels { MainViewModel.Factory }

    private val settings get() = (requireActivity().application as AppTreinoApplication).settings

    /** Último estado recebido, para redesenhar o cabeçalho quando o modo muda. */
    private var lastState: ProfileUiState? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Edge-to-edge: o conteúdo começa abaixo da status bar e é cortado nela ao rolar.
        binding.root.applySystemBarsPadding()
        bindStaticContent()
        setupModeToggle()
        setupSettings()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.profileState.collect(::renderState) }
                launch { viewModel.mode.collect(::renderMode) }
            }
        }
    }

    private fun bindStaticContent() {
        binding.statWorkouts.textLabel.setText(R.string.profile_stat_workouts)
        binding.statExercises.textLabel.setText(R.string.profile_stat_exercises)
        binding.statActiveDays.textLabel.setText(R.string.profile_stat_active_days)
        binding.statCoachStudents.textLabel.setText(R.string.profile_stat_students)
        binding.statCoachWorkouts.textLabel.setText(R.string.profile_stat_coach_workouts)
        binding.statCoachAverage.textLabel.setText(R.string.profile_stat_week_average)

        val versionName = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0).versionName.orEmpty()
        binding.textVersion.text = getString(R.string.profile_version, versionName)
    }

    /** Seletor Aluno | Coach: a Bottom Navigation da MainActivity reage à mudança. */
    private fun setupModeToggle() {
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.setMode(if (checkedId == R.id.button_mode_coach) AppMode.COACH else AppMode.STUDENT)
        }
    }

    private fun setupSettings() {
        // Define o estado ANTES do listener para não disparar uma troca de tema ao abrir a tela.
        binding.switchDarkTheme.isChecked = settings.isDarkTheme
        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            // Aplica na hora; o AppCompat recria a Activity com o novo tema.
            settings.isDarkTheme = isChecked
        }
        binding.rowDarkTheme.setOnClickListener { binding.switchDarkTheme.toggle() }
        binding.rowResetDemo.setOnClickListener { confirmResetDemo() }
    }

    private fun renderMode(mode: AppMode) {
        val isCoach = mode == AppMode.COACH
        val buttonId = if (isCoach) R.id.button_mode_coach else R.id.button_mode_student
        if (binding.toggleMode.checkedButtonId != buttonId) binding.toggleMode.check(buttonId)

        binding.layoutStudentSection.isVisible = !isCoach
        binding.layoutCoachSection.isVisible = isCoach
        renderHeader(isCoach)
    }

    private fun renderState(state: ProfileUiState) {
        lastState = state

        // Aluno
        binding.statWorkouts.setValue(state.completedWorkouts)
        binding.statExercises.setValue(state.completedExercises)
        binding.statActiveDays.setValue(state.activeDaysThisWeek)
        binding.planGoal.bind(getString(R.string.plan_goal_label), getString(state.goal.labelRes))
        binding.planSplit.bind(
            getString(R.string.plan_split_label),
            state.workoutLetters.joinToString(getString(R.string.separator_dot))
                .ifEmpty { getString(R.string.plan_split_none) },
        )
        binding.planFrequency.bind(
            getString(R.string.plan_frequency_label),
            getString(R.string.plan_frequency_value, state.trainingDaysPerWeek),
        )

        // Coach
        binding.statCoachStudents.setValue(state.coachStudents)
        binding.statCoachWorkouts.setValue(state.coachWorkouts)
        binding.statCoachAverage.textValue.text = getString(R.string.today_progress_percent, state.coachWeekAverage)

        renderHeader(isCoach = binding.layoutCoachSection.isVisible)
    }

    private fun renderHeader(isCoach: Boolean) {
        if (isCoach) {
            binding.textAvatar.setText(R.string.coach_initials)
            binding.textProfileName.setText(R.string.coach_name)
            binding.textProfileSubtitle.setText(R.string.coach_role)
        } else {
            val state = lastState ?: return
            val name = state.studentName.resolve(resources)
            binding.textAvatar.text = Formatters.initials(name)
            binding.textProfileName.text = name
            binding.textProfileSubtitle.text =
                getString(R.string.profile_student_subtitle, getString(state.goal.labelRes))
        }
    }

    private fun confirmResetDemo() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_reset_demo_title)
            .setMessage(R.string.dialog_reset_demo_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_restore) { _, _ ->
                viewModel.resetDemoData()
                Snackbar.make(binding.root, R.string.message_demo_restored, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun ViewStatTileBinding.setValue(value: Int) {
        textValue.text = Formatters.integer(resources, value)
    }

    private fun ViewInfoRowBinding.bind(label: String, value: String) {
        textRowLabel.text = label
        textRowValue.text = value
    }

    override fun scrollToTop() {
        _binding?.scrollProfile?.smoothScrollTo(0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

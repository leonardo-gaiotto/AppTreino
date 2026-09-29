package com.leonardo.apptreino.ui.today

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
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.FragmentTodayBinding
import com.leonardo.apptreino.ui.common.Formatters
import com.leonardo.apptreino.ui.common.applySystemBarsPadding
import com.leonardo.apptreino.ui.exercise.ExerciseDetailActivity
import com.leonardo.apptreino.ui.main.ExerciseItem
import com.leonardo.apptreino.ui.main.MainActivity
import com.leonardo.apptreino.ui.main.MainViewModel
import com.leonardo.apptreino.ui.main.TodayUiState
import kotlinx.coroutines.launch

/**
 * Aba "Hoje": treino do dia selecionado, progresso, exercícios e cardio.
 */
class TodayFragment : Fragment(), MainActivity.ScrollableToTop {

    /*
     * Padrão de ViewBinding em Fragments: a View do Fragment pode ser destruída
     * (onDestroyView) enquanto o Fragment continua vivo. Por isso o binding é anulável
     * e zerado em onDestroyView, evitando vazamento de memória.
     */
    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    /** Mesma instância para todas as abas: pertence à Activity, não ao Fragment. */
    private val viewModel: MainViewModel by activityViewModels { MainViewModel.Factory }

    private val adapter = TodayAdapter(
        onExerciseClick = ::openExerciseDetail,
        onToggleExercise = { item -> viewModel.toggleExercise(item) },
        onToggleCardio = { item -> viewModel.toggleCardio(item.cardio.id) },
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Edge-to-edge: o conteúdo começa abaixo da status bar e é cortado nela ao rolar.
        binding.root.applySystemBarsPadding()
        binding.recyclerExercises.adapter = adapter

        binding.buttonBackToToday.setOnClickListener { viewModel.selectToday() }
        binding.buttonRestart.setOnClickListener { confirmRestart() }
        binding.buttonSeeWeek.setOnClickListener {
            MainActivity.requestTab(parentFragmentManager, R.id.nav_week)
        }

        // Coleta o estado apenas enquanto a tela está pelo menos STARTED (visível).
        // Ao ir para segundo plano a coleta para; ao voltar, recomeça automaticamente.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.todayState.collect(::render)
            }
        }
    }

    private fun render(state: TodayUiState) {
        val res = resources
        binding.textDate.text = Formatters.fullDate(res, state.date)
        binding.textTitle.text = if (state.isToday) {
            getString(R.string.today_title_today)
        } else {
            getString(R.string.today_title_other_day, Formatters.dayNameFull(res, state.date.dayOfWeek))
        }
        binding.buttonBackToToday.isVisible = !state.isToday

        val isTraining = state is TodayUiState.Training
        binding.cardWorkout.isVisible = isTraining
        binding.layoutListHeader.isVisible = isTraining
        binding.recyclerExercises.isVisible = isTraining
        binding.layoutRest.isVisible = !isTraining

        when (state) {
            is TodayUiState.Training -> renderTraining(state)
            is TodayUiState.Rest -> adapter.submitList(emptyList())
        }
    }

    private fun renderTraining(state: TodayUiState.Training) {
        val res = resources
        val progress = state.progress

        binding.textWorkoutLetter.text = state.letter.toString()
        binding.textWorkoutName.text = Formatters.workoutTitle(res, state.letter)
        binding.textWorkoutFocus.text = state.workout.name.resolve(res)
        binding.textWorkoutMeta.text = Formatters.workoutMeta(res, state.workout)

        binding.textProgressCount.text = res.getQuantityString(
            R.plurals.today_progress_count, progress.totalItems, progress.doneItems, progress.totalItems,
        )
        binding.textProgressPercent.text = getString(R.string.today_progress_percent, progress.percent)
        binding.progressWorkout.setProgressCompat(progress.percent, true)
        val remaining = progress.totalItems - progress.doneItems
        binding.textProgressMessage.text = when {
            progress.isComplete -> getString(R.string.today_progress_completed)
            !progress.isStarted -> getString(R.string.today_progress_not_started)
            else -> res.getQuantityString(R.plurals.today_progress_remaining, remaining, remaining)
        }
        binding.buttonRestart.isVisible = progress.isStarted

        // ListAdapter + DiffUtil: só os cards alterados são redesenhados.
        adapter.submitList(state.items)
    }

    /** Navegação para a 2ª tela com INTENT EXPLÍCITA (ver ExerciseDetailActivity.newIntent). */
    private fun openExerciseDetail(item: ExerciseItem) {
        val intent = ExerciseDetailActivity.newIntent(
            context = requireContext(),
            prescribedId = item.exercise.id,
            date = viewModel.currentSelectedDate(),
        )
        startActivity(intent)
    }

    private fun confirmRestart() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_reset_day_title)
            .setMessage(R.string.dialog_reset_day_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_restart) { _, _ ->
                viewModel.restartSelectedDay()
                Snackbar.make(binding.root, R.string.message_workout_restarted, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun scrollToTop() {
        val binding = _binding ?: return
        binding.appBar.setExpanded(true, true)
        binding.recyclerExercises.smoothScrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // A RecyclerView guarda referência ao adapter; soltamos para não reter a View antiga.
        binding.recyclerExercises.adapter = null
        _binding = null
    }
}

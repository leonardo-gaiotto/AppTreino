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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.FragmentStudentListBinding
import com.leonardo.apptreino.ui.common.Formatters
import com.leonardo.apptreino.ui.editor.WorkoutEditorActivity
import kotlinx.coroutines.launch

/** Aba "Treinos" do painel: lista dos treinos do aluno com criar, editar e excluir. */
class StudentWorkoutsFragment : Fragment() {

    private var _binding: FragmentStudentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentDashboardViewModel by activityViewModels { StudentDashboardViewModel.Factory }

    private val adapter = WorkoutCardAdapter(
        onEdit = { item -> openEditor(item.workout.id) },
        onDelete = ::confirmDelete,
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.adapter = adapter
        binding.textEmpty.setText(R.string.workouts_empty)
        binding.fab.isVisible = true
        binding.fab.setText(R.string.action_new_workout)
        binding.fab.setOnClickListener { openEditor(workoutId = null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val workouts = state?.workouts.orEmpty()
                    binding.textEmpty.isVisible = workouts.isEmpty()
                    adapter.submitList(workouts)
                }
            }
        }
    }

    /** Abre o editor por Intent explícita (sem ID = novo treino). */
    private fun openEditor(workoutId: String?) {
        startActivity(WorkoutEditorActivity.newIntent(requireContext(), viewModel.studentId, workoutId))
    }

    private fun confirmDelete(item: WorkoutCardItem) {
        val title = Formatters.workoutTitle(resources, item.letter)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_delete_workout_title, title))
            .setMessage(R.string.dialog_delete_workout_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteWorkout(item.workout.id)
                Snackbar.make(binding.root, R.string.message_workout_deleted, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recycler.adapter = null
        _binding = null
    }
}

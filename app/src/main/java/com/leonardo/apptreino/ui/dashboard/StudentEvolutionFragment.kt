package com.leonardo.apptreino.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.FragmentStudentListBinding
import kotlinx.coroutines.launch

/**
 * Aba "Evolução" do painel: um card por exercício que o aluno já registrou, com o
 * valor atual, a variação desde a primeira sessão e um minigráfico.
 * Tocar num card abre o histórico completo, sessão a sessão.
 */
class StudentEvolutionFragment : Fragment() {

    private var _binding: FragmentStudentListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentDashboardViewModel by activityViewModels { StudentDashboardViewModel.Factory }

    private val adapter = EvolutionAdapter(onClick = { evolution ->
        startActivity(
            ExerciseHistoryActivity.newIntent(requireContext(), viewModel.studentId, evolution.exercise.id),
        )
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recycler.adapter = adapter
        // Sem botão flutuante nesta aba: o fim da lista não precisa de espaço extra.
        binding.recycler.updatePadding(bottom = resources.getDimensionPixelSize(R.dimen.list_bottom_padding))
        binding.textEmpty.setText(R.string.evolution_empty)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val evolutions = state?.evolutions.orEmpty()
                    binding.textEmpty.isVisible = evolutions.isEmpty()
                    adapter.submitList(evolutions)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recycler.adapter = null
        _binding = null
    }
}

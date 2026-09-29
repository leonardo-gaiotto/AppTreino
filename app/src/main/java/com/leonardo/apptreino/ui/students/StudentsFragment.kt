package com.leonardo.apptreino.ui.students

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.FragmentStudentsBinding
import com.leonardo.apptreino.ui.common.applySystemBarsPadding
import com.leonardo.apptreino.ui.dashboard.StudentDashboardActivity
import com.leonardo.apptreino.ui.main.MainActivity
import kotlinx.coroutines.launch

/**
 * Aba "Alunos" (só no modo Coach): lista com busca por nome e cadastro de aluno.
 * Tocar num aluno abre o painel dele por Intent explícita.
 */
class StudentsFragment : Fragment(), MainActivity.ScrollableToTop {

    private var _binding: FragmentStudentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentsViewModel by viewModels { StudentsViewModel.Factory }

    private val adapter = StudentAdapter(onStudentClick = { student ->
        startActivity(StudentDashboardActivity.newIntent(requireContext(), student.id))
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applySystemBarsPadding()
        binding.recyclerStudents.adapter = adapter

        binding.inputSearch.doAfterTextChanged { viewModel.search(it?.toString().orEmpty()) }
        binding.fabNewStudent.setOnClickListener {
            startActivity(StudentFormActivity.newIntent(requireContext()))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: StudentsUiState) {
        binding.textStudentsSubtitle.text = resources.getQuantityString(
            R.plurals.students_subtitle, state.totalStudents, state.totalStudents,
        )
        binding.textEmpty.isVisible = state.students.isEmpty()
        adapter.submitList(state.students)
    }

    override fun scrollToTop() {
        val binding = _binding ?: return
        binding.appBar.setExpanded(true, true)
        binding.recyclerStudents.smoothScrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerStudents.adapter = null
        _binding = null
    }
}

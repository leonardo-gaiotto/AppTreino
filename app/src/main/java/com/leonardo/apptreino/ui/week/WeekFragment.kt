package com.leonardo.apptreino.ui.week

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.FragmentWeekBinding
import com.leonardo.apptreino.ui.common.applySystemBarsPadding
import com.leonardo.apptreino.ui.main.MainActivity
import com.leonardo.apptreino.ui.main.MainViewModel
import com.leonardo.apptreino.ui.main.WeekDayItem
import com.leonardo.apptreino.ui.main.WeekUiState
import kotlinx.coroutines.launch

/**
 * Aba "Semana": plano semanal (divisão ABC) com o progresso de cada dia.
 * Tocar em um dia seleciona esse dia no ViewModel compartilhado e abre a aba "Hoje".
 */
class WeekFragment : Fragment(), MainActivity.ScrollableToTop {

    private var _binding: FragmentWeekBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels { MainViewModel.Factory }

    private val adapter = WeekDayAdapter(onDayClick = ::onDayClick)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeekBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Edge-to-edge: o conteúdo começa abaixo da status bar e é cortado nela ao rolar.
        binding.root.applySystemBarsPadding()
        binding.recyclerDays.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weekState.collect(::render)
            }
        }
    }

    private fun render(state: WeekUiState) {
        binding.textWeekSummary.text = resources.getQuantityString(
            R.plurals.week_summary_count, state.plannedWorkouts, state.completedWorkouts, state.plannedWorkouts,
        )
        binding.textWeekPercent.text = getString(R.string.today_progress_percent, state.progressPercent)
        binding.progressWeek.setProgressCompat(state.progressPercent, true)
        adapter.submitList(state.days)
    }

    private fun onDayClick(day: WeekDayItem) {
        viewModel.selectDate(day.date)
        MainActivity.requestTab(parentFragmentManager, R.id.nav_today)
    }

    override fun scrollToTop() {
        val binding = _binding ?: return
        binding.appBar.setExpanded(true, true)
        binding.recyclerDays.smoothScrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerDays.adapter = null
        _binding = null
    }
}

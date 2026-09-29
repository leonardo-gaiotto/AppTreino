package com.leonardo.apptreino.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.ActivityStudentDashboardBinding
import com.leonardo.apptreino.ui.common.applySystemBarsPadding
import com.leonardo.apptreino.ui.common.bindAvatar
import kotlinx.coroutines.launch

/**
 * Painel do aluno (modo Coach), aberto pela aba "Alunos" com Intent explícita.
 *
 * Três abas, cada uma um Fragment com ViewBinding, num ViewPager2:
 * - Resumo: dados do aluno, objetivo, semana e sequência de treinos;
 * - Treinos: criar, editar e excluir os treinos do aluno;
 * - Evolução: histórico de carga/repetições de cada exercício.
 */
class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDashboardBinding

    /** Compartilhado com os 3 Fragments (activityViewModels). */
    private val viewModel: StudentDashboardViewModel by viewModels { StudentDashboardViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (viewModel.uiState.value == null) {
            finish()
            return
        }

        binding = ActivityStudentDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(top = true, bottom = true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.viewPager.adapter = DashboardPagerAdapter()
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.setText(TABS[position])
        }.attach()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state == null) finish() else render(state)
                }
            }
        }
    }

    private fun render(state: DashboardUiState) {
        val name = state.student.name.resolve(resources)
        binding.textAvatar.bindAvatar(name, state.colorIndex)
        binding.textName.text = name
        binding.textGoal.setText(state.student.goal.labelRes)
    }

    /** Cria o Fragment de cada aba sob demanda. */
    private inner class DashboardPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount(): Int = TABS.size

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> StudentSummaryFragment()
            1 -> StudentWorkoutsFragment()
            else -> StudentEvolutionFragment()
        }
    }

    companion object {
        const val EXTRA_STUDENT_ID = "extra_student_id"

        private val TABS = intArrayOf(R.string.tab_summary, R.string.tab_workouts, R.string.tab_evolution)

        /** Intent EXPLÍCITA para o painel do aluno [studentId]. */
        fun newIntent(context: Context, studentId: String): Intent =
            Intent(context, StudentDashboardActivity::class.java).putExtra(EXTRA_STUDENT_ID, studentId)
    }
}

package com.leonardo.apptreino.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.leonardo.apptreino.R
import com.leonardo.apptreino.databinding.ActivityExerciseHistoryBinding
import com.leonardo.apptreino.databinding.ViewStatTileBinding
import com.leonardo.apptreino.ui.common.EvolutionFormat
import com.leonardo.apptreino.ui.common.Formatters
import com.leonardo.apptreino.ui.common.applySystemBarsPadding
import kotlinx.coroutines.launch

/**
 * Evolução completa de UM exercício de um aluno: gráfico por sessão e a lista de
 * sessões com cada série (repetições × carga) e indicador de subida/queda.
 * Aberta pela aba "Evolução" com Intent explícita.
 */
class ExerciseHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExerciseHistoryBinding

    private val viewModel: ExerciseHistoryViewModel by viewModels { ExerciseHistoryViewModel.Factory }

    private val sessionAdapter = SessionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (viewModel.uiState.value == null) {
            finish()
            return
        }

        binding = ActivityExerciseHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding(top = true, bottom = true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerSessions.adapter = sessionAdapter
        binding.chart.showGrid = true

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> if (state == null) finish() else render(state) }
            }
        }
    }

    private fun render(state: ExerciseHistoryUiState) {
        val evolution = state.evolution
        val metric = evolution.metric

        binding.textStudent.text = state.studentName.resolve(resources)
        binding.textExercise.setText(evolution.exercise.nameRes)
        binding.textMetric.text = getString(R.string.history_metric_subtitle, EvolutionFormat.metricLabel(resources, metric))

        binding.statCurrent.bind(EvolutionFormat.value(resources, metric, evolution.latest.value), R.string.history_stat_current)
        binding.statChange.bind(EvolutionFormat.delta(resources, metric, evolution.totalChange), R.string.history_stat_change)
        binding.statSessions.bind(Formatters.integer(resources, evolution.sessions.size), R.string.history_stat_sessions)

        binding.chart.labelFormatter = { EvolutionFormat.value(resources, metric, it.toDouble()) }
        binding.chart.setValues(evolution.sessions.map { it.value })
        binding.chart.contentDescription = getString(
            R.string.cd_evolution_chart,
            getString(evolution.exercise.nameRes),
            EvolutionFormat.delta(resources, metric, evolution.totalChange),
        )
        binding.textFirstDate.text = Formatters.shortDate(resources, evolution.sessions.first().date)
        binding.textLastDate.text = Formatters.shortDate(resources, evolution.latest.date)

        sessionAdapter.submitList(state.sessions)
    }

    private fun ViewStatTileBinding.bind(value: String, labelRes: Int) {
        textValue.text = value
        textLabel.setText(labelRes)
    }

    companion object {
        const val EXTRA_STUDENT_ID = "extra_student_id"
        const val EXTRA_CATALOG_ID = "extra_catalog_id"

        /** Intent EXPLÍCITA para o histórico do exercício [catalogId] do aluno [studentId]. */
        fun newIntent(context: Context, studentId: String, catalogId: String): Intent =
            Intent(context, ExerciseHistoryActivity::class.java)
                .putExtra(EXTRA_STUDENT_ID, studentId)
                .putExtra(EXTRA_CATALOG_ID, catalogId)
    }
}

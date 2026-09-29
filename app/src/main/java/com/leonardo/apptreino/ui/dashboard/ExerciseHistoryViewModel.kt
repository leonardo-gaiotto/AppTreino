package com.leonardo.apptreino.ui.dashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leonardo.apptreino.AppTreinoApplication
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.data.GymState
import com.leonardo.apptreino.data.model.UiText
import com.leonardo.apptreino.domain.EvolutionCalculator
import com.leonardo.apptreino.domain.EvolutionMetric
import com.leonardo.apptreino.domain.EvolutionSession
import com.leonardo.apptreino.domain.ExerciseEvolution
import com.leonardo.apptreino.domain.Trend
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Uma sessão na lista do histórico, já com a tendência em relação à anterior. */
data class SessionRow(
    val session: EvolutionSession,
    val metric: EvolutionMetric,
    val isIsometric: Boolean,
    val trend: Trend,
)

data class ExerciseHistoryUiState(
    val studentName: UiText,
    val evolution: ExerciseEvolution,
    /** Sessões da mais recente para a mais antiga. */
    val sessions: List<SessionRow>,
)

/** ViewModel do histórico de um exercício de um aluno (IDs vindos da Intent). */
class ExerciseHistoryViewModel(
    savedStateHandle: SavedStateHandle,
    repository: GymRepository,
) : ViewModel() {

    private val studentId = savedStateHandle.get<String>(ExerciseHistoryActivity.EXTRA_STUDENT_ID).orEmpty()
    private val catalogId = savedStateHandle.get<String>(ExerciseHistoryActivity.EXTRA_CATALOG_ID).orEmpty()

    val uiState: StateFlow<ExerciseHistoryUiState?> =
        repository.state.map(::buildState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(repository.state.value))

    private fun buildState(state: GymState): ExerciseHistoryUiState? {
        val student = state.student(studentId) ?: return null
        val evolution = EvolutionCalculator.evolutionOf(state, studentId, catalogId) ?: return null
        val rows = evolution.sessions.mapIndexed { index, session ->
            SessionRow(
                session = session,
                metric = evolution.metric,
                isIsometric = evolution.exercise.isIsometric,
                trend = evolution.trendOfSession(index),
            )
        }
        return ExerciseHistoryUiState(student.name, evolution, rows.reversed())
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppTreinoApplication
                ExerciseHistoryViewModel(createSavedStateHandle(), app.gymRepository)
            }
        }
    }
}

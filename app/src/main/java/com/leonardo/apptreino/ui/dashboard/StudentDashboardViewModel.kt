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
import com.leonardo.apptreino.data.model.Student
import com.leonardo.apptreino.data.model.Workout
import com.leonardo.apptreino.domain.DayProgress
import com.leonardo.apptreino.domain.EvolutionCalculator
import com.leonardo.apptreino.domain.ExerciseEvolution
import com.leonardo.apptreino.domain.ProgressCalculator
import com.leonardo.apptreino.domain.WeekSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Um treino na aba "Treinos" do painel. */
data class WorkoutCardItem(val workout: Workout, val letter: Char)

/** Tudo o que o painel do aluno mostra nas 3 abas. */
data class DashboardUiState(
    val student: Student,
    /** Posição do aluno na lista: define a cor do avatar. */
    val colorIndex: Int,
    val week: WeekSummary,
    val streak: Int,
    val completedWorkouts: Int,
    val today: DayProgress?,
    val todayLetter: Char?,
    val workouts: List<WorkoutCardItem>,
    val evolutions: List<ExerciseEvolution>,
)

/**
 * ViewModel do painel do aluno, compartilhado pelos Fragments Resumo, Treinos e Evolução
 * (cada um usa `activityViewModels()`). O ID do aluno chega pelo extra da Intent.
 */
class StudentDashboardViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GymRepository,
    private val clock: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    val studentId: String = savedStateHandle.get<String>(StudentDashboardActivity.EXTRA_STUDENT_ID).orEmpty()

    /** `null` se o aluno não existir. */
    val uiState: StateFlow<DashboardUiState?> =
        repository.state.map(::buildState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(repository.state.value))

    fun deleteWorkout(workoutId: String) {
        repository.deleteWorkout(studentId, workoutId)
    }

    private fun buildState(state: GymState): DashboardUiState? {
        val student = state.student(studentId) ?: return null
        val today = clock()
        val todayProgress = ProgressCalculator.dayProgress(state, student, today)
        return DashboardUiState(
            student = student,
            colorIndex = state.students.indexOf(student),
            week = ProgressCalculator.weekSummary(state, student, today),
            streak = ProgressCalculator.streak(state, student, today),
            completedWorkouts = ProgressCalculator.completedWorkouts(state, student),
            today = todayProgress,
            todayLetter = todayProgress?.workout?.let { student.letterOf(it.id) },
            workouts = student.workouts.map { WorkoutCardItem(it, student.letterOf(it.id)) },
            evolutions = EvolutionCalculator.evolutionsFor(state, studentId),
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppTreinoApplication
                StudentDashboardViewModel(createSavedStateHandle(), app.gymRepository)
            }
        }
    }
}

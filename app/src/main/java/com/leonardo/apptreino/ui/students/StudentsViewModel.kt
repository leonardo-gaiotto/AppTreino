package com.leonardo.apptreino.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leonardo.apptreino.AppTreinoApplication
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.data.GymState
import com.leonardo.apptreino.data.model.Goal
import com.leonardo.apptreino.data.model.UiText
import com.leonardo.apptreino.data.model.Workout
import com.leonardo.apptreino.data.normalizeForSearch
import com.leonardo.apptreino.domain.DayProgress
import com.leonardo.apptreino.domain.ProgressCalculator
import com.leonardo.apptreino.domain.WeekSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Um aluno na lista do coach. */
data class StudentListItem(
    val id: String,
    val name: String,
    val goal: Goal,
    /** Posição na lista completa: define a cor do avatar. */
    val colorIndex: Int,
    val todayWorkout: Workout?,
    val todayLetter: Char?,
    val todayProgress: DayProgress?,
    val week: WeekSummary,
)

data class StudentsUiState(
    val totalStudents: Int,
    val students: List<StudentListItem>,
    val query: String,
)

/** ViewModel da aba "Alunos" (modo Coach): lista e busca por nome. */
class StudentsViewModel(
    private val repository: GymRepository,
    private val resolve: (UiText) -> String,
    private val clock: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val query = MutableStateFlow("")

    val uiState: StateFlow<StudentsUiState> =
        combine(repository.state, query) { state, query -> buildState(state, query) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildState(repository.state.value, ""))

    fun search(text: String) {
        query.value = text
    }

    private fun buildState(state: GymState, query: String): StudentsUiState {
        val today = clock()
        val normalizedQuery = query.trim().normalizeForSearch()
        val items = state.students.mapIndexed { index, student ->
            val todayWorkout = student.workoutFor(today.dayOfWeek)
            StudentListItem(
                id = student.id,
                name = resolve(student.name),
                goal = student.goal,
                colorIndex = index,
                todayWorkout = todayWorkout,
                todayLetter = todayWorkout?.let { student.letterOf(it.id) },
                todayProgress = ProgressCalculator.dayProgress(state, student, today),
                week = ProgressCalculator.weekSummary(state, student, today),
            )
        }
        return StudentsUiState(
            totalStudents = items.size,
            students = items.filter { normalizedQuery.isEmpty() || it.name.normalizeForSearch().contains(normalizedQuery) },
            query = query,
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppTreinoApplication
                StudentsViewModel(app.gymRepository, app.textResolver)
            }
        }
    }
}

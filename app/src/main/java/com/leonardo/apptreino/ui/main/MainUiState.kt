package com.leonardo.apptreino.ui.main

import com.leonardo.apptreino.data.ExerciseCatalog
import com.leonardo.apptreino.data.GymState
import com.leonardo.apptreino.data.model.Cardio
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.data.model.Goal
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Student
import com.leonardo.apptreino.data.model.UiText
import com.leonardo.apptreino.data.model.Workout
import com.leonardo.apptreino.domain.DayProgress
import com.leonardo.apptreino.domain.ProgressCalculator
import java.time.LocalDate

/*
 * Estados de interface das abas do aluno. São classes imutáveis: a tela apenas
 * "desenha" o estado recebido, sem guardar lógica de negócio.
 */

/** Um exercício na lista do dia, já com o status do registro. */
data class ExerciseItem(
    val exercise: PrescribedExercise,
    val catalog: CatalogExercise,
    /** Posição do exercício no treino (1, 2, 3...). */
    val number: Int,
    val loggedSets: Int,
) {
    val isDone: Boolean get() = loggedSets >= exercise.sets
    val isPartial: Boolean get() = loggedSets in 1 until exercise.sets
}

/** Um cardio na lista do dia. */
data class CardioItem(val cardio: Cardio, val isDone: Boolean)

/** Itens da lista da aba "Hoje" — o adapter tem um tipo de View para cada um. */
sealed interface TodayListItem {
    /** Chave estável usada pelo DiffUtil para saber se é "o mesmo item". */
    val key: String

    data class ExerciseRow(val item: ExerciseItem) : TodayListItem {
        override val key: String get() = "exercise_${item.exercise.id}"
    }

    data object CardioHeader : TodayListItem {
        override val key: String get() = "cardio_header"
    }

    data class CardioRow(val item: CardioItem) : TodayListItem {
        override val key: String get() = "cardio_${item.cardio.id}"
    }
}

/** Estado da aba "Hoje". */
sealed interface TodayUiState {
    val date: LocalDate
    val isToday: Boolean

    data class Training(
        override val date: LocalDate,
        override val isToday: Boolean,
        val workout: Workout,
        val letter: Char,
        val progress: DayProgress,
        val items: List<TodayListItem>,
    ) : TodayUiState

    data class Rest(
        override val date: LocalDate,
        override val isToday: Boolean,
    ) : TodayUiState
}

/** Situação de um dia na aba "Semana". */
enum class DayStatus { REST, DONE, IN_PROGRESS, TODAY, UPCOMING, MISSED }

/** Um dia da semana na aba "Semana". */
data class WeekDayItem(
    val date: LocalDate,
    val workout: Workout?,
    val letter: Char?,
    val progress: DayProgress?,
    val isToday: Boolean,
    val isSelected: Boolean,
    val status: DayStatus,
)

/** Estado da aba "Semana". */
data class WeekUiState(val days: List<WeekDayItem>) {
    val plannedWorkouts: Int get() = days.count { it.workout != null }
    val completedWorkouts: Int get() = days.count { it.status == DayStatus.DONE }
    val progressPercent: Int
        get() = if (plannedWorkouts == 0) 0 else completedWorkouts * 100 / plannedWorkouts
}

/** Dados da aba "Perfil" (parte do aluno e parte do coach). */
data class ProfileUiState(
    val studentName: UiText,
    val goal: Goal,
    /** Letras dos treinos, ex.: "A · B · C". */
    val workoutLetters: List<Char>,
    val trainingDaysPerWeek: Int,
    val completedWorkouts: Int,
    val completedExercises: Int,
    val activeDaysThisWeek: Int,
    val coachStudents: Int,
    val coachWorkouts: Int,
    val coachWeekAverage: Int,
)

/**
 * Funções puras que transformam o estado do repositório em estados de tela.
 * Por não dependerem do Android, são cobertas por testes unitários simples.
 */
object MainUiStateMapper {

    fun buildToday(state: GymState, student: Student, date: LocalDate, today: LocalDate): TodayUiState {
        val progress = ProgressCalculator.dayProgress(state, student, date)
            ?: return TodayUiState.Rest(date = date, isToday = date == today)
        val workout = progress.workout

        val exerciseRows = workout.exercises.mapIndexed { index, exercise ->
            TodayListItem.ExerciseRow(
                ExerciseItem(
                    exercise = exercise,
                    catalog = ExerciseCatalog.require(exercise.catalogId),
                    number = index + 1,
                    loggedSets = state.exerciseLog(student.id, date, exercise.id)?.sets?.size ?: 0,
                ),
            )
        }
        val cardioRows = workout.cardio.map { cardio ->
            TodayListItem.CardioRow(CardioItem(cardio, state.isCardioDone(student.id, date, cardio.id)))
        }
        val items = if (cardioRows.isEmpty()) exerciseRows else exerciseRows + TodayListItem.CardioHeader + cardioRows

        return TodayUiState.Training(
            date = date,
            isToday = date == today,
            workout = workout,
            letter = student.letterOf(workout.id),
            progress = progress,
            items = items,
        )
    }

    fun buildWeek(state: GymState, student: Student, selectedDate: LocalDate, today: LocalDate): WeekUiState =
        WeekUiState(
            ProgressCalculator.weekOf(today).map { date ->
                val progress = ProgressCalculator.dayProgress(state, student, date)
                WeekDayItem(
                    date = date,
                    workout = progress?.workout,
                    letter = progress?.workout?.let { student.letterOf(it.id) },
                    progress = progress,
                    isToday = date == today,
                    isSelected = date == selectedDate,
                    status = statusOf(date, today, progress),
                )
            },
        )

    fun buildProfile(
        state: GymState,
        student: Student,
        today: LocalDate,
    ): ProfileUiState = ProfileUiState(
        studentName = student.name,
        goal = student.goal,
        workoutLetters = student.workouts.map { student.letterOf(it.id) },
        trainingDaysPerWeek = student.workouts.sumOf { it.days.size },
        completedWorkouts = ProgressCalculator.completedWorkouts(state, student),
        completedExercises = ProgressCalculator.completedExercises(state, student),
        activeDaysThisWeek = ProgressCalculator.activeDaysThisWeek(state, student, today),
        coachStudents = state.students.size,
        coachWorkouts = state.students.sumOf { it.workouts.size },
        coachWeekAverage = state.students
            .map { ProgressCalculator.weekSummary(state, it, today).percent }
            .average()
            .takeIf { !it.isNaN() }
            ?.toInt() ?: 0,
    )

    fun statusOf(date: LocalDate, today: LocalDate, progress: DayProgress?): DayStatus = when {
        progress == null -> DayStatus.REST
        progress.isComplete -> DayStatus.DONE
        progress.isStarted -> DayStatus.IN_PROGRESS
        date == today -> DayStatus.TODAY
        date.isAfter(today) -> DayStatus.UPCOMING
        else -> DayStatus.MISSED
    }
}

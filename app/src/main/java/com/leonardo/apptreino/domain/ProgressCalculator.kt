package com.leonardo.apptreino.domain

import com.leonardo.apptreino.data.GymState
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Student
import com.leonardo.apptreino.data.model.Workout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Progresso de um aluno em um dia de treino. */
data class DayProgress(
    val workout: Workout,
    val doneExercises: Int,
    val doneCardio: Int,
) {
    val totalItems: Int get() = workout.itemCount
    val doneItems: Int get() = doneExercises + doneCardio
    val percent: Int get() = if (totalItems == 0) 0 else doneItems * 100 / totalItems
    val isComplete: Boolean get() = totalItems > 0 && doneItems == totalItems
    val isStarted: Boolean get() = doneItems > 0
}

/** Resumo da semana: treinos previstos e concluídos. */
data class WeekSummary(val planned: Int, val completed: Int) {
    val percent: Int get() = if (planned == 0) 0 else completed * 100 / planned
}

/**
 * Regras de progresso, em funções puras (sem Android): fáceis de testar e reaproveitadas
 * pelas telas do aluno e do coach.
 */
object ProgressCalculator {

    /** Um exercício está feito quando TODAS as séries prescritas foram registradas. */
    fun isExerciseDone(state: GymState, studentId: String, date: LocalDate, exercise: PrescribedExercise): Boolean {
        val registered = state.exerciseLog(studentId, date, exercise.id)?.sets?.size ?: 0
        return registered >= exercise.sets
    }

    /** Progresso do dia, ou `null` quando é dia de descanso. */
    fun dayProgress(state: GymState, student: Student, date: LocalDate): DayProgress? {
        val workout = student.workoutFor(date.dayOfWeek) ?: return null
        return DayProgress(
            workout = workout,
            doneExercises = workout.exercises.count { isExerciseDone(state, student.id, date, it) },
            doneCardio = workout.cardio.count { state.isCardioDone(student.id, date, it.id) },
        )
    }

    /** Os 7 dias (segunda a domingo) da semana que contém [date]. */
    fun weekOf(date: LocalDate): List<LocalDate> {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L until 7L).map { monday.plusDays(it) }
    }

    fun weekSummary(state: GymState, student: Student, today: LocalDate): WeekSummary {
        val days = weekOf(today).mapNotNull { dayProgress(state, student, it) }
        return WeekSummary(planned = days.size, completed = days.count { it.isComplete })
    }

    /**
     * Sequência de treinos concluídos seguidos, contando para trás a partir de hoje.
     * Dias de descanso não quebram a sequência, e o treino de hoje ainda em andamento também não.
     */
    fun streak(state: GymState, student: Student, today: LocalDate): Int {
        var streak = 0
        for (daysAgo in 0L..MAX_STREAK_LOOKBACK_DAYS) {
            val date = today.minusDays(daysAgo)
            val progress = dayProgress(state, student, date) ?: continue
            when {
                progress.isComplete -> streak++
                date == today -> continue
                else -> return streak
            }
        }
        return streak
    }

    /** Total de treinos concluídos pelo aluno em todo o histórico. */
    fun completedWorkouts(state: GymState, student: Student): Int =
        loggedDates(state, student.id).count { dayProgress(state, student, it)?.isComplete == true }

    /** Total de exercícios concluídos (todas as séries registradas) em todo o histórico. */
    fun completedExercises(state: GymState, student: Student): Int =
        loggedDates(state, student.id).sumOf { date -> dayProgress(state, student, date)?.doneExercises ?: 0 }

    /** Dias da semana atual em que o aluno registrou alguma coisa. */
    fun activeDaysThisWeek(state: GymState, student: Student, today: LocalDate): Int {
        val week = weekOf(today).toSet()
        return loggedDates(state, student.id).count { it in week }
    }

    private fun loggedDates(state: GymState, studentId: String): Set<LocalDate> =
        state.exerciseLogs.keys.filter { it.studentId == studentId }.map { it.date }.toSet() +
            state.cardioLogs.filter { it.studentId == studentId }.map { it.date }

    private const val MAX_STREAK_LOOKBACK_DAYS = 366L
}

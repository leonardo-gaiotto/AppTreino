package com.leonardo.apptreino.data.model

import java.time.DayOfWeek

/** Um aluno do coach, com seus dados físicos e seus treinos. */
data class Student(
    val id: String,
    val name: UiText,
    val goal: Goal,
    val age: Int,
    val weightKg: Double,
    val heightCm: Int,
    val notes: UiText? = null,
    val workouts: List<Workout> = emptyList(),
) {
    /** Índice de Massa Corporal (peso / altura²). */
    val bmi: Double get() = weightKg / ((heightCm / 100.0) * (heightCm / 100.0))

    /** Treino do dia da semana, ou `null` quando é dia de descanso. */
    fun workoutFor(day: DayOfWeek): Workout? = workouts.firstOrNull { day in it.days }

    fun findWorkout(workoutId: String): Workout? = workouts.firstOrNull { it.id == workoutId }

    /** Letra do treino pela ordem: primeiro = A, segundo = B... */
    fun letterOf(workoutId: String): Char = 'A' + workouts.indexOfFirst { it.id == workoutId }.coerceAtLeast(0)
}

package com.leonardo.apptreino

import com.leonardo.apptreino.data.GymState
import com.leonardo.apptreino.data.model.Cardio
import com.leonardo.apptreino.data.model.CardioType
import com.leonardo.apptreino.data.model.Goal
import com.leonardo.apptreino.data.model.Intensity
import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.data.model.SetLog
import com.leonardo.apptreino.data.model.Student
import com.leonardo.apptreino.data.model.UiText
import com.leonardo.apptreino.data.model.Workout
import java.time.DayOfWeek
import java.time.LocalDate

/** Dados pequenos e previsíveis para os testes (independentes dos mocks do app). */
object TestData {

    /** 28/09/2026 é uma segunda-feira. */
    val MONDAY: LocalDate = LocalDate.of(2026, 9, 28)

    const val STUDENT_ID = "aluno_teste"

    fun exercise(id: String, catalogId: String = "bench_press", sets: Int = 3, load: Double = 40.0) =
        PrescribedExercise(
            id = id,
            catalogId = catalogId,
            sets = sets,
            reps = Reps.Range(8, 10),
            load = Load.Kilograms(load),
            restSeconds = 60,
        )

    /** Treino "a" (seg e qua) com 2 exercícios e 1 cardio; treino "b" (ter) com 1 exercício. */
    val workoutA = Workout(
        id = "a",
        name = UiText.Dynamic("Treino de teste A"),
        days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
        exercises = listOf(exercise("a1", "bench_press"), exercise("a2", "pec_deck")),
        cardio = listOf(Cardio("a_c1", CardioType.TREADMILL, 15, Intensity.MODERATE)),
    )

    val workoutB = Workout(
        id = "b",
        name = UiText.Dynamic("Treino de teste B"),
        days = setOf(DayOfWeek.TUESDAY),
        exercises = listOf(exercise("b1", "lat_pulldown", sets = 2)),
    )

    val student = Student(
        id = STUDENT_ID,
        name = UiText.Dynamic("Aluno Teste"),
        goal = Goal.HYPERTROPHY,
        age = 25,
        weightKg = 80.0,
        heightCm = 180,
        workouts = listOf(workoutA, workoutB),
    )

    fun emptyState(): GymState = GymState(students = listOf(student), exerciseLogs = emptyMap(), cardioLogs = emptySet())

    fun setLog(reps: Int = 10, load: Double = 40.0) = SetLog(reps, load)
}

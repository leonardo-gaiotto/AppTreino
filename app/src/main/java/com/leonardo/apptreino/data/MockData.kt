package com.leonardo.apptreino.data

import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.Cardio
import com.leonardo.apptreino.data.model.CardioLogKey
import com.leonardo.apptreino.data.model.CardioType
import com.leonardo.apptreino.data.model.ExerciseLog
import com.leonardo.apptreino.data.model.ExerciseLogKey
import com.leonardo.apptreino.data.model.Goal
import com.leonardo.apptreino.data.model.Intensity
import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.data.model.SetLog
import com.leonardo.apptreino.data.model.Student
import com.leonardo.apptreino.data.model.UiText
import com.leonardo.apptreino.data.model.Workout
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Dados de demonstração: os 5 alunos do coach, seus treinos e algumas semanas de
 * histórico. Não há API nem banco de dados — tudo nasce aqui e vive em memória.
 *
 * O histórico é gerado com uma semente fixa ([Random] determinístico): a cada execução
 * os números são os mesmos, e a carga sobe aos poucos ao longo das semanas.
 */
object MockData {

    /** Aluno usado no modo Aluno (o dono do celular). */
    const val LEONARDO_ID = "leonardo"

    /** Quantos dias de histórico gerar (6 semanas). */
    const val HISTORY_DAYS = 42L

    /** Chance (em %) de o aluno ter faltado em um dia de treino passado. */
    private const val MISS_CHANCE = 12

    fun create(today: LocalDate): GymState {
        val students = listOf(leonardo(), ana(), bruno(), carla(), diego())
        val exerciseLogs = mutableMapOf<ExerciseLogKey, ExerciseLog>()
        val cardioLogs = mutableSetOf<CardioLogKey>()

        students.forEachIndexed { index, student ->
            generateHistory(student, today, Random(seed = SEED + index), exerciseLogs, cardioLogs)
        }
        return GymState(students = students, exerciseLogs = exerciseLogs.toMap(), cardioLogs = cardioLogs.toSet())
    }

    // ------------------------------------------------------------------ Alunos

    private fun leonardo() = Student(
        id = LEONARDO_ID,
        name = UiText.Resource(R.string.mock_student_leonardo),
        goal = Goal.HYPERTROPHY,
        age = 22, weightKg = 76.0, heightCm = 178,
        notes = UiText.Resource(R.string.mock_notes_leonardo),
        workouts = listOf(
            Workout(
                id = "leo_a",
                name = UiText.Resource(R.string.mock_workout_push),
                days = setOf(MONDAY, THURSDAY),
                exercises = listOf(
                    ex("leo_a", "bench_press", 4, reps(8, 10), kg(40), 90, R.string.ex_bench_press_note),
                    ex("leo_a", "incline_db_press", 3, reps(10, 12), kg(18, perSide = true), 75),
                    ex("leo_a", "pec_deck", 3, reps(12, 15), kg(35), 60, R.string.ex_pec_deck_note),
                    ex("leo_a", "db_shoulder_press", 4, reps(8, 10), kg(14, perSide = true), 90),
                    ex("leo_a", "lateral_raise", 4, reps(12, 15), kg(8, perSide = true), 60, R.string.ex_lateral_raise_note),
                    ex("leo_a", "rope_pushdown", 3, reps(12, 15), kg(25), 60),
                    ex("leo_a", "overhead_extension", 3, reps(10, 12), kg(14), 60),
                ),
                cardio = listOf(
                    Cardio("leo_a_c1", CardioType.TREADMILL, 15, Intensity.MODERATE, UiText.Resource(R.string.mock_cardio_note_finisher)),
                ),
            ),
            Workout(
                id = "leo_b",
                name = UiText.Resource(R.string.mock_workout_pull),
                days = setOf(TUESDAY, FRIDAY),
                exercises = listOf(
                    ex("leo_b", "lat_pulldown", 4, reps(8, 10), kg(50), 90, R.string.ex_lat_pulldown_note),
                    ex("leo_b", "barbell_row", 4, reps(8, 10), kg(40), 90),
                    ex("leo_b", "seated_row", 3, reps(10, 12), kg(45), 75),
                    ex("leo_b", "one_arm_row", 3, reps(10, 12), kg(22), 60),
                    ex("leo_b", "face_pull", 3, reps(15, 15), kg(20), 60, R.string.ex_face_pull_note),
                    ex("leo_b", "barbell_curl", 3, reps(10, 12), kg(25), 60),
                    ex("leo_b", "hammer_curl", 3, reps(10, 12), kg(12, perSide = true), 60),
                ),
                cardio = listOf(Cardio("leo_b_c1", CardioType.BIKE, 15, Intensity.LIGHT)),
            ),
            Workout(
                id = "leo_c",
                name = UiText.Resource(R.string.mock_workout_legs),
                days = setOf(WEDNESDAY, SATURDAY),
                exercises = listOf(
                    ex("leo_c", "back_squat", 4, reps(6, 8), kg(60), 120, R.string.ex_back_squat_note),
                    ex("leo_c", "leg_press", 4, reps(10, 12), kg(180), 90),
                    ex("leo_c", "stiff", 3, reps(10, 12), kg(40), 90, R.string.ex_stiff_note),
                    ex("leo_c", "leg_extension", 3, reps(12, 15), kg(45), 60),
                    ex("leo_c", "leg_curl", 3, reps(10, 12), kg(35), 60),
                    ex("leo_c", "standing_calf_raise", 4, reps(12, 15), kg(60), 45, R.string.ex_standing_calf_raise_note),
                    ex("leo_c", "plank", 3, Reps.Duration(45), Load.BodyWeight, 45),
                ),
            ),
        ),
    )

    private fun ana() = Student(
        id = "ana",
        name = UiText.Resource(R.string.mock_student_ana),
        goal = Goal.WEIGHT_LOSS,
        age = 28, weightKg = 68.0, heightCm = 165,
        notes = UiText.Resource(R.string.mock_notes_ana),
        workouts = listOf(
            Workout(
                id = "ana_a",
                name = UiText.Resource(R.string.mock_workout_full_body_1),
                days = setOf(MONDAY, WEDNESDAY, FRIDAY),
                exercises = listOf(
                    ex("ana_a", "leg_press", 3, reps(12, 15), kg(90), 60),
                    ex("ana_a", "lat_pulldown", 3, reps(12, 15), kg(30), 60),
                    ex("ana_a", "incline_db_press", 3, reps(12, 15), kg(8, perSide = true), 60),
                    ex("ana_a", "lunge", 3, reps(10, 12), kg(6, perSide = true), 60),
                    ex("ana_a", "seated_row", 3, reps(12, 15), kg(30), 60),
                    ex("ana_a", "plank", 3, Reps.Duration(30), Load.BodyWeight, 30),
                ),
                cardio = listOf(
                    Cardio("ana_a_c1", CardioType.ELLIPTICAL, 25, Intensity.MODERATE, UiText.Resource(R.string.mock_cardio_note_knee)),
                ),
            ),
            Workout(
                id = "ana_b",
                name = UiText.Resource(R.string.mock_workout_full_body_2),
                days = setOf(TUESDAY, THURSDAY),
                exercises = listOf(
                    ex("ana_b", "hip_thrust", 4, reps(12, 15), kg(40), 60),
                    ex("ana_b", "one_arm_row", 3, reps(12, 15), kg(12), 60),
                    ex("ana_b", "db_shoulder_press", 3, reps(12, 15), kg(6, perSide = true), 60),
                    ex("ana_b", "leg_curl", 3, reps(12, 15), kg(25), 60),
                    ex("ana_b", "hip_abduction", 3, reps(15, 20), kg(35), 45),
                    ex("ana_b", "crunch", 3, reps(15, 20), Load.BodyWeight, 45),
                ),
                cardio = listOf(
                    Cardio("ana_b_c1", CardioType.JUMP_ROPE, 10, Intensity.HIGH),
                    Cardio("ana_b_c2", CardioType.TREADMILL, 20, Intensity.LIGHT),
                ),
            ),
        ),
    )

    private fun bruno() = Student(
        id = "bruno",
        name = UiText.Resource(R.string.mock_student_bruno),
        goal = Goal.CONDITIONING,
        age = 34, weightKg = 82.0, heightCm = 180,
        workouts = listOf(
            Workout(
                id = "bruno_a",
                name = UiText.Resource(R.string.mock_workout_strength_conditioning),
                days = setOf(MONDAY, WEDNESDAY, FRIDAY),
                exercises = listOf(
                    ex("bruno_a", "pull_up", 4, reps(6, 8), Load.BodyWeight, 90),
                    ex("bruno_a", "dips", 4, reps(8, 10), Load.BodyWeight, 90),
                    ex("bruno_a", "bulgarian_squat", 3, reps(8, 10), kg(14, perSide = true), 75),
                    ex("bruno_a", "barbell_row", 4, reps(8, 10), kg(50), 90),
                    ex("bruno_a", "face_pull", 3, reps(15, 15), kg(20), 60),
                    ex("bruno_a", "plank", 3, Reps.Duration(60), Load.BodyWeight, 45),
                ),
                cardio = listOf(Cardio("bruno_a_c1", CardioType.ROWING, 15, Intensity.HIGH)),
            ),
            Workout(
                id = "bruno_b",
                name = UiText.Resource(R.string.mock_workout_core_cardio),
                days = setOf(SATURDAY),
                exercises = listOf(
                    ex("bruno_b", "crunch", 3, reps(20, 25), Load.BodyWeight, 45),
                    ex("bruno_b", "plank", 3, Reps.Duration(60), Load.BodyWeight, 45),
                    ex("bruno_b", "lunge", 3, reps(12, 12), kg(10, perSide = true), 60),
                    ex("bruno_b", "hip_abduction", 3, reps(15, 20), kg(45), 45),
                    ex("bruno_b", "face_pull", 3, reps(15, 15), kg(20), 45),
                    ex("bruno_b", "hammer_curl", 3, reps(12, 15), kg(12, perSide = true), 45),
                ),
                cardio = listOf(
                    Cardio("bruno_b_c1", CardioType.STAIRS, 20, Intensity.MODERATE),
                    Cardio("bruno_b_c2", CardioType.BIKE, 20, Intensity.LIGHT),
                ),
            ),
        ),
    )

    private fun carla() = Student(
        id = "carla",
        name = UiText.Resource(R.string.mock_student_carla),
        goal = Goal.STRENGTH,
        age = 41, weightKg = 61.0, heightCm = 162,
        workouts = listOf(
            Workout(
                id = "carla_a",
                name = UiText.Resource(R.string.mock_workout_lower_push),
                days = setOf(MONDAY, THURSDAY),
                exercises = listOf(
                    ex("carla_a", "back_squat", 5, reps(4, 6), kg(55), 150, R.string.mock_note_carla_squat),
                    ex("carla_a", "bench_press", 5, reps(4, 6), kg(35), 150),
                    ex("carla_a", "barbell_row", 4, reps(6, 8), kg(35), 120),
                    ex("carla_a", "leg_curl", 3, reps(8, 10), kg(30), 90),
                    ex("carla_a", "standing_calf_raise", 3, reps(10, 12), kg(50), 60),
                    ex("carla_a", "plank", 3, Reps.Duration(45), Load.BodyWeight, 45),
                ),
            ),
            Workout(
                id = "carla_b",
                name = UiText.Resource(R.string.mock_workout_posterior_pull),
                days = setOf(TUESDAY, FRIDAY),
                exercises = listOf(
                    ex("carla_b", "stiff", 4, reps(6, 8), kg(50), 120),
                    ex("carla_b", "db_shoulder_press", 4, reps(6, 8), kg(12, perSide = true), 120),
                    ex("carla_b", "lat_pulldown", 4, reps(6, 8), kg(45), 120),
                    ex("carla_b", "hip_thrust", 4, reps(8, 10), kg(70), 90),
                    ex("carla_b", "preacher_curl", 3, reps(8, 10), kg(20), 60),
                    ex("carla_b", "crunch", 3, reps(15, 15), Load.BodyWeight, 45),
                ),
            ),
        ),
    )

    private fun diego() = Student(
        id = "diego",
        name = UiText.Resource(R.string.mock_student_diego),
        goal = Goal.HEALTH,
        age = 55, weightKg = 90.0, heightCm = 175,
        notes = UiText.Resource(R.string.mock_notes_diego),
        workouts = listOf(
            Workout(
                id = "diego_a",
                name = UiText.Resource(R.string.mock_workout_health),
                days = setOf(TUESDAY, THURSDAY, SATURDAY),
                exercises = listOf(
                    ex("diego_a", "leg_press", 3, reps(12, 15), kg(80), 75),
                    ex("diego_a", "seated_row", 3, reps(12, 15), kg(35), 75),
                    ex("diego_a", "pec_deck", 3, reps(12, 15), kg(25), 75),
                    ex("diego_a", "leg_extension", 3, reps(12, 15), kg(30), 60),
                    ex("diego_a", "hammer_curl", 3, reps(12, 15), kg(8, perSide = true), 60),
                    ex("diego_a", "plank", 3, Reps.Duration(20), Load.BodyWeight, 45),
                ),
                cardio = listOf(
                    Cardio("diego_a_c1", CardioType.TREADMILL, 20, Intensity.LIGHT, UiText.Resource(R.string.mock_cardio_note_walk)),
                ),
            ),
        ),
    )

    // ------------------------------------------------------------------ Histórico

    private fun generateHistory(
        student: Student,
        today: LocalDate,
        random: Random,
        exerciseLogs: MutableMap<ExerciseLogKey, ExerciseLog>,
        cardioLogs: MutableSet<CardioLogKey>,
    ) {
        for (daysAgo in HISTORY_DAYS downTo 0L) {
            val date = today.minusDays(daysAgo)
            val workout = student.workoutFor(date.dayOfWeek) ?: continue
            val isToday = daysAgo == 0L

            // Hoje o Leonardo ainda não treinou; os outros alunos estão no meio do treino.
            val exercisesDone = when {
                isToday && student.id == LEONARDO_ID -> 0
                isToday -> random.nextInt(0, workout.exercises.size + 1)
                random.nextInt(100) < MISS_CHANCE -> 0
                else -> workout.exercises.size
            }
            if (exercisesDone == 0) continue

            val weeksAgo = (daysAgo / 7).toInt()
            workout.exercises.take(exercisesDone).forEach { exercise ->
                val key = ExerciseLogKey(student.id, date, exercise.id)
                exerciseLogs[key] = ExerciseLog(
                    key = key,
                    workoutId = workout.id,
                    catalogId = exercise.catalogId,
                    sets = simulateSets(exercise, weeksAgo, random),
                )
            }
            if (!isToday) {
                workout.cardio.forEach { cardioLogs += CardioLogKey(student.id, date, it.id) }
            }
        }
    }

    /** Carga um pouco menor nas semanas antigas, com pequenas oscilações (como na vida real). */
    private fun simulateSets(exercise: PrescribedExercise, weeksAgo: Int, random: Random): Map<Int, SetLog> {
        val load = when (val prescribed = exercise.load) {
            is Load.Kilograms -> {
                val progression = 1.0 - LOAD_DROP_PER_WEEK * weeksAgo
                val dip = if (random.nextInt(8) == 0) -1.0 else 0.0
                (prescribed.value * progression + dip).roundToInt().toDouble().coerceAtLeast(1.0)
            }
            Load.BodyWeight -> 0.0
        }
        return (0 until exercise.sets).associateWith { setIndex ->
            val reps = when (val prescribed = exercise.reps) {
                is Reps.Range -> {
                    // Com o peso do corpo, a evolução aparece nas repetições.
                    val bodyWeightBonus = if (exercise.load is Load.BodyWeight) -weeksAgo / 2 else 0
                    (prescribed.max - setIndex / 2 + random.nextInt(-2, 2) + bodyWeightBonus)
                        .coerceIn(prescribed.min - 2, prescribed.max + 2)
                        .coerceAtLeast(1)
                }
                is Reps.Duration -> (prescribed.seconds - weeksAgo * 3 + random.nextInt(-5, 6)).coerceAtLeast(10)
            }
            SetLog(reps = reps, loadKg = load)
        }
    }

    private const val SEED = 2026
    private const val LOAD_DROP_PER_WEEK = 0.03

    // ------------------------------------------------------------------ Atalhos

    private fun ex(
        workoutId: String,
        catalogId: String,
        sets: Int,
        reps: Reps,
        load: Load,
        restSeconds: Int,
        noteRes: Int? = null,
    ): PrescribedExercise {
        ExerciseCatalog.require(catalogId) // garante que o exercício existe no catálogo
        return PrescribedExercise(
            id = "${workoutId}_$catalogId",
            catalogId = catalogId,
            sets = sets,
            reps = reps,
            load = load,
            restSeconds = restSeconds,
            coachNote = noteRes?.let { UiText.Resource(it) },
        )
    }

    private fun reps(min: Int, max: Int) = Reps.Range(min, max)

    private fun kg(value: Int, perSide: Boolean = false) = Load.Kilograms(value.toDouble(), perSide)
}

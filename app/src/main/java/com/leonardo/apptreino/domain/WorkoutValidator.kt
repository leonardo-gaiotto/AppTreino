package com.leonardo.apptreino.domain

import androidx.annotation.StringRes
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.Cardio
import com.leonardo.apptreino.data.model.CardioType
import com.leonardo.apptreino.data.model.Intensity
import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.data.model.UiText
import com.leonardo.apptreino.data.model.Workout
import java.time.DayOfWeek

/** Problemas que impedem salvar um treino. */
enum class WorkoutError(@StringRes val messageRes: Int) {
    NAME_REQUIRED(R.string.error_workout_name),
    DAYS_REQUIRED(R.string.error_workout_days),
    DAY_CONFLICT(R.string.error_workout_day_conflict),
    EMPTY(R.string.error_workout_empty),
}

/** Regras para salvar um treino. */
object WorkoutValidator {

    fun validate(
        name: String,
        days: Set<DayOfWeek>,
        exerciseCount: Int,
        cardioCount: Int,
        workoutId: String,
        otherWorkouts: List<Workout>,
    ): Set<WorkoutError> = buildSet {
        if (name.isBlank()) add(WorkoutError.NAME_REQUIRED)
        if (days.isEmpty()) add(WorkoutError.DAYS_REQUIRED)
        if (conflictingDays(days, workoutId, otherWorkouts).isNotEmpty()) add(WorkoutError.DAY_CONFLICT)
        if (exerciseCount + cardioCount == 0) add(WorkoutError.EMPTY)
    }

    /** Dias já ocupados por OUTRO treino do aluno (um treino por dia, no máximo). */
    fun conflictingDays(days: Set<DayOfWeek>, workoutId: String, workouts: List<Workout>): Set<DayOfWeek> =
        workouts.filter { it.id != workoutId }.flatMap { it.days }.toSet() intersect days
}

// ---------------------------------------------------------------------- Prescrição de exercício

/** Campos do formulário de prescrição (texto digitado). */
data class PrescriptionInput(
    val sets: String,
    val repsMin: String,
    val repsMax: String,
    val seconds: String,
    val load: String,
    val isBodyWeight: Boolean,
    val isPerSide: Boolean,
    val rest: String,
    val note: String,
    val isIsometric: Boolean,
)

enum class PrescriptionField { SETS, REPS_MIN, REPS_MAX, SECONDS, LOAD, REST }

object PrescriptionValidator {

    val SETS_RANGE = 1..10
    val REPS_RANGE = 1..100
    val SECONDS_RANGE = 5..600
    val LOAD_RANGE = 0.5..500.0
    val REST_RANGE = 0..600

    fun validate(input: PrescriptionInput): Map<PrescriptionField, Int> = buildMap {
        rangeError(input.sets, SETS_RANGE, R.string.error_sets_range)?.let { put(PrescriptionField.SETS, it) }
        if (input.isIsometric) {
            rangeError(input.seconds, SECONDS_RANGE, R.string.error_seconds_range)?.let { put(PrescriptionField.SECONDS, it) }
        } else {
            val minError = rangeError(input.repsMin, REPS_RANGE, R.string.error_reps_range)
            val maxError = rangeError(input.repsMax, REPS_RANGE, R.string.error_reps_range)
            minError?.let { put(PrescriptionField.REPS_MIN, it) }
            maxError?.let { put(PrescriptionField.REPS_MAX, it) }
            if (minError == null && maxError == null && input.repsMax.trim().toInt() < input.repsMin.trim().toInt()) {
                put(PrescriptionField.REPS_MAX, R.string.error_reps_max_lower)
            }
        }
        if (!input.isBodyWeight) {
            decimalRangeError(input.load, LOAD_RANGE, R.string.error_load_range)?.let { put(PrescriptionField.LOAD, it) }
        }
        rangeError(input.rest, REST_RANGE, R.string.error_rest_range)?.let { put(PrescriptionField.REST, it) }
    }

    /** Converte a entrada validada em um [PrescribedExercise]. */
    fun toPrescription(input: PrescriptionInput, id: String, catalogId: String): PrescribedExercise {
        require(validate(input).isEmpty()) { "Entrada inválida" }
        return PrescribedExercise(
            id = id,
            catalogId = catalogId,
            sets = input.sets.trim().toInt(),
            reps = if (input.isIsometric) {
                Reps.Duration(input.seconds.trim().toInt())
            } else {
                Reps.Range(input.repsMin.trim().toInt(), input.repsMax.trim().toInt())
            },
            load = if (input.isBodyWeight) Load.BodyWeight else Load.Kilograms(parseDecimal(input.load)!!, input.isPerSide),
            restSeconds = input.rest.trim().toInt(),
            coachNote = input.note.trim().takeIf { it.isNotEmpty() }?.let { UiText.Dynamic(it) },
        )
    }
}

// ---------------------------------------------------------------------- Cardio

data class CardioInput(
    val type: CardioType?,
    val duration: String,
    val intensity: Intensity?,
    val note: String,
)

enum class CardioField { TYPE, DURATION, INTENSITY }

object CardioValidator {

    val DURATION_RANGE = 1..180

    fun validate(input: CardioInput): Map<CardioField, Int> = buildMap {
        if (input.type == null) put(CardioField.TYPE, R.string.error_cardio_type)
        rangeError(input.duration, DURATION_RANGE, R.string.error_duration_range)?.let { put(CardioField.DURATION, it) }
        if (input.intensity == null) put(CardioField.INTENSITY, R.string.error_intensity)
    }

    fun toCardio(input: CardioInput, id: String): Cardio {
        require(validate(input).isEmpty()) { "Entrada inválida" }
        return Cardio(
            id = id,
            type = requireNotNull(input.type),
            durationMinutes = input.duration.trim().toInt(),
            intensity = requireNotNull(input.intensity),
            note = input.note.trim().takeIf { it.isNotEmpty() }?.let { UiText.Dynamic(it) },
        )
    }
}

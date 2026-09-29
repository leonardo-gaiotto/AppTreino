package com.leonardo.apptreino.ui.common

import android.content.res.Resources
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.Cardio
import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.data.model.SetLog
import com.leonardo.apptreino.data.model.Workout
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Converte os dados dos modelos em textos para a interface.
 * Todos os textos vêm de `strings.xml`; aqui só montamos as combinações.
 */
object Formatters {

    /** Número com até 1 casa decimal no formato do aparelho. Ex.: "42,5" ou "40". */
    fun decimal(res: Resources, value: Double): String =
        NumberFormat.getNumberInstance(res.configuration.locales[0])
            .apply { maximumFractionDigits = 1 }
            .format(value)

    fun integer(res: Resources, value: Int): String = res.getString(R.string.format_integer, value)

    /** Ex.: "8–10", "15" ou "45 s". */
    fun reps(res: Resources, reps: Reps): String = when (reps) {
        is Reps.Range ->
            if (reps.min == reps.max) integer(res, reps.min)
            else res.getString(R.string.reps_range, reps.min, reps.max)
        is Reps.Duration -> duration(res, reps.seconds)
    }

    /** Ex.: "40 kg", "12 kg cada" ou "Peso corporal". */
    fun load(res: Resources, load: Load): String = when (load) {
        is Load.Kilograms ->
            if (load.perSide) res.getString(R.string.load_kg_per_side, decimal(res, load.value))
            else kilograms(res, load.value)
        Load.BodyWeight -> res.getString(R.string.load_bodyweight)
    }

    /** Ex.: "42,5 kg". */
    fun kilograms(res: Resources, value: Double): String = res.getString(R.string.load_kg, decimal(res, value))

    /** Ex.: "45 s", "2 min" ou "1 min 30 s". */
    fun duration(res: Resources, totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return when {
            minutes == 0 -> res.getString(R.string.duration_seconds, seconds)
            seconds == 0 -> res.getString(R.string.duration_minutes, minutes)
            else -> res.getString(R.string.duration_minutes_seconds, minutes, seconds)
        }
    }

    /** Linha de prescrição. Ex.: "4 × 8–10 · 40 kg". */
    fun prescription(res: Resources, exercise: PrescribedExercise): String = res.getString(
        R.string.prescription_summary,
        res.getString(R.string.prescription_sets_reps, exercise.sets, reps(res, exercise.reps)),
        load(res, exercise.load),
    )

    /** O que foi pedido para UMA série. Ex.: "8–10 × 40 kg" ou "45 s". */
    fun setTarget(res: Resources, exercise: PrescribedExercise): String = when (exercise.load) {
        Load.BodyWeight -> reps(res, exercise.reps)
        is Load.Kilograms -> res.getString(R.string.set_value_with_load, reps(res, exercise.reps), load(res, exercise.load))
    }

    /** O que foi feito em UMA série. Ex.: "10 × 42,5 kg", "12" ou "45 s". */
    fun setDone(res: Resources, setLog: SetLog, isIsometric: Boolean): String {
        val amount = if (isIsometric) duration(res, setLog.reps) else integer(res, setLog.reps)
        return if (setLog.loadKg > 0) {
            res.getString(R.string.set_value_with_load, amount, kilograms(res, setLog.loadKg))
        } else {
            amount
        }
    }

    /** Ex.: "7 exercícios · 1 cardio · ~60 min". */
    fun workoutMeta(res: Resources, workout: Workout): String {
        val parts = buildList {
            add(res.getQuantityString(R.plurals.exercise_count, workout.exercises.size, workout.exercises.size))
            if (workout.cardio.isNotEmpty()) {
                add(res.getQuantityString(R.plurals.cardio_count, workout.cardio.size, workout.cardio.size))
            }
            add(res.getString(R.string.minutes_approx, workout.estimatedMinutes))
        }
        return parts.joinToString(res.getString(R.string.separator_dot))
    }

    /** Ex.: "Treino A". */
    fun workoutTitle(res: Resources, letter: Char): String = res.getString(R.string.workout_title, letter.toString())

    /** Ex.: "Seg · Qui". */
    fun days(res: Resources, days: Set<DayOfWeek>): String {
        val names = res.getStringArray(R.array.week_days_abbr)
        return days.sorted().joinToString(res.getString(R.string.separator_dot)) { names[it.value - 1] }
    }

    /** Ex.: "15 min · Moderada". */
    fun cardioSummary(res: Resources, cardio: Cardio): String = res.getString(
        R.string.cardio_summary,
        res.getString(R.string.duration_minutes, cardio.durationMinutes),
        res.getString(cardio.intensity.labelRes),
    )

    /** Ex.: "segunda-feira, 28 de setembro". */
    fun fullDate(res: Resources, date: LocalDate): String = res.getString(
        R.string.date_full,
        dayNameFull(res, date.dayOfWeek),
        date.dayOfMonth,
        res.getStringArray(R.array.months)[date.monthValue - 1],
    )

    /** Ex.: "28/09". */
    fun shortDate(res: Resources, date: LocalDate): String =
        res.getString(R.string.date_short, date.dayOfMonth, date.monthValue)

    /** Ex.: "quarta-feira". */
    fun dayNameFull(res: Resources, day: DayOfWeek): String =
        res.getStringArray(R.array.week_days_full)[day.value - 1]

    /** Ex.: "QUA". */
    fun dayNameShort(res: Resources, day: DayOfWeek): String =
        res.getStringArray(R.array.week_days_short)[day.value - 1]

    /** Iniciais para o avatar. Ex.: "Ana Souza" → "AS". */
    fun initials(name: String): String =
        name.trim().split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .let { parts -> if (parts.size >= 2) listOf(parts.first(), parts.last()) else parts }
            .joinToString("") { it.first().uppercase() }
}

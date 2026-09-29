package com.leonardo.apptreino.domain

import com.leonardo.apptreino.data.ExerciseCatalog
import com.leonardo.apptreino.data.GymState
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.data.model.SetLog
import java.time.LocalDate

/** O que medir na evolução de um exercício. */
enum class EvolutionMetric {
    /** Maior carga da sessão (exercícios com peso). */
    LOAD,

    /** Máximo de repetições (exercícios com o peso do corpo). */
    REPS,

    /** Maior tempo (exercícios isométricos). */
    SECONDS,
}

enum class Trend { UP, DOWN, STABLE }

/** Uma sessão (um dia) em que o aluno fez o exercício. */
data class EvolutionSession(
    val date: LocalDate,
    val sets: List<SetLog>,
    /** Valor principal da sessão, conforme a [EvolutionMetric]. */
    val value: Double,
)

/** Histórico completo de um exercício para um aluno. */
data class ExerciseEvolution(
    val exercise: CatalogExercise,
    val metric: EvolutionMetric,
    /** Sessões em ordem cronológica (a mais antiga primeiro). */
    val sessions: List<EvolutionSession>,
) {
    val latest: EvolutionSession get() = sessions.last()

    /** Variação desde a primeira sessão registrada. */
    val totalChange: Double get() = latest.value - sessions.first().value

    /** Variação em relação à sessão anterior. */
    val lastChange: Double get() = if (sessions.size < 2) 0.0 else latest.value - sessions[sessions.size - 2].value

    val trend: Trend get() = trendOf(totalChange)

    /** Compara cada sessão com a anterior (a primeira fica STABLE). */
    fun trendOfSession(index: Int): Trend =
        if (index == 0) Trend.STABLE else trendOf(sessions[index].value - sessions[index - 1].value)

    private fun trendOf(change: Double): Trend = when {
        change > 0 -> Trend.UP
        change < 0 -> Trend.DOWN
        else -> Trend.STABLE
    }
}

/**
 * Monta a evolução de cada exercício a partir dos registros do aluno.
 * Agrupa pelo exercício do CATÁLOGO, então o histórico continua mesmo se o coach
 * mudar o treino ou a prescrição.
 */
object EvolutionCalculator {

    fun metricOf(exercise: CatalogExercise): EvolutionMetric = when {
        exercise.isIsometric -> EvolutionMetric.SECONDS
        exercise.isBodyWeight -> EvolutionMetric.REPS
        else -> EvolutionMetric.LOAD
    }

    /** Evolução de todos os exercícios que o aluno já registrou, na ordem do catálogo. */
    fun evolutionsFor(state: GymState, studentId: String): List<ExerciseEvolution> {
        val catalogIds = state.exerciseLogs.values
            .filter { it.key.studentId == studentId }
            .map { it.catalogId }
            .toSet()
        return ExerciseCatalog.all
            .filter { it.id in catalogIds }
            .mapNotNull { evolutionOf(state, studentId, it.id) }
    }

    fun evolutionOf(state: GymState, studentId: String, catalogId: String): ExerciseEvolution? {
        val exercise = ExerciseCatalog.find(catalogId) ?: return null
        val metric = metricOf(exercise)
        val sessions = state.exerciseLogs.values
            .filter { it.key.studentId == studentId && it.catalogId == catalogId && it.sets.isNotEmpty() }
            .sortedBy { it.key.date }
            .map { log ->
                val sets = log.sets.toSortedMap().values.toList()
                EvolutionSession(date = log.key.date, sets = sets, value = valueOf(sets, metric))
            }
        return if (sessions.isEmpty()) null else ExerciseEvolution(exercise, metric, sessions)
    }

    private fun valueOf(sets: List<SetLog>, metric: EvolutionMetric): Double = when (metric) {
        EvolutionMetric.LOAD -> sets.maxOf { it.loadKg }
        EvolutionMetric.REPS, EvolutionMetric.SECONDS -> sets.maxOf { it.reps }.toDouble()
    }
}

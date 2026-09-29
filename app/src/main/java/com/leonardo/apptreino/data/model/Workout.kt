package com.leonardo.apptreino.data.model

import java.time.DayOfWeek

/** Quantidade de repetições de cada série. */
sealed interface Reps {
    /** Faixa de repetições, ex.: 8–10. Quando [min] == [max] é um número fixo. */
    data class Range(val min: Int, val max: Int) : Reps {
        init {
            require(min in 1..max) { "Faixa de repetições inválida: $min–$max" }
        }
    }

    /** Exercícios isométricos (ex.: prancha), medidos em segundos. */
    data class Duration(val seconds: Int) : Reps {
        init {
            require(seconds > 0) { "Duração inválida: $seconds" }
        }
    }
}

/** Carga sugerida pelo coach. */
sealed interface Load {
    /** Carga em quilos. [perSide] indica "em cada halter/lado". */
    data class Kilograms(val value: Double, val perSide: Boolean = false) : Load {
        init {
            require(value >= 0) { "Carga negativa: $value" }
        }
    }

    /** Apenas o peso do próprio corpo. */
    data object BodyWeight : Load

    /** Carga em kg usada como referência no registro (0 para peso corporal). */
    val kilograms: Double
        get() = when (this) {
            is Kilograms -> value
            BodyWeight -> 0.0
        }
}

/** Um exercício do catálogo com a prescrição que o coach definiu para este treino. */
data class PrescribedExercise(
    val id: String,
    val catalogId: String,
    val sets: Int,
    val reps: Reps,
    val load: Load,
    val restSeconds: Int,
    /** Observação opcional do coach. */
    val coachNote: UiText? = null,
) {
    /** Repetições (ou segundos) pré-preenchidas no registro do aluno: o topo da faixa pedida. */
    val targetReps: Int
        get() = when (reps) {
            is Reps.Range -> reps.max
            is Reps.Duration -> reps.seconds
        }

    /** Tempo aproximado (execução + descanso entre as séries), em segundos. */
    val estimatedSeconds: Int
        get() {
            val workPerSet = when (reps) {
                is Reps.Range -> SECONDS_PER_SET
                is Reps.Duration -> reps.seconds
            }
            return sets * (workPerSet + restSeconds)
        }

    private companion object {
        /** Média de tempo sob tensão de uma série de 8 a 15 repetições. */
        const val SECONDS_PER_SET = 40
    }
}

/** Uma atividade de cardio dentro do treino. */
data class Cardio(
    val id: String,
    val type: CardioType,
    val durationMinutes: Int,
    val intensity: Intensity,
    val note: UiText? = null,
)

/**
 * Um treino do aluno: nome, dias da semana em que acontece, exercícios e cardio.
 * Cada dia da semana pertence a no máximo um treino (ver WorkoutValidator).
 */
data class Workout(
    val id: String,
    val name: UiText,
    val days: Set<DayOfWeek>,
    val exercises: List<PrescribedExercise>,
    val cardio: List<Cardio> = emptyList(),
) {
    /** Total de itens do dia: exercícios + atividades de cardio. */
    val itemCount: Int get() = exercises.size + cardio.size

    val totalSets: Int get() = exercises.sumOf { it.sets }

    /** Duração estimada (musculação + cardio), arredondada para múltiplos de 5 minutos. */
    val estimatedMinutes: Int
        get() {
            val minutes = exercises.sumOf { it.estimatedSeconds } / 60.0 + cardio.sumOf { it.durationMinutes }
            return (Math.round(minutes / 5.0) * 5).toInt().coerceAtLeast(5)
        }
}

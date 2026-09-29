package com.leonardo.apptreino.domain

import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.Reps

/** Resultado da comparação entre o que o aluno fez e o que o coach pediu. */
enum class Comparison { ABOVE, ON_TARGET, BELOW }

/**
 * Compara uma série registrada com a prescrição.
 * Nas repetições vale a FAIXA pedida (8–10): 9 está no alvo, 7 abaixo e 11 acima.
 */
object SetComparator {

    fun reps(done: Int, target: Reps): Comparison = when (target) {
        is Reps.Range -> when {
            done < target.min -> Comparison.BELOW
            done > target.max -> Comparison.ABOVE
            else -> Comparison.ON_TARGET
        }
        is Reps.Duration -> compare(done.toDouble(), target.seconds.toDouble())
    }

    /** Diferença até o limite mais próximo da faixa (0 quando está no alvo). */
    fun repsDelta(done: Int, target: Reps): Int = when (target) {
        is Reps.Range -> when {
            done < target.min -> done - target.min
            done > target.max -> done - target.max
            else -> 0
        }
        is Reps.Duration -> done - target.seconds
    }

    fun load(doneKg: Double, target: Load): Comparison = compare(doneKg, target.kilograms)

    fun loadDelta(doneKg: Double, target: Load): Double = doneKg - target.kilograms

    private fun compare(done: Double, target: Double): Comparison = when {
        done > target + EPSILON -> Comparison.ABOVE
        done < target - EPSILON -> Comparison.BELOW
        else -> Comparison.ON_TARGET
    }

    private const val EPSILON = 0.01
}

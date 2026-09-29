package com.leonardo.apptreino.domain

import com.leonardo.apptreino.TestData
import com.leonardo.apptreino.TestData.MONDAY
import com.leonardo.apptreino.TestData.STUDENT_ID
import com.leonardo.apptreino.data.ExerciseCatalog
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.data.model.SetLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EvolutionCalculatorTest {

    private val repository = GymRepository(initialState = TestData::emptyState)
    private val workout = TestData.workoutA
    private val bench = workout.exercises[0]

    /** Registra uma sessão do supino com a carga informada (3 séries). */
    private fun session(daysAfterMonday: Long, load: Double, reps: Int = 10) {
        repository.completeExercise(STUDENT_ID, MONDAY.plusDays(daysAfterMonday), workout.id, bench) {
            SetLog(reps, load)
        }
    }

    @Test
    fun `metrica depende do tipo de exercicio`() {
        assertEquals(EvolutionMetric.LOAD, EvolutionCalculator.metricOf(ExerciseCatalog.require("bench_press")))
        assertEquals(EvolutionMetric.REPS, EvolutionCalculator.metricOf(ExerciseCatalog.require("pull_up")))
        assertEquals(EvolutionMetric.SECONDS, EvolutionCalculator.metricOf(ExerciseCatalog.require("plank")))
    }

    @Test
    fun `sessoes ficam em ordem cronologica com a maior carga de cada uma`() {
        session(14, 44.0)
        session(0, 40.0)
        session(7, 42.0)

        val evolution = EvolutionCalculator.evolutionOf(repository.state.value, STUDENT_ID, "bench_press")!!
        assertEquals(listOf(40.0, 42.0, 44.0), evolution.sessions.map { it.value })
        assertEquals(4.0, evolution.totalChange, 0.001)
        assertEquals(2.0, evolution.lastChange, 0.001)
        assertEquals(Trend.UP, evolution.trend)
    }

    @Test
    fun `queda de carga aparece como tendencia de baixa`() {
        session(0, 42.0)
        session(7, 40.0)

        val evolution = EvolutionCalculator.evolutionOf(repository.state.value, STUDENT_ID, "bench_press")!!
        assertEquals(Trend.DOWN, evolution.trend)
        assertEquals(Trend.STABLE, evolution.trendOfSession(0))
        assertEquals(Trend.DOWN, evolution.trendOfSession(1))
    }

    @Test
    fun `lista so os exercicios que o aluno ja registrou`() {
        session(0, 40.0)
        val evolutions = EvolutionCalculator.evolutionsFor(repository.state.value, STUDENT_ID)
        assertEquals(listOf("bench_press"), evolutions.map { it.exercise.id })
    }

    @Test
    fun `sem registros nao ha evolucao`() {
        assertNull(EvolutionCalculator.evolutionOf(repository.state.value, STUDENT_ID, "bench_press"))
    }
}

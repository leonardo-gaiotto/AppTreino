package com.leonardo.apptreino.data

import com.leonardo.apptreino.TestData
import com.leonardo.apptreino.domain.EvolutionCalculator
import com.leonardo.apptreino.domain.WorkoutValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.temporal.ChronoUnit

/** Garante que os dados de demonstração são consistentes e "ricos" para a apresentação. */
class MockDataTest {

    private val today = TestData.MONDAY
    private val state = MockData.create(today)

    @Test
    fun `sao 5 alunos e o Leonardo e um deles`() {
        assertEquals(5, state.students.size)
        assertNotNull(state.student(MockData.LEONARDO_ID))
    }

    @Test
    fun `cada treino mockado tem entre 6 e 8 exercicios do catalogo`() {
        state.students.flatMap { it.workouts }.forEach { workout ->
            assertTrue("Treino ${workout.id}", workout.exercises.size in 6..8)
            workout.exercises.forEach { assertNotNull(ExerciseCatalog.find(it.catalogId)) }
        }
    }

    @Test
    fun `nenhum aluno tem dois treinos no mesmo dia`() {
        state.students.forEach { student ->
            student.workouts.forEach { workout ->
                val conflicts = WorkoutValidator.conflictingDays(workout.days, workout.id, student.workouts)
                assertTrue("Conflito em ${workout.id}: $conflicts", conflicts.isEmpty())
            }
        }
    }

    @Test
    fun `todos os alunos tem pelo menos 4 semanas de historico`() {
        state.students.forEach { student ->
            val weeks = state.exerciseLogs.keys
                .filter { it.studentId == student.id }
                .map { ChronoUnit.WEEKS.between(it.date, today) }
                .toSet()
            assertTrue("${student.id} tem ${weeks.size} semanas", weeks.size >= 4)
        }
    }

    @Test
    fun `o Leonardo ainda nao treinou hoje`() {
        assertTrue(state.exerciseLogs.keys.none { it.studentId == MockData.LEONARDO_ID && it.date == today })
    }

    @Test
    fun `a carga do Leonardo sobe ao longo das semanas`() {
        val evolution = EvolutionCalculator.evolutionOf(state, MockData.LEONARDO_ID, "bench_press")
        assertNotNull(evolution)
        assertTrue(evolution!!.sessions.size >= 6)
        assertTrue(evolution.totalChange > 0)
    }

    @Test
    fun `o historico e deterministico`() {
        assertEquals(state, MockData.create(today))
    }
}

package com.leonardo.apptreino.domain

import com.leonardo.apptreino.TestData
import com.leonardo.apptreino.TestData.MONDAY
import com.leonardo.apptreino.TestData.STUDENT_ID
import com.leonardo.apptreino.data.GymRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

class ProgressCalculatorTest {

    private lateinit var repository: GymRepository
    private val student = TestData.student
    private val workoutA = TestData.workoutA
    private val workoutB = TestData.workoutB

    @Before
    fun setUp() {
        repository = GymRepository(initialState = TestData::emptyState)
    }

    private val state get() = repository.state.value

    /** Conclui tudo (exercícios + cardio) do treino do dia. */
    private fun completeDay(dayOffset: Long) {
        val date = MONDAY.plusDays(dayOffset)
        val workout = student.workoutFor(date.dayOfWeek)!!
        workout.exercises.forEach { exercise ->
            repository.completeExercise(STUDENT_ID, date, workout.id, exercise) { TestData.setLog() }
        }
        workout.cardio.forEach { repository.toggleCardio(STUDENT_ID, date, it.id) }
    }

    @Test
    fun `progresso do dia conta exercicios e cardio`() {
        repository.completeExercise(STUDENT_ID, MONDAY, workoutA.id, workoutA.exercises[0]) { TestData.setLog() }
        repository.toggleCardio(STUDENT_ID, MONDAY, workoutA.cardio[0].id)

        val progress = ProgressCalculator.dayProgress(state, student, MONDAY)!!
        assertEquals(3, progress.totalItems)
        assertEquals(2, progress.doneItems)
        assertEquals(66, progress.percent)
        assertTrue(progress.isStarted)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `dia sem treino nao tem progresso`() {
        assertNull(ProgressCalculator.dayProgress(state, student, MONDAY.plusDays(3))) // quinta
    }

    @Test
    fun `semana vai de segunda a domingo`() {
        val week = ProgressCalculator.weekOf(MONDAY.plusDays(4))
        assertEquals(7, week.size)
        assertEquals(DayOfWeek.MONDAY, week.first().dayOfWeek)
        assertEquals(MONDAY, week.first())
    }

    @Test
    fun `resumo da semana`() {
        completeDay(0) // segunda
        val summary = ProgressCalculator.weekSummary(state, student, MONDAY)
        assertEquals(3, summary.planned) // seg, ter, qua
        assertEquals(1, summary.completed)
        assertEquals(33, summary.percent)
    }

    @Test
    fun `sequencia conta treinos seguidos e ignora descanso e o treino de hoje em andamento`() {
        completeDay(0) // segunda
        completeDay(1) // terça
        completeDay(2) // quarta
        // quinta a domingo: descanso; segunda seguinte (hoje) ainda sem nada feito
        val nextMonday = MONDAY.plusDays(7)
        assertEquals(3, ProgressCalculator.streak(state, student, nextMonday))
    }

    @Test
    fun `treino perdido quebra a sequencia`() {
        completeDay(0) // segunda
        // terça sem treino feito
        completeDay(2) // quarta
        assertEquals(1, ProgressCalculator.streak(state, student, MONDAY.plusDays(3)))
    }

    @Test
    fun `totais do historico`() {
        completeDay(0)
        repository.completeExercise(STUDENT_ID, MONDAY.plusDays(1), workoutB.id, workoutB.exercises[0]) { TestData.setLog() }

        assertEquals(2, ProgressCalculator.completedWorkouts(state, student)) // terça tem 1 item só
        assertEquals(3, ProgressCalculator.completedExercises(state, student))
        assertEquals(2, ProgressCalculator.activeDaysThisWeek(state, student, MONDAY))
    }
}

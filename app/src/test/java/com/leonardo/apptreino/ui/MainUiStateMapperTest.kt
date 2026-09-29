package com.leonardo.apptreino.ui

import com.leonardo.apptreino.TestData
import com.leonardo.apptreino.TestData.MONDAY
import com.leonardo.apptreino.TestData.STUDENT_ID
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.ui.main.DayStatus
import com.leonardo.apptreino.ui.main.MainUiStateMapper
import com.leonardo.apptreino.ui.main.TodayListItem
import com.leonardo.apptreino.ui.main.TodayUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainUiStateMapperTest {

    private val repository = GymRepository(initialState = TestData::emptyState)
    private val student = TestData.student
    private val workoutA = TestData.workoutA
    private val state get() = repository.state.value

    // ---------------------------------------------------------------- Hoje

    @Test
    fun `dia de treino lista exercicios numerados e depois o cardio`() {
        val today = MainUiStateMapper.buildToday(state, student, MONDAY, MONDAY) as TodayUiState.Training

        assertEquals('A', today.letter)
        assertTrue(today.isToday)
        assertEquals(
            listOf("exercise_a1", "exercise_a2", "cardio_header", "cardio_a_c1"),
            today.items.map { it.key },
        )
        val numbers = today.items.filterIsInstance<TodayListItem.ExerciseRow>().map { it.item.number }
        assertEquals(listOf(1, 2), numbers)
    }

    @Test
    fun `treino sem cardio nao tem titulo de cardio`() {
        val tuesday = MainUiStateMapper.buildToday(state, student, MONDAY.plusDays(1), MONDAY) as TodayUiState.Training
        assertTrue(tuesday.items.none { it == TodayListItem.CardioHeader })
        assertFalse(tuesday.isToday)
    }

    @Test
    fun `dia sem treino gera estado de descanso`() {
        assertTrue(MainUiStateMapper.buildToday(state, student, MONDAY.plusDays(3), MONDAY) is TodayUiState.Rest)
    }

    @Test
    fun `series registradas aparecem no item e no progresso`() {
        val bench = workoutA.exercises[0]
        repository.logSet(STUDENT_ID, MONDAY, workoutA.id, bench, 0, TestData.setLog())

        val today = MainUiStateMapper.buildToday(state, student, MONDAY, MONDAY) as TodayUiState.Training
        val item = (today.items.first() as TodayListItem.ExerciseRow).item
        assertEquals(1, item.loggedSets)
        assertTrue(item.isPartial)
        assertFalse(item.isDone)
        assertEquals(0, today.progress.doneItems)
    }

    // ---------------------------------------------------------------- Semana

    @Test
    fun `status de cada dia da semana`() {
        // Hoje = quarta. Segunda foi concluída e terça ficou sem treino.
        workoutA.exercises.forEach { exercise ->
            repository.completeExercise(STUDENT_ID, MONDAY, workoutA.id, exercise) { TestData.setLog() }
        }
        repository.toggleCardio(STUDENT_ID, MONDAY, workoutA.cardio[0].id)
        val wednesday = MONDAY.plusDays(2)

        val week = MainUiStateMapper.buildWeek(state, student, wednesday, wednesday)

        assertEquals(
            listOf(
                DayStatus.DONE, // seg
                DayStatus.MISSED, // ter
                DayStatus.TODAY, // qua
                DayStatus.REST, // qui
                DayStatus.REST, // sex
                DayStatus.REST, // sáb
                DayStatus.REST, // dom
            ),
            week.days.map { it.status },
        )
        assertEquals(3, week.plannedWorkouts)
        assertEquals(1, week.completedWorkouts)
        assertEquals(listOf(wednesday), week.days.filter { it.isSelected }.map { it.date })
    }

    @Test
    fun `dia futuro e dia em andamento`() {
        // Só o primeiro exercício de segunda foi feito: o dia está em andamento.
        repository.completeExercise(STUDENT_ID, MONDAY, workoutA.id, workoutA.exercises[0]) { TestData.setLog() }

        val week = MainUiStateMapper.buildWeek(state, student, MONDAY, MONDAY)
        assertEquals(DayStatus.IN_PROGRESS, week.days[0].status)
        assertEquals(DayStatus.UPCOMING, week.days[1].status)
    }

    // ---------------------------------------------------------------- Perfil

    @Test
    fun `perfil do aluno e numeros do coach`() {
        val profile = MainUiStateMapper.buildProfile(state, student, MONDAY)
        assertEquals(listOf('A', 'B'), profile.workoutLetters)
        assertEquals(3, profile.trainingDaysPerWeek)
        assertEquals(1, profile.coachStudents)
        assertEquals(2, profile.coachWorkouts)
        assertEquals(0, profile.coachWeekAverage)
    }
}

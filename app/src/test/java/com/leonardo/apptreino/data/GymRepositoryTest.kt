package com.leonardo.apptreino.data

import com.leonardo.apptreino.TestData
import com.leonardo.apptreino.TestData.MONDAY
import com.leonardo.apptreino.TestData.STUDENT_ID
import com.leonardo.apptreino.data.model.Goal
import com.leonardo.apptreino.data.model.Student
import com.leonardo.apptreino.data.model.UiText
import com.leonardo.apptreino.domain.ProgressCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.DayOfWeek

class GymRepositoryTest {

    private lateinit var repository: GymRepository
    private val workout = TestData.workoutA
    private val bench = workout.exercises[0] // 3 séries

    @Before
    fun setUp() {
        repository = GymRepository(initialState = TestData::emptyState)
    }

    private val state get() = repository.state.value
    private fun isDone() = ProgressCalculator.isExerciseDone(state, STUDENT_ID, MONDAY, bench)

    // ------------------------------------------------------------------ Registro das séries

    @Test
    fun `registrar parte das series nao conclui o exercicio`() {
        repository.logSet(STUDENT_ID, MONDAY, workout.id, bench, 0, TestData.setLog())
        repository.logSet(STUDENT_ID, MONDAY, workout.id, bench, 1, TestData.setLog())

        assertEquals(2, state.exerciseLog(STUDENT_ID, MONDAY, bench.id)?.sets?.size)
        assertFalse(isDone())
    }

    @Test
    fun `registrar todas as series conclui o exercicio automaticamente`() {
        repeat(bench.sets) { index ->
            repository.logSet(STUDENT_ID, MONDAY, workout.id, bench, index, TestData.setLog())
        }
        assertTrue(isDone())
    }

    @Test
    fun `registrar de novo a mesma serie atualiza os valores`() {
        repository.logSet(STUDENT_ID, MONDAY, workout.id, bench, 0, TestData.setLog(reps = 8))
        repository.logSet(STUDENT_ID, MONDAY, workout.id, bench, 0, TestData.setLog(reps = 12, load = 42.5))

        val log = state.exerciseLog(STUDENT_ID, MONDAY, bench.id)!!
        assertEquals(1, log.sets.size)
        assertEquals(TestData.setLog(12, 42.5), log.sets[0])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `serie fora da prescricao e rejeitada`() {
        repository.logSet(STUDENT_ID, MONDAY, workout.id, bench, bench.sets, TestData.setLog())
    }

    @Test
    fun `desfazer a ultima serie remove o registro do dia`() {
        repository.logSet(STUDENT_ID, MONDAY, workout.id, bench, 0, TestData.setLog())
        repository.removeSetLog(STUDENT_ID, MONDAY, bench.id, 0)

        assertNull(state.exerciseLog(STUDENT_ID, MONDAY, bench.id))
    }

    @Test
    fun `concluir mantem as series ja registradas e completa as que faltam`() {
        repository.logSet(STUDENT_ID, MONDAY, workout.id, bench, 0, TestData.setLog(reps = 12))
        repository.completeExercise(STUDENT_ID, MONDAY, workout.id, bench) { TestData.setLog(reps = 9) }

        val sets = state.exerciseLog(STUDENT_ID, MONDAY, bench.id)!!.sets
        assertEquals(listOf(12, 9, 9), (0 until bench.sets).map { sets.getValue(it).reps })
        assertTrue(isDone())
    }

    @Test
    fun `cardio alterna entre feito e pendente`() {
        val cardio = workout.cardio.first()
        repository.toggleCardio(STUDENT_ID, MONDAY, cardio.id)
        assertTrue(state.isCardioDone(STUDENT_ID, MONDAY, cardio.id))
        repository.toggleCardio(STUDENT_ID, MONDAY, cardio.id)
        assertFalse(state.isCardioDone(STUDENT_ID, MONDAY, cardio.id))
    }

    @Test
    fun `reiniciar o dia limpa so aquele dia`() {
        val wednesday = MONDAY.plusDays(2)
        repository.completeExercise(STUDENT_ID, MONDAY, workout.id, bench) { TestData.setLog() }
        repository.completeExercise(STUDENT_ID, wednesday, workout.id, bench) { TestData.setLog() }
        repository.toggleCardio(STUDENT_ID, MONDAY, workout.cardio.first().id)

        repository.clearDay(STUDENT_ID, MONDAY)

        assertNull(state.exerciseLog(STUDENT_ID, MONDAY, bench.id))
        assertTrue(state.cardioLogs.isEmpty())
        assertTrue(ProgressCalculator.isExerciseDone(state, STUDENT_ID, wednesday, bench))
    }

    // ------------------------------------------------------------------ Alunos e treinos (coach)

    @Test
    fun `cadastrar aluno adiciona na lista`() {
        val novo = Student("novo", UiText.Dynamic("Novo Aluno"), Goal.HEALTH, 30, 70.0, 170)
        repository.addStudent(novo)
        assertEquals(listOf(STUDENT_ID, "novo"), state.students.map { it.id })
    }

    @Test
    fun `salvar treino novo e editar treino existente`() {
        val novo = TestData.workoutB.copy(id = "c", days = setOf(DayOfWeek.FRIDAY))
        repository.saveWorkout(STUDENT_ID, novo)
        assertEquals(listOf("a", "b", "c"), state.student(STUDENT_ID)!!.workouts.map { it.id })

        val editado = workout.copy(exercises = workout.exercises.take(1))
        repository.saveWorkout(STUDENT_ID, editado)
        val workouts = state.student(STUDENT_ID)!!.workouts
        assertEquals(3, workouts.size)
        assertEquals(1, workouts.first { it.id == "a" }.exercises.size)
    }

    @Test
    fun `alteracao do coach aparece na hora para o aluno`() {
        // O coach move o treino B da terça para a sexta: terça vira descanso.
        repository.saveWorkout(STUDENT_ID, TestData.workoutB.copy(days = setOf(DayOfWeek.FRIDAY)))
        val student = state.student(STUDENT_ID)!!
        assertNull(student.workoutFor(DayOfWeek.TUESDAY))
        assertEquals("b", student.workoutFor(DayOfWeek.FRIDAY)?.id)
    }

    @Test
    fun `excluir treino`() {
        repository.deleteWorkout(STUDENT_ID, "b")
        assertEquals(listOf("a"), state.student(STUDENT_ID)!!.workouts.map { it.id })
    }

    @Test
    fun `restaurar demonstracao volta ao estado inicial`() {
        repository.deleteWorkout(STUDENT_ID, "a")
        repository.logSet(STUDENT_ID, MONDAY, workout.id, bench, 0, TestData.setLog())

        repository.resetToDemo()

        assertEquals(TestData.emptyState(), state)
    }

    @Test
    fun `ids gerados sao unicos`() {
        val ids = List(100) { GymRepository.newId() }
        assertEquals(100, ids.toSet().size)
    }
}

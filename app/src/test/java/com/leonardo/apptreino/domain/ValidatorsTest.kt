package com.leonardo.apptreino.domain

import com.leonardo.apptreino.R
import com.leonardo.apptreino.TestData
import com.leonardo.apptreino.data.model.CardioType
import com.leonardo.apptreino.data.model.Goal
import com.leonardo.apptreino.data.model.Intensity
import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.data.model.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ValidatorsTest {

    // ------------------------------------------------------------------ Cadastro de aluno

    private val validStudent = StudentInput(
        name = "Maria Silva", goal = Goal.WEIGHT_LOSS, age = "30", weight = "65,5", height = "168", notes = "",
    )

    @Test
    fun `aluno valido e convertido corretamente`() {
        assertTrue(StudentValidator.validate(validStudent).isEmpty())
        val student = StudentValidator.toStudent(validStudent, id = "id")
        assertEquals(UiText.Dynamic("Maria Silva"), student.name)
        assertEquals(65.5, student.weightKg, 0.001)
        assertNull(student.notes) // observação vazia vira null
    }

    @Test
    fun `nome vazio ou curto e rejeitado`() {
        assertEquals(R.string.error_required, StudentValidator.validate(validStudent.copy(name = "  "))[StudentField.NAME])
        assertEquals(R.string.error_name_short, StudentValidator.validate(validStudent.copy(name = "Al"))[StudentField.NAME])
    }

    @Test
    fun `objetivo e obrigatorio`() {
        assertTrue(StudentField.GOAL in StudentValidator.validate(validStudent.copy(goal = null)))
    }

    @Test
    fun `numeros fora da faixa ou negativos sao rejeitados`() {
        val errors = StudentValidator.validate(validStudent.copy(age = "7", weight = "-60", height = "abc"))
        assertEquals(R.string.error_age_range, errors[StudentField.AGE])
        assertEquals(R.string.error_weight_range, errors[StudentField.WEIGHT])
        assertEquals(R.string.error_invalid_number, errors[StudentField.HEIGHT])
    }

    // ------------------------------------------------------------------ Treino

    private val otherWorkouts = listOf(TestData.workoutA, TestData.workoutB)

    @Test
    fun `treino valido`() {
        val errors = WorkoutValidator.validate("Pernas", setOf(DayOfWeek.FRIDAY), 5, 0, "novo", otherWorkouts)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `treino sem nome sem dia e vazio e rejeitado`() {
        val errors = WorkoutValidator.validate(" ", emptySet(), 0, 0, "novo", otherWorkouts)
        assertEquals(setOf(WorkoutError.NAME_REQUIRED, WorkoutError.DAYS_REQUIRED, WorkoutError.EMPTY), errors)
    }

    @Test
    fun `um treino por dia no maximo`() {
        val errors = WorkoutValidator.validate("Extra", setOf(DayOfWeek.MONDAY), 1, 0, "novo", otherWorkouts)
        assertTrue(WorkoutError.DAY_CONFLICT in errors)
        assertEquals(
            setOf(DayOfWeek.MONDAY),
            WorkoutValidator.conflictingDays(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), "novo", otherWorkouts),
        )
    }

    @Test
    fun `editar um treino nao conflita com os proprios dias`() {
        val errors = WorkoutValidator.validate("A", TestData.workoutA.days, 2, 1, TestData.workoutA.id, otherWorkouts)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `cardio sozinho ja e um treino valido`() {
        assertTrue(WorkoutValidator.validate("Cardio", setOf(DayOfWeek.SUNDAY), 0, 1, "novo", otherWorkouts).isEmpty())
    }

    // ------------------------------------------------------------------ Prescrição

    private val validPrescription = PrescriptionInput(
        sets = "4", repsMin = "8", repsMax = "10", seconds = "", load = "42,5",
        isBodyWeight = false, isPerSide = true, rest = "90", note = "Controle a descida", isIsometric = false,
    )

    @Test
    fun `prescricao valida e convertida corretamente`() {
        assertTrue(PrescriptionValidator.validate(validPrescription).isEmpty())
        val exercise = PrescriptionValidator.toPrescription(validPrescription, "p1", "bench_press")
        assertEquals(Reps.Range(8, 10), exercise.reps)
        assertEquals(Load.Kilograms(42.5, perSide = true), exercise.load)
        assertEquals(UiText.Dynamic("Controle a descida"), exercise.coachNote)
    }

    @Test
    fun `valores negativos ou zerados sao rejeitados`() {
        val errors = PrescriptionValidator.validate(validPrescription.copy(sets = "0", load = "-10", rest = "-1"))
        assertEquals(R.string.error_sets_range, errors[PrescriptionField.SETS])
        assertEquals(R.string.error_load_range, errors[PrescriptionField.LOAD])
        assertEquals(R.string.error_rest_range, errors[PrescriptionField.REST])
    }

    @Test
    fun `repeticao maxima menor que a minima e rejeitada`() {
        val errors = PrescriptionValidator.validate(validPrescription.copy(repsMin = "12", repsMax = "8"))
        assertEquals(R.string.error_reps_max_lower, errors[PrescriptionField.REPS_MAX])
    }

    @Test
    fun `isometrico usa segundos e peso corporal ignora a carga`() {
        val plank = validPrescription.copy(isIsometric = true, seconds = "45", load = "", isBodyWeight = true)
        assertTrue(PrescriptionValidator.validate(plank).isEmpty())
        val exercise = PrescriptionValidator.toPrescription(plank, "p2", "plank")
        assertEquals(Reps.Duration(45), exercise.reps)
        assertEquals(Load.BodyWeight, exercise.load)
    }

    // ------------------------------------------------------------------ Cardio

    @Test
    fun `cardio valido e invalido`() {
        val valid = CardioInput(CardioType.BIKE, "20", Intensity.LIGHT, "")
        assertTrue(CardioValidator.validate(valid).isEmpty())
        assertEquals(20, CardioValidator.toCardio(valid, "c1").durationMinutes)

        val errors = CardioValidator.validate(CardioInput(null, "0", null, ""))
        assertEquals(setOf(CardioField.TYPE, CardioField.DURATION, CardioField.INTENSITY), errors.keys)
    }
}

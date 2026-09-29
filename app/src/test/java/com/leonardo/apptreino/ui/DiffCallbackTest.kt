package com.leonardo.apptreino.ui

import com.leonardo.apptreino.TestData
import com.leonardo.apptreino.data.ExerciseCatalog
import com.leonardo.apptreino.data.model.Goal
import com.leonardo.apptreino.domain.WeekSummary
import com.leonardo.apptreino.ui.exercise.SetRow
import com.leonardo.apptreino.ui.exercise.SetRowDiffCallback
import com.leonardo.apptreino.ui.main.CardioItem
import com.leonardo.apptreino.ui.main.DayStatus
import com.leonardo.apptreino.ui.main.ExerciseItem
import com.leonardo.apptreino.ui.main.TodayListItem
import com.leonardo.apptreino.ui.main.WeekDayItem
import com.leonardo.apptreino.ui.students.StudentDiffCallback
import com.leonardo.apptreino.ui.students.StudentListItem
import com.leonardo.apptreino.ui.today.TodayDiffCallback
import com.leonardo.apptreino.ui.week.WeekDayDiffCallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica as regras que o DiffUtil usa para decidir o que redesenhar nas listas. */
class DiffCallbackTest {

    private val bench = TestData.workoutA.exercises[0]
    private val row = TodayListItem.ExerciseRow(
        ExerciseItem(bench, ExerciseCatalog.require(bench.catalogId), number = 1, loggedSets = 0),
    )

    // ---------------------------------------------------------------- Hoje

    @Test
    fun `mesmo exercicio e o mesmo item mesmo com outro status`() {
        val done = TodayListItem.ExerciseRow(row.item.copy(loggedSets = 3))
        assertTrue(TodayDiffCallback.areItemsTheSame(row, done))
        assertFalse(TodayDiffCallback.areContentsTheSame(row, done))
    }

    @Test
    fun `so o status mudou gera payload de atualizacao parcial`() {
        val partial = TodayListItem.ExerciseRow(row.item.copy(loggedSets = 1))
        assertEquals(TodayDiffCallback.PAYLOAD_STATUS, TodayDiffCallback.getChangePayload(row, partial))
    }

    @Test
    fun `outras mudancas exigem redesenho completo`() {
        val renumbered = TodayListItem.ExerciseRow(row.item.copy(number = 5, loggedSets = 1))
        assertNull(TodayDiffCallback.getChangePayload(row, renumbered))
    }

    @Test
    fun `exercicio e cardio nunca sao o mesmo item`() {
        val cardio = TodayListItem.CardioRow(CardioItem(TestData.workoutA.cardio[0], isDone = false))
        assertFalse(TodayDiffCallback.areItemsTheSame(row, cardio))
        assertTrue(TodayDiffCallback.areItemsTheSame(TodayListItem.CardioHeader, TodayListItem.CardioHeader))
    }

    // ---------------------------------------------------------------- Semana, alunos e séries

    @Test
    fun `dias da semana sao comparados pela data`() {
        val day = WeekDayItem(TestData.MONDAY, null, null, null, isToday = true, isSelected = false, status = DayStatus.REST)
        assertTrue(WeekDayDiffCallback.areItemsTheSame(day, day.copy(isSelected = true)))
        assertFalse(WeekDayDiffCallback.areContentsTheSame(day, day.copy(isSelected = true)))
        assertFalse(WeekDayDiffCallback.areItemsTheSame(day, day.copy(date = TestData.MONDAY.plusDays(1))))
    }

    @Test
    fun `alunos sao comparados pelo id`() {
        val student = StudentListItem("ana", "Ana", Goal.HEALTH, 0, null, null, null, WeekSummary(3, 1))
        assertTrue(StudentDiffCallback.areItemsTheSame(student, student.copy(week = WeekSummary(3, 2))))
        assertFalse(StudentDiffCallback.areContentsTheSame(student, student.copy(week = WeekSummary(3, 2))))
    }

    @Test
    fun `series sao comparadas pela posicao`() {
        val set = SetRow(0, bench, isIsometric = false, reps = 10, loadKg = 40.0, isRegistered = false)
        assertTrue(SetRowDiffCallback.areItemsTheSame(set, set.copy(isRegistered = true)))
        assertFalse(SetRowDiffCallback.areContentsTheSame(set, set.copy(isRegistered = true)))
    }
}

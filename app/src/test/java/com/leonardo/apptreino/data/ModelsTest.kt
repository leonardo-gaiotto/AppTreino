package com.leonardo.apptreino.data

import com.leonardo.apptreino.TestData
import com.leonardo.apptreino.data.model.Cardio
import com.leonardo.apptreino.data.model.CardioType
import com.leonardo.apptreino.data.model.Intensity
import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.data.model.UiText
import com.leonardo.apptreino.data.model.Workout
import com.leonardo.apptreino.util.YouTube
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class ModelsTest {

    private fun exercise(sets: Int, reps: Reps, rest: Int) =
        PrescribedExercise("x", "bench_press", sets, reps, Load.BodyWeight, rest)

    @Test(expected = IllegalArgumentException::class)
    fun `faixa de repeticoes invertida e rejeitada`() {
        Reps.Range(12, 8)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `carga negativa e rejeitada`() {
        Load.Kilograms(-5.0)
    }

    @Test
    fun `tempo estimado considera execucao e descanso`() {
        // 4 séries × (40 s de execução + 60 s de descanso)
        assertEquals(400, exercise(4, Reps.Range(8, 10), 60).estimatedSeconds)
        // Isométrico: usa a duração da série no lugar dos 40 s padrão.
        assertEquals(3 * (45 + 30), exercise(3, Reps.Duration(45), 30).estimatedSeconds)
    }

    @Test
    fun `duracao do treino soma musculacao e cardio em multiplos de 5 minutos`() {
        // 7 × 400 s ≈ 46,7 min + 15 min de cardio = 61,7 → 60 min
        val workout = Workout(
            id = "t",
            name = UiText.Dynamic("Teste"),
            days = setOf(DayOfWeek.MONDAY),
            exercises = List(7) { exercise(4, Reps.Range(8, 10), 60) },
            cardio = listOf(Cardio("c", CardioType.BIKE, 15, Intensity.LIGHT)),
        )
        assertEquals(60, workout.estimatedMinutes)
        assertEquals(28, workout.totalSets)
        assertEquals(8, workout.itemCount)
    }

    @Test
    fun `valor pre-preenchido no registro e o topo da faixa`() {
        assertEquals(10, exercise(3, Reps.Range(8, 10), 60).targetReps)
        assertEquals(45, exercise(3, Reps.Duration(45), 60).targetReps)
        assertEquals(0.0, Load.BodyWeight.kilograms, 0.0)
    }

    @Test
    fun `aluno calcula IMC, treino do dia e letra do treino`() {
        val student = TestData.student
        assertEquals(24.7, student.bmi, 0.05)
        assertEquals("a", student.workoutFor(DayOfWeek.WEDNESDAY)?.id)
        assertNull(student.workoutFor(DayOfWeek.SUNDAY))
        assertEquals('B', student.letterOf("b"))
    }

    @Test
    fun `validacao de id do YouTube`() {
        assertTrue(YouTube.isValidVideoId("pCPyqW60Wuk"))
        assertTrue(YouTube.isValidVideoId("-oIl0YJGf9c"))
        assertFalse(YouTube.isValidVideoId("curto"))
        assertFalse(YouTube.isValidVideoId("https://youtu.be/pCPyqW60Wuk"))
    }

    @Test
    fun `urls do YouTube`() {
        assertEquals("https://www.youtube.com/watch?v=pCPyqW60Wuk", YouTube.watchUrl("pCPyqW60Wuk"))
        assertEquals(
            "https://img.youtube.com/vi/pCPyqW60Wuk/mqdefault.jpg",
            YouTube.thumbnailUrl("pCPyqW60Wuk", YouTube.Thumbnail.MEDIUM),
        )
        assertEquals(
            "https://img.youtube.com/vi/pCPyqW60Wuk/maxresdefault.jpg",
            YouTube.thumbnailUrl("pCPyqW60Wuk", YouTube.Thumbnail.HD),
        )
    }
}

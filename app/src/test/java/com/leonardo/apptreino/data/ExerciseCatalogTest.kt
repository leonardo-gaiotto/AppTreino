package com.leonardo.apptreino.data

import com.leonardo.apptreino.data.model.MuscleGroup
import com.leonardo.apptreino.util.YouTube
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseCatalogTest {

    private val all = ExerciseCatalog.all

    /** Nomes "de mentira" para testar a busca sem depender do Android. */
    private val names = mapOf(
        "hip_thrust" to "Elevação pélvica",
        "lateral_raise" to "Elevação lateral com halteres",
        "bench_press" to "Supino reto com barra",
    )

    private fun search(query: String, group: MuscleGroup? = null) =
        ExerciseCatalog.search(query, group) { names[it.id] ?: it.id }.map { it.id }

    @Test
    fun `catalogo tem pelo menos 25 exercicios com ids unicos`() {
        assertTrue(all.size >= 25)
        assertEquals(all.size, all.map { it.id }.toSet().size)
    }

    @Test
    fun `todos os videos tem id valido e nao se repetem`() {
        all.forEach { assertTrue("Vídeo inválido em ${it.id}", YouTube.isValidVideoId(it.youtubeVideoId)) }
        assertEquals(all.size, all.map { it.youtubeVideoId }.toSet().size)
    }

    @Test
    fun `todos os textos apontam para recursos`() {
        all.forEach { exercise ->
            assertNotEquals(0, exercise.nameRes)
            assertNotEquals(0, exercise.targetRes)
            assertNotEquals(0, exercise.stepsRes)
            assertNotEquals(0, exercise.commonMistakesRes)
        }
    }

    @Test
    fun `todo grupo muscular tem pelo menos um exercicio`() {
        MuscleGroup.entries.forEach { group ->
            assertTrue("Grupo sem exercícios: $group", all.any { it.group == group })
        }
    }

    @Test
    fun `busca ignora maiusculas e acentos`() {
        assertEquals(listOf("lateral_raise", "hip_thrust"), search("ELEVACAO"))
        assertEquals(listOf("hip_thrust"), search("pélvica"))
    }

    @Test
    fun `filtro por grupo e busca podem ser combinados`() {
        assertEquals(listOf("hip_thrust"), search("elevação", MuscleGroup.GLUTES))
        assertTrue(search("", MuscleGroup.CHEST).containsAll(listOf("bench_press", "pec_deck")))
        assertEquals(all.size, search("").size)
    }

    @Test
    fun `busca por id`() {
        assertEquals("plank", ExerciseCatalog.find("plank")?.id)
        assertNull(ExerciseCatalog.find("nao_existe"))
    }
}

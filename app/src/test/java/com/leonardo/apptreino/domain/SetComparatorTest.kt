package com.leonardo.apptreino.domain

import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.Reps
import org.junit.Assert.assertEquals
import org.junit.Test

/** Comparação "o que o coach pediu × o que o aluno fez". */
class SetComparatorTest {

    private val range = Reps.Range(8, 10)

    @Test
    fun `repeticoes dentro da faixa estao no alvo`() {
        assertEquals(Comparison.ON_TARGET, SetComparator.reps(8, range))
        assertEquals(Comparison.ON_TARGET, SetComparator.reps(10, range))
        assertEquals(0, SetComparator.repsDelta(9, range))
    }

    @Test
    fun `repeticoes acima e abaixo da faixa`() {
        assertEquals(Comparison.ABOVE, SetComparator.reps(12, range))
        assertEquals(2, SetComparator.repsDelta(12, range))
        assertEquals(Comparison.BELOW, SetComparator.reps(6, range))
        assertEquals(-2, SetComparator.repsDelta(6, range))
    }

    @Test
    fun `isometrico compara os segundos`() {
        val plank = Reps.Duration(45)
        assertEquals(Comparison.ABOVE, SetComparator.reps(50, plank))
        assertEquals(Comparison.ON_TARGET, SetComparator.reps(45, plank))
        assertEquals(-5, SetComparator.repsDelta(40, plank))
    }

    @Test
    fun `carga acima, igual e abaixo do previsto`() {
        val load = Load.Kilograms(40.0)
        assertEquals(Comparison.ABOVE, SetComparator.load(42.5, load))
        assertEquals(Comparison.ON_TARGET, SetComparator.load(40.0, load))
        assertEquals(Comparison.BELOW, SetComparator.load(38.0, load))
        assertEquals(-2.0, SetComparator.loadDelta(38.0, load), 0.001)
    }

    @Test
    fun `peso corporal com carga extra conta como acima`() {
        assertEquals(Comparison.ON_TARGET, SetComparator.load(0.0, Load.BodyWeight))
        assertEquals(Comparison.ABOVE, SetComparator.load(5.0, Load.BodyWeight))
    }
}

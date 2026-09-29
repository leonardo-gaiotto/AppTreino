package com.leonardo.apptreino.domain

import androidx.annotation.StringRes
import com.leonardo.apptreino.R
import com.leonardo.apptreino.data.model.Goal
import com.leonardo.apptreino.data.model.Student
import com.leonardo.apptreino.data.model.UiText

/** O que o coach digitou no cadastro de aluno (ainda como texto). */
data class StudentInput(
    val name: String,
    val goal: Goal?,
    val age: String,
    val weight: String,
    val height: String,
    val notes: String,
)

enum class StudentField { NAME, GOAL, AGE, WEIGHT, HEIGHT }

/** Validação do cadastro de aluno. Cada campo inválido recebe uma mensagem de `strings.xml`. */
object StudentValidator {

    const val MIN_NAME_LENGTH = 3
    val AGE_RANGE = 10..100
    val WEIGHT_RANGE = 30.0..300.0
    val HEIGHT_RANGE = 100..250

    /** @return mapa campo → mensagem de erro. Vazio quando está tudo certo. */
    fun validate(input: StudentInput): Map<StudentField, Int> = buildMap {
        val name = input.name.trim()
        when {
            name.isEmpty() -> put(StudentField.NAME, R.string.error_required)
            name.length < MIN_NAME_LENGTH -> put(StudentField.NAME, R.string.error_name_short)
        }
        if (input.goal == null) put(StudentField.GOAL, R.string.error_goal_required)
        rangeError(input.age, AGE_RANGE, R.string.error_age_range)?.let { put(StudentField.AGE, it) }
        decimalRangeError(input.weight, WEIGHT_RANGE, R.string.error_weight_range)?.let { put(StudentField.WEIGHT, it) }
        rangeError(input.height, HEIGHT_RANGE, R.string.error_height_range)?.let { put(StudentField.HEIGHT, it) }
    }

    /** Converte a entrada já validada em um [Student] novo, sem treinos. */
    fun toStudent(input: StudentInput, id: String): Student {
        require(validate(input).isEmpty()) { "Entrada inválida" }
        return Student(
            id = id,
            name = UiText.Dynamic(input.name.trim()),
            goal = requireNotNull(input.goal),
            age = input.age.trim().toInt(),
            weightKg = parseDecimal(input.weight)!!,
            heightCm = input.height.trim().toInt(),
            notes = input.notes.trim().takeIf { it.isNotEmpty() }?.let { UiText.Dynamic(it) },
        )
    }
}

// ---------------------------------------------------------------------- Auxiliares de validação

/** Aceita "72,5" e "72.5". */
internal fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

/** Erro de um campo inteiro obrigatório dentro de [range] (negativos também caem aqui). */
@StringRes
internal fun rangeError(text: String, range: IntRange, @StringRes rangeMessage: Int): Int? {
    if (text.isBlank()) return R.string.error_required
    val value = text.trim().toIntOrNull() ?: return R.string.error_invalid_number
    return if (value in range) null else rangeMessage
}

/** Mesma regra de [rangeError] para números decimais. */
@StringRes
internal fun decimalRangeError(text: String, range: ClosedFloatingPointRange<Double>, @StringRes rangeMessage: Int): Int? {
    if (text.isBlank()) return R.string.error_required
    val value = parseDecimal(text) ?: return R.string.error_invalid_number
    return if (value in range) null else rangeMessage
}

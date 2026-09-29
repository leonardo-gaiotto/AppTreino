package com.leonardo.apptreino.data.model

import java.time.LocalDate

/**
 * O que o aluno registrou em UMA série.
 * @param reps repetições feitas (ou segundos, em exercícios isométricos).
 * @param loadKg carga usada (0 para peso corporal).
 */
data class SetLog(val reps: Int, val loadKg: Double)

/** Identifica o registro de um exercício de um aluno em um dia. */
data class ExerciseLogKey(val studentId: String, val date: LocalDate, val prescribedId: String)

/**
 * Registro de um exercício em um dia: as séries feitas, indexadas pela posição
 * (0 = primeira série). Guarda o [catalogId] para montar a evolução do exercício
 * mesmo que o coach altere o treino depois.
 */
data class ExerciseLog(
    val key: ExerciseLogKey,
    val workoutId: String,
    val catalogId: String,
    val sets: Map<Int, SetLog>,
)

/** Identifica um cardio marcado como feito por um aluno em um dia. */
data class CardioLogKey(val studentId: String, val date: LocalDate, val cardioId: String)

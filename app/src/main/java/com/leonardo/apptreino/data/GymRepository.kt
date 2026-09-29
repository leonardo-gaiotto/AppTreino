package com.leonardo.apptreino.data

import com.leonardo.apptreino.data.model.CardioLogKey
import com.leonardo.apptreino.data.model.ExerciseLog
import com.leonardo.apptreino.data.model.ExerciseLogKey
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.SetLog
import com.leonardo.apptreino.data.model.Student
import com.leonardo.apptreino.data.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID

/** Foto imutável de todos os dados do app em um instante. */
data class GymState(
    val students: List<Student>,
    val exerciseLogs: Map<ExerciseLogKey, ExerciseLog>,
    val cardioLogs: Set<CardioLogKey>,
) {
    fun student(id: String): Student? = students.firstOrNull { it.id == id }

    fun exerciseLog(studentId: String, date: LocalDate, prescribedId: String): ExerciseLog? =
        exerciseLogs[ExerciseLogKey(studentId, date, prescribedId)]

    fun isCardioDone(studentId: String, date: LocalDate, cardioId: String): Boolean =
        CardioLogKey(studentId, date, cardioId) in cardioLogs
}

/**
 * Repositório ÚNICO e compartilhado por todas as telas (aluno e coach), 100% em memória.
 *
 * Guarda um [StateFlow] de [GymState]. Cada alteração cria um NOVO estado (os modelos são
 * imutáveis) e todas as telas que observam o fluxo são atualizadas na hora. É por isso
 * que o que o coach muda no treino do Leonardo aparece imediatamente na aba "Hoje", e o
 * que o Leonardo registra aparece na evolução dele no painel do coach.
 *
 * Nada é salvo em disco: ao fechar o app, os dados voltam ao estado de demonstração.
 */
class GymRepository(private val initialState: () -> GymState) {

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<GymState> = _state.asStateFlow()

    // ------------------------------------------------------------------ Alunos

    fun addStudent(student: Student) {
        _state.update { it.copy(students = it.students + student) }
    }

    // ------------------------------------------------------------------ Treinos

    /** Cria o treino ou substitui o existente com o mesmo ID. */
    fun saveWorkout(studentId: String, workout: Workout) {
        updateStudent(studentId) { student ->
            val exists = student.workouts.any { it.id == workout.id }
            student.copy(
                workouts = if (exists) {
                    student.workouts.map { if (it.id == workout.id) workout else it }
                } else {
                    student.workouts + workout
                },
            )
        }
    }

    fun deleteWorkout(studentId: String, workoutId: String) {
        updateStudent(studentId) { student ->
            student.copy(workouts = student.workouts.filterNot { it.id == workoutId })
        }
    }

    // ------------------------------------------------------------------ Registro do aluno

    /** Registra (ou atualiza) uma série de um exercício. */
    fun logSet(
        studentId: String,
        date: LocalDate,
        workoutId: String,
        exercise: PrescribedExercise,
        setIndex: Int,
        setLog: SetLog,
    ) {
        require(setIndex in 0 until exercise.sets) { "Série inexistente: $setIndex" }
        val key = ExerciseLogKey(studentId, date, exercise.id)
        _state.update { state ->
            val current = state.exerciseLogs[key]
            val updated = ExerciseLog(
                key = key,
                workoutId = workoutId,
                catalogId = exercise.catalogId,
                sets = current?.sets.orEmpty() + (setIndex to setLog),
            )
            state.copy(exerciseLogs = state.exerciseLogs + (key to updated))
        }
    }

    /** Desfaz o registro de uma série. */
    fun removeSetLog(studentId: String, date: LocalDate, prescribedId: String, setIndex: Int) {
        val key = ExerciseLogKey(studentId, date, prescribedId)
        _state.update { state ->
            val current = state.exerciseLogs[key] ?: return@update state
            val remaining = current.sets - setIndex
            state.copy(
                exerciseLogs = if (remaining.isEmpty()) {
                    state.exerciseLogs - key
                } else {
                    state.exerciseLogs + (key to current.copy(sets = remaining))
                },
            )
        }
    }

    /** Registra de uma vez todas as séries que faltam, com os valores informados. */
    fun completeExercise(
        studentId: String,
        date: LocalDate,
        workoutId: String,
        exercise: PrescribedExercise,
        valuesFor: (setIndex: Int) -> SetLog,
    ) {
        val key = ExerciseLogKey(studentId, date, exercise.id)
        _state.update { state ->
            val current = state.exerciseLogs[key]?.sets.orEmpty()
            val allSets = (0 until exercise.sets).associateWith { current[it] ?: valuesFor(it) }
            val log = ExerciseLog(key, workoutId, exercise.catalogId, allSets)
            state.copy(exerciseLogs = state.exerciseLogs + (key to log))
        }
    }

    /** Apaga todas as séries registradas de um exercício no dia. */
    fun clearExercise(studentId: String, date: LocalDate, prescribedId: String) {
        _state.update { it.copy(exerciseLogs = it.exerciseLogs - ExerciseLogKey(studentId, date, prescribedId)) }
    }

    fun toggleCardio(studentId: String, date: LocalDate, cardioId: String) {
        val key = CardioLogKey(studentId, date, cardioId)
        _state.update { state ->
            state.copy(cardioLogs = if (key in state.cardioLogs) state.cardioLogs - key else state.cardioLogs + key)
        }
    }

    /** Desmarca tudo o que o aluno fez em um dia (botão "Reiniciar treino"). */
    fun clearDay(studentId: String, date: LocalDate) {
        _state.update { state ->
            state.copy(
                exerciseLogs = state.exerciseLogs.filterKeys { !(it.studentId == studentId && it.date == date) },
                cardioLogs = state.cardioLogs.filterNot { it.studentId == studentId && it.date == date }.toSet(),
            )
        }
    }

    /** Volta todos os dados ao estado de demonstração. */
    fun resetToDemo() {
        _state.value = initialState()
    }

    private fun updateStudent(studentId: String, transform: (Student) -> Student) {
        _state.update { state ->
            state.copy(students = state.students.map { if (it.id == studentId) transform(it) else it })
        }
    }

    companion object {
        /** Gera IDs únicos para alunos, treinos, exercícios e cardios criados pelo coach. */
        fun newId(): String = UUID.randomUUID().toString()
    }
}

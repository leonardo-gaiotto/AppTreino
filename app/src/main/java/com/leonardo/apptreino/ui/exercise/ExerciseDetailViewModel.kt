package com.leonardo.apptreino.ui.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leonardo.apptreino.AppTreinoApplication
import com.leonardo.apptreino.data.ExerciseCatalog
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.data.GymState
import com.leonardo.apptreino.data.MockData
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.data.model.SetLog
import com.leonardo.apptreino.data.model.Workout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/** Uma linha do registro de séries. */
data class SetRow(
    val index: Int,
    val target: PrescribedExercise,
    val isIsometric: Boolean,
    /** Valores exibidos: o registro, ou o rascunho, ou a prescrição do coach. */
    val reps: Int,
    val loadKg: Double,
    val isRegistered: Boolean,
) {
    val showsLoad: Boolean get() = target.load !is Load.BodyWeight
}

/** Estado da tela de detalhe do exercício. */
data class ExerciseDetailUiState(
    val workout: Workout,
    val letter: Char,
    val exercise: PrescribedExercise,
    val catalog: CatalogExercise,
    /** Posição do exercício no treino (1, 2, 3...). */
    val position: Int,
    val sets: List<SetRow>,
) {
    val registeredSets: Int get() = sets.count { it.isRegistered }
    val isDone: Boolean get() = registeredSets >= exercise.sets
}

/**
 * ViewModel do detalhe do exercício e do REGISTRO DAS SÉRIES.
 *
 * - Os extras da Intent chegam automaticamente no [SavedStateHandle].
 * - Séries ainda não registradas guardam um "rascunho" local (o que o aluno ajustou
 *   com − / +). Ao registrar, o valor vai para o [GymRepository] e passa a aparecer
 *   também na aba "Hoje" e na evolução do aluno no painel do coach.
 * - Ao registrar TODAS as séries, o exercício fica feito automaticamente.
 */
class ExerciseDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GymRepository,
) : ViewModel() {

    private val studentId = MockData.LEONARDO_ID

    private val prescribedId: String =
        savedStateHandle.get<String>(ExerciseDetailActivity.EXTRA_PRESCRIBED_ID).orEmpty()

    private val date: LocalDate =
        savedStateHandle.get<String>(ExerciseDetailActivity.EXTRA_DATE)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()

    /** Rascunho das séries ainda não registradas: índice → valores ajustados. */
    private val drafts = MutableStateFlow<Map<Int, SetLog>>(emptyMap())

    /** `null` se o exercício não existir mais (ex.: o coach removeu do treino). */
    val uiState: StateFlow<ExerciseDetailUiState?> =
        combine(repository.state, drafts) { state, drafts -> buildState(state, drafts) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                buildState(repository.state.value, drafts.value),
            )

    // ------------------------------------------------------------------ Ações

    fun changeReps(index: Int, direction: Int) = updateSet(index) { row ->
        val step = if (row.isIsometric) SECONDS_STEP else REPS_STEP
        SetLog(reps = (row.reps + direction * step).coerceIn(1, MAX_REPS), loadKg = row.loadKg)
    }

    fun changeLoad(index: Int, direction: Int) = updateSet(index) { row ->
        SetLog(reps = row.reps, loadKg = (row.loadKg + direction * LOAD_STEP_KG).coerceIn(0.0, MAX_LOAD_KG))
    }

    /** Registra a série com os valores exibidos, ou desfaz o registro. */
    fun toggleSet(index: Int) {
        val state = uiState.value ?: return
        val row = state.sets.getOrNull(index) ?: return
        if (row.isRegistered) {
            repository.removeSetLog(studentId, date, prescribedId, index)
            drafts.update { it + (index to SetLog(row.reps, row.loadKg)) }
        } else {
            repository.logSet(studentId, date, state.workout.id, state.exercise, index, SetLog(row.reps, row.loadKg))
            drafts.update { it - index }
        }
    }

    /** Botão principal: registra todas as séries que faltam, ou apaga o registro do dia. */
    fun toggleAll() {
        val state = uiState.value ?: return
        if (state.isDone) {
            repository.clearExercise(studentId, date, prescribedId)
        } else {
            val rows = state.sets.associateBy { it.index }
            repository.completeExercise(studentId, date, state.workout.id, state.exercise) { index ->
                val row = rows.getValue(index)
                SetLog(row.reps, row.loadKg)
            }
            drafts.value = emptyMap()
        }
    }

    // ------------------------------------------------------------------ Estado

    /** Ajusta uma série: se já registrada, atualiza o registro; se não, só o rascunho. */
    private fun updateSet(index: Int, transform: (SetRow) -> SetLog) {
        val state = uiState.value ?: return
        val row = state.sets.getOrNull(index) ?: return
        val updated = transform(row)
        if (row.isRegistered) {
            repository.logSet(studentId, date, state.workout.id, state.exercise, index, updated)
        } else {
            drafts.update { it + (index to updated) }
        }
    }

    private fun buildState(state: GymState, drafts: Map<Int, SetLog>): ExerciseDetailUiState? {
        val student = state.student(studentId) ?: return null
        val workout = student.workouts.firstOrNull { w -> w.exercises.any { it.id == prescribedId } } ?: return null
        val exerciseIndex = workout.exercises.indexOfFirst { it.id == prescribedId }
        val exercise = workout.exercises[exerciseIndex]
        val catalog = ExerciseCatalog.find(exercise.catalogId) ?: return null
        val logged = state.exerciseLog(studentId, date, prescribedId)?.sets.orEmpty()

        val rows = (0 until exercise.sets).map { index ->
            val values = logged[index] ?: drafts[index] ?: SetLog(exercise.targetReps, exercise.load.kilograms)
            SetRow(
                index = index,
                target = exercise,
                isIsometric = exercise.reps is Reps.Duration,
                reps = values.reps,
                loadKg = values.loadKg,
                isRegistered = index in logged,
            )
        }
        return ExerciseDetailUiState(
            workout = workout,
            letter = student.letterOf(workout.id),
            exercise = exercise,
            catalog = catalog,
            position = exerciseIndex + 1,
            sets = rows,
        )
    }

    companion object {
        private const val REPS_STEP = 1
        private const val SECONDS_STEP = 5
        private const val LOAD_STEP_KG = 1.0
        private const val MAX_REPS = 600
        private const val MAX_LOAD_KG = 500.0

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppTreinoApplication
                ExerciseDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = app.gymRepository,
                )
            }
        }
    }
}

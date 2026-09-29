package com.leonardo.apptreino.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leonardo.apptreino.AppTreinoApplication
import com.leonardo.apptreino.data.ExerciseCatalog
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.data.model.Cardio
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.data.model.Load
import com.leonardo.apptreino.data.model.PrescribedExercise
import com.leonardo.apptreino.data.model.Reps
import com.leonardo.apptreino.data.model.UiText
import com.leonardo.apptreino.data.model.Workout
import com.leonardo.apptreino.domain.WorkoutError
import com.leonardo.apptreino.domain.WorkoutValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek

/** Rascunho imutável do treino em edição. */
data class WorkoutDraft(
    val id: String,
    val name: String,
    val days: Set<DayOfWeek>,
    val exercises: List<PrescribedExercise>,
    val cardio: List<Cardio>,
)

data class WorkoutEditorUiState(
    val isNew: Boolean,
    val draft: WorkoutDraft,
    /** Dias ocupados por OUTROS treinos do aluno → letra do treino (ex.: SEG → "B"). */
    val takenDays: Map<DayOfWeek, Char>,
    val errors: Set<WorkoutError>,
    val hasChanges: Boolean,
)

/**
 * ViewModel do editor de treino (modo Coach).
 *
 * Guarda um RASCUNHO do treino: nada vai para o repositório até o coach tocar em
 * "Salvar" e a validação passar. O rascunho sobrevive à rotação da tela.
 * Também é compartilhado com os formulários de exercício e de cardio (bottom sheets).
 */
class WorkoutEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: GymRepository,
    private val resolve: (UiText) -> String,
) : ViewModel() {

    private val studentId: String = savedStateHandle.get<String>(WorkoutEditorActivity.EXTRA_STUDENT_ID).orEmpty()
    private val workoutId: String? = savedStateHandle.get<String>(WorkoutEditorActivity.EXTRA_WORKOUT_ID)

    private val student = repository.state.value.student(studentId)
    private val original: Workout? = workoutId?.let { student?.findWorkout(it) }

    private val initialDraft = WorkoutDraft(
        id = original?.id ?: GymRepository.newId(),
        name = original?.name?.let(resolve).orEmpty(),
        days = original?.days.orEmpty(),
        exercises = original?.exercises.orEmpty(),
        cardio = original?.cardio.orEmpty(),
    )

    private val _uiState = MutableStateFlow(
        WorkoutEditorUiState(
            isNew = original == null,
            draft = initialDraft,
            takenDays = takenDays(),
            errors = emptySet(),
            hasChanges = false,
        ),
    )
    val uiState: StateFlow<WorkoutEditorUiState> = _uiState.asStateFlow()

    /** `false` se o aluno não existir (a tela é fechada). */
    val isValid: Boolean get() = student != null

    val studentName: UiText? get() = student?.name

    // ------------------------------------------------------------------ Nome e dias

    fun updateName(name: String) = editDraft(clear = WorkoutError.NAME_REQUIRED) { it.copy(name = name) }

    /** Marca/desmarca um dia. Dias de outros treinos não podem ser escolhidos. */
    fun toggleDay(day: DayOfWeek) {
        if (day in _uiState.value.takenDays) return
        editDraft(clear = WorkoutError.DAYS_REQUIRED) { draft ->
            draft.copy(days = if (day in draft.days) draft.days - day else draft.days + day)
        }
    }

    // ------------------------------------------------------------------ Exercícios

    fun findExercise(prescribedId: String): PrescribedExercise? =
        _uiState.value.draft.exercises.firstOrNull { it.id == prescribedId }

    /** Prescrição sugerida ao adicionar um exercício novo do catálogo. */
    fun defaultPrescription(catalog: CatalogExercise): PrescribedExercise = PrescribedExercise(
        id = GymRepository.newId(),
        catalogId = catalog.id,
        sets = DEFAULT_SETS,
        reps = if (catalog.isIsometric) Reps.Duration(DEFAULT_SECONDS) else Reps.Range(DEFAULT_REPS_MIN, DEFAULT_REPS_MAX),
        load = if (catalog.isBodyWeight) Load.BodyWeight else Load.Kilograms(DEFAULT_LOAD_KG),
        restSeconds = DEFAULT_REST_SECONDS,
    )

    /** Adiciona o exercício ou atualiza o existente com o mesmo ID. */
    fun upsertExercise(exercise: PrescribedExercise) = editDraft(clear = WorkoutError.EMPTY) { draft ->
        val exists = draft.exercises.any { it.id == exercise.id }
        draft.copy(
            exercises = if (exists) draft.exercises.map { if (it.id == exercise.id) exercise else it }
            else draft.exercises + exercise,
        )
    }

    fun removeExercise(prescribedId: String) = editDraft { draft ->
        draft.copy(exercises = draft.exercises.filterNot { it.id == prescribedId })
    }

    /** Aplica a nova ordem dos exercícios (ao soltar depois de arrastar). */
    fun reorderExercises(orderedIds: List<String>) = editDraft { draft ->
        val byId = draft.exercises.associateBy { it.id }
        val reordered = orderedIds.mapNotNull { byId[it] }
        // Segurança: só aplica se a nova ordem tiver exatamente os mesmos exercícios.
        if (reordered.size == draft.exercises.size) draft.copy(exercises = reordered) else draft
    }

    // ------------------------------------------------------------------ Cardio

    fun findCardio(cardioId: String): Cardio? = _uiState.value.draft.cardio.firstOrNull { it.id == cardioId }

    fun upsertCardio(cardio: Cardio) = editDraft(clear = WorkoutError.EMPTY) { draft ->
        val exists = draft.cardio.any { it.id == cardio.id }
        draft.copy(cardio = if (exists) draft.cardio.map { if (it.id == cardio.id) cardio else it } else draft.cardio + cardio)
    }

    fun removeCardio(cardioId: String) = editDraft { draft ->
        draft.copy(cardio = draft.cardio.filterNot { it.id == cardioId })
    }

    // ------------------------------------------------------------------ Salvar

    /** Valida e salva no repositório. @return `true` se salvou. */
    fun save(): Boolean {
        val state = _uiState.value
        val draft = state.draft
        val otherWorkouts = repository.state.value.student(studentId)?.workouts.orEmpty()
        val errors = WorkoutValidator.validate(
            name = draft.name,
            days = draft.days,
            exerciseCount = draft.exercises.size,
            cardioCount = draft.cardio.size,
            workoutId = draft.id,
            otherWorkouts = otherWorkouts,
        )
        _uiState.update { it.copy(errors = errors) }
        if (errors.isNotEmpty()) return false

        // Mantém o nome original (de strings.xml) se o coach não o alterou.
        val name = original?.name?.takeIf { resolve(it) == draft.name.trim() } ?: UiText.Dynamic(draft.name.trim())
        repository.saveWorkout(
            studentId,
            Workout(id = draft.id, name = name, days = draft.days, exercises = draft.exercises, cardio = draft.cardio),
        )
        return true
    }

    private fun editDraft(clear: WorkoutError? = null, transform: (WorkoutDraft) -> WorkoutDraft) {
        _uiState.update { state ->
            val draft = transform(state.draft)
            state.copy(
                draft = draft,
                errors = if (clear != null) state.errors - clear else state.errors,
                hasChanges = draft != initialDraft,
            )
        }
    }

    private fun takenDays(): Map<DayOfWeek, Char> {
        val current = student ?: return emptyMap()
        return current.workouts
            .filter { it.id != workoutId }
            .flatMap { workout -> workout.days.map { it to current.letterOf(workout.id) } }
            .toMap()
    }

    /** Exercício do catálogo, para mostrar nome e miniatura na lista. */
    fun catalogOf(exercise: PrescribedExercise): CatalogExercise? = ExerciseCatalog.find(exercise.catalogId)

    companion object {
        private const val DEFAULT_SETS = 3
        private const val DEFAULT_REPS_MIN = 10
        private const val DEFAULT_REPS_MAX = 12
        private const val DEFAULT_SECONDS = 30
        private const val DEFAULT_LOAD_KG = 10.0
        private const val DEFAULT_REST_SECONDS = 60

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppTreinoApplication
                WorkoutEditorViewModel(createSavedStateHandle(), app.gymRepository, app.textResolver)
            }
        }
    }
}

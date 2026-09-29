package com.leonardo.apptreino.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leonardo.apptreino.AppTreinoApplication
import com.leonardo.apptreino.data.AppSettings
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.data.GymState
import com.leonardo.apptreino.data.MockData
import com.leonardo.apptreino.data.model.AppMode
import com.leonardo.apptreino.data.model.SetLog
import com.leonardo.apptreino.data.model.Student
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/**
 * ViewModel compartilhado pelas abas da [MainActivity].
 *
 * - Sobrevive à rotação de tela (o ViewModel não é recriado junto com a Activity).
 * - O dia selecionado fica no [SavedStateHandle].
 * - Os Fragments obtêm esta mesma instância com `activityViewModels()`, e é assim que
 *   a aba "Semana" consegue escolher o dia exibido na aba "Hoje".
 * - No modo Aluno, as telas mostram os dados do Leonardo ([MockData.LEONARDO_ID]),
 *   que é um dos alunos do coach: os dois modos leem o MESMO [GymRepository].
 */
class MainViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val repository: GymRepository,
    private val settings: AppSettings,
    private val clock: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val studentId = MockData.LEONARDO_ID

    private val selectedDate: StateFlow<LocalDate> =
        savedStateHandle.getStateFlow(KEY_SELECTED_DATE, clock().toString())
            .map(LocalDate::parse)
            .stateIn(viewModelScope, SharingStarted.Eagerly, currentSelectedDate())

    /** Modo atual (Aluno ou Coach): define as abas da Bottom Navigation. */
    val mode: StateFlow<AppMode> = settings.mode

    /** Estado da aba "Hoje": recalculado quando muda o dia ou qualquer dado do repositório. */
    val todayState: StateFlow<TodayUiState> =
        combine(selectedDate, repository.state) { date, state -> buildToday(state, date) }
            .stateIn(viewModelScope, WHILE_SUBSCRIBED, buildToday(repository.state.value, currentSelectedDate()))

    /** Estado da aba "Semana". */
    val weekState: StateFlow<WeekUiState> =
        combine(selectedDate, repository.state) { date, state ->
            MainUiStateMapper.buildWeek(state, state.me(), date, clock())
        }.stateIn(
            viewModelScope, WHILE_SUBSCRIBED,
            MainUiStateMapper.buildWeek(repository.state.value, repository.state.value.me(), currentSelectedDate(), clock()),
        )

    /** Estado da aba "Perfil". */
    val profileState: StateFlow<ProfileUiState> =
        repository.state.map { state -> MainUiStateMapper.buildProfile(state, state.me(), clock()) }
            .stateIn(
                viewModelScope, WHILE_SUBSCRIBED,
                MainUiStateMapper.buildProfile(repository.state.value, repository.state.value.me(), clock()),
            )

    // ------------------------------------------------------------------ Ações

    fun selectDate(date: LocalDate) {
        savedStateHandle[KEY_SELECTED_DATE] = date.toString()
    }

    fun selectToday() = selectDate(clock())

    /**
     * Atalho do check no card: se o exercício não está feito, registra as séries que
     * faltam com os valores que o coach passou; se está feito, apaga o registro.
     */
    fun toggleExercise(item: ExerciseItem) {
        val date = currentSelectedDate()
        val workout = repository.state.value.me().workoutFor(date.dayOfWeek) ?: return
        if (item.isDone) {
            repository.clearExercise(studentId, date, item.exercise.id)
        } else {
            val target = item.exercise
            repository.completeExercise(studentId, date, workout.id, target) {
                SetLog(reps = target.targetReps, loadKg = target.load.kilograms)
            }
        }
    }

    fun toggleCardio(cardioId: String) {
        repository.toggleCardio(studentId, currentSelectedDate(), cardioId)
    }

    fun restartSelectedDay() {
        repository.clearDay(studentId, currentSelectedDate())
    }

    fun setMode(mode: AppMode) = settings.setMode(mode)

    fun resetDemoData() {
        repository.resetToDemo()
        selectToday()
    }

    /** Data em exibição na aba "Hoje" (usada ao abrir o detalhe do exercício). */
    fun currentSelectedDate(): LocalDate =
        savedStateHandle.get<String>(KEY_SELECTED_DATE)?.let(LocalDate::parse) ?: clock()

    private fun buildToday(state: GymState, date: LocalDate): TodayUiState =
        MainUiStateMapper.buildToday(state, state.me(), date, clock())

    private fun GymState.me(): Student = requireNotNull(student(studentId)) { "Aluno do modo Aluno não encontrado" }

    companion object {
        private const val KEY_SELECTED_DATE = "selected_date"

        /** Mantém a coleta ativa por 5 s após a tela sumir (ex.: rotação), evitando recomputar. */
        private val WHILE_SUBSCRIBED = SharingStarted.WhileSubscribed(5_000)

        /** Fábrica que injeta as dependências vindas do [AppTreinoApplication]. */
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppTreinoApplication
                MainViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = app.gymRepository,
                    settings = app.settings,
                )
            }
        }
    }
}


package com.leonardo.apptreino.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leonardo.apptreino.AppTreinoApplication
import com.leonardo.apptreino.data.ExerciseCatalog
import com.leonardo.apptreino.data.model.CatalogExercise
import com.leonardo.apptreino.data.model.MuscleGroup
import com.leonardo.apptreino.data.model.UiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CatalogUiState(
    val query: String,
    /** `null` = todos os grupos. */
    val group: MuscleGroup?,
    val exercises: List<CatalogExercise>,
)

/** ViewModel do catálogo: busca por nome (sem acentos) e filtro por grupo muscular. */
class CatalogViewModel(private val resolve: (UiText) -> String) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState(query = "", group = null, exercises = ExerciseCatalog.all))
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    fun search(query: String) = update(query = query, group = _uiState.value.group)

    fun filter(group: MuscleGroup?) = update(query = _uiState.value.query, group = group)

    private fun update(query: String, group: MuscleGroup?) {
        _uiState.value = CatalogUiState(
            query = query,
            group = group,
            exercises = ExerciseCatalog.search(query, group) { resolve(UiText.Resource(it.nameRes)) },
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppTreinoApplication
                CatalogViewModel(app.textResolver)
            }
        }
    }
}

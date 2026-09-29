package com.leonardo.apptreino.ui.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.leonardo.apptreino.AppTreinoApplication
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.domain.StudentField
import com.leonardo.apptreino.domain.StudentInput
import com.leonardo.apptreino.domain.StudentValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel do cadastro de aluno: valida os campos e, se estiver tudo certo,
 * adiciona o aluno ao repositório compartilhado (a lista do coach atualiza na hora).
 */
class StudentFormViewModel(private val repository: GymRepository) : ViewModel() {

    /** Erros por campo (mensagens de strings.xml). Sobrevive à rotação da tela. */
    private val _errors = MutableStateFlow<Map<StudentField, Int>>(emptyMap())
    val errors: StateFlow<Map<StudentField, Int>> = _errors.asStateFlow()

    /** @return `true` se o aluno foi salvo. */
    fun save(input: StudentInput): Boolean {
        val errors = StudentValidator.validate(input)
        _errors.value = errors
        if (errors.isNotEmpty()) return false

        repository.addStudent(StudentValidator.toStudent(input, id = GymRepository.newId()))
        return true
    }

    /** Limpa o erro de um campo assim que o coach começa a corrigi-lo. */
    fun clearError(field: StudentField) {
        if (field in _errors.value) _errors.value = _errors.value - field
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as AppTreinoApplication
                StudentFormViewModel(app.gymRepository)
            }
        }
    }
}

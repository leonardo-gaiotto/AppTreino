package com.leonardo.apptreino.data

import androidx.appcompat.app.AppCompatDelegate
import com.leonardo.apptreino.data.model.AppMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferências da sessão, em memória: modo (Aluno/Coach) e tema.
 * O app sempre abre no modo Aluno com o tema escuro.
 */
class AppSettings {

    private val _mode = MutableStateFlow(AppMode.STUDENT)
    val mode: StateFlow<AppMode> = _mode.asStateFlow()

    fun setMode(mode: AppMode) {
        _mode.value = mode
    }

    var isDarkTheme: Boolean = true
        set(value) {
            field = value
            applyTheme()
        }

    /** Aplica o tema. O AppCompat recria as Activities abertas quando o modo muda. */
    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO,
        )
    }
}

package com.leonardo.apptreino

import android.app.Application
import com.leonardo.apptreino.data.AppSettings
import com.leonardo.apptreino.data.GymRepository
import com.leonardo.apptreino.data.MockData
import com.leonardo.apptreino.data.model.UiText
import java.time.LocalDate

/**
 * Ponto de entrada do app, criado antes de qualquer Activity.
 *
 * Funciona como um "container" simples de dependências: o repositório e as
 * configurações são criados uma única vez e compartilhados por todas as telas
 * (via fábricas dos ViewModels). Tudo fica em memória.
 */
class AppTreinoApplication : Application() {

    val gymRepository: GymRepository by lazy {
        GymRepository(initialState = { MockData.create(LocalDate.now()) })
    }

    val settings: AppSettings by lazy { AppSettings() }

    /** Converte [UiText] em texto (usado pelos ViewModels para buscar por nome). */
    val textResolver: (UiText) -> String = { it.resolve(resources) }

    override fun onCreate() {
        super.onCreate()
        // Tema escuro por padrão, aplicado antes da primeira tela aparecer.
        settings.applyTheme()
    }
}

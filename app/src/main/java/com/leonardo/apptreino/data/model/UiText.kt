package com.leonardo.apptreino.data.model

import android.content.res.Resources
import androidx.annotation.StringRes

/**
 * Texto exibido na interface, que pode vir de dois lugares:
 * - [Resource]: textos do app e dos dados mockados, sempre em `strings.xml`;
 * - [Dynamic]: dados digitados pelo usuário (nome de um aluno novo, nome de um treino...).
 *
 * Assim os modelos continuam sem texto fixo no código e ainda aceitam dados do coach.
 */
sealed interface UiText {

    data class Resource(@StringRes val resId: Int) : UiText

    data class Dynamic(val value: String) : UiText

    fun resolve(res: Resources): String = when (this) {
        is Resource -> res.getString(resId)
        is Dynamic -> value
    }
}

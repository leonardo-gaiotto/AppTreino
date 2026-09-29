package com.leonardo.apptreino.data.model

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes

/**
 * Exercício do CATÁLOGO: tudo o que não muda de aluno para aluno — nome, grupo muscular,
 * passo a passo, erros comuns e vídeo. Ao adicionar ao treino, o coach só define a
 * prescrição ([PrescribedExercise]); o vídeo e a miniatura vêm daqui automaticamente.
 */
data class CatalogExercise(
    val id: String,
    @StringRes val nameRes: Int,
    val group: MuscleGroup,
    /** Músculo-alvo mais específico, ex.: "Peitoral superior". */
    @StringRes val targetRes: Int,
    @ArrayRes val stepsRes: Int,
    @ArrayRes val commonMistakesRes: Int,
    val youtubeVideoId: String,
    /** Isométrico (ex.: prancha): prescrito em segundos, não em repetições. */
    val isIsometric: Boolean = false,
    /** Feito com o peso do corpo por padrão (ex.: barra fixa). */
    val isBodyWeight: Boolean = false,
)

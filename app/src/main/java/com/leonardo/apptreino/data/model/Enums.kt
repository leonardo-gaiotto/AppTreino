package com.leonardo.apptreino.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.leonardo.apptreino.R

/** Modo de uso do app: o aluno acompanha o treino; o coach gerencia os alunos. */
enum class AppMode { STUDENT, COACH }

/** Grupos musculares usados no filtro do catálogo. */
enum class MuscleGroup(@StringRes val labelRes: Int) {
    CHEST(R.string.group_chest),
    BACK(R.string.group_back),
    SHOULDERS(R.string.group_shoulders),
    BICEPS(R.string.group_biceps),
    TRICEPS(R.string.group_triceps),
    LEGS(R.string.group_legs),
    GLUTES(R.string.group_glutes),
    CALVES(R.string.group_calves),
    CORE(R.string.group_core),
}

/** Objetivo do aluno. */
enum class Goal(@StringRes val labelRes: Int) {
    HYPERTROPHY(R.string.goal_hypertrophy),
    WEIGHT_LOSS(R.string.goal_weight_loss),
    CONDITIONING(R.string.goal_conditioning),
    STRENGTH(R.string.goal_strength),
    HEALTH(R.string.goal_health),
}

/** Tipos de cardio que o coach pode prescrever. */
enum class CardioType(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
    TREADMILL(R.string.cardio_treadmill, R.drawable.ic_cardio_run),
    BIKE(R.string.cardio_bike, R.drawable.ic_cardio_bike),
    ELLIPTICAL(R.string.cardio_elliptical, R.drawable.ic_cardio_run),
    STAIRS(R.string.cardio_stairs, R.drawable.ic_cardio_stairs),
    JUMP_ROPE(R.string.cardio_jump_rope, R.drawable.ic_cardio_bolt),
    ROWING(R.string.cardio_rowing, R.drawable.ic_cardio_rowing),
}

/** Intensidade do cardio. */
enum class Intensity(@StringRes val labelRes: Int) {
    LIGHT(R.string.intensity_light),
    MODERATE(R.string.intensity_moderate),
    HIGH(R.string.intensity_high),
}

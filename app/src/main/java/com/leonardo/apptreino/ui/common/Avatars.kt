package com.leonardo.apptreino.ui.common

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.leonardo.apptreino.R

/** Paleta dos avatares dos alunos (fundo colorido + iniciais escuras). */
private val AVATAR_COLORS = intArrayOf(
    R.color.avatar_1,
    R.color.avatar_2,
    R.color.avatar_3,
    R.color.avatar_4,
    R.color.avatar_5,
)

/** Mostra as iniciais de [name] num círculo com a cor da posição [colorIndex]. */
fun TextView.bindAvatar(name: String, colorIndex: Int) {
    text = Formatters.initials(name)
    setBackgroundResource(R.drawable.bg_circle_surface_variant)
    backgroundTintList = ColorStateList.valueOf(
        ContextCompat.getColor(context, AVATAR_COLORS[colorIndex.mod(AVATAR_COLORS.size)]),
    )
    setTextColor(ContextCompat.getColor(context, R.color.brand_ink))
}

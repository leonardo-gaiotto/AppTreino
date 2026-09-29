package com.leonardo.apptreino.ui.common

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding

/*
 * Edge-to-edge: o app desenha atrás das barras do sistema (status bar e navegação).
 * Cada tela usa estas extensões para empurrar o seu conteúdo para fora dessas áreas.
 * O padding original do layout é preservado e o inset é somado a ele.
 */

/**
 * Soma o tamanho das barras do sistema ao padding desta View.
 *
 * Use na View raiz de cada tela (com `clipToPadding` = true, o padrão), assim o
 * conteúdo rolável é cortado ANTES da status bar e nunca passa por baixo do relógio.
 */
fun View.applySystemBarsPadding(top: Boolean = true, bottom: Boolean = false, includeKeyboard: Boolean = false) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        // Em telas com formulário, o teclado também "empurra" o conteúdo para cima
        // (no modo edge-to-edge o adjustResize não faz isso sozinho).
        val keyboard = if (includeKeyboard) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
        view.updatePadding(
            left = initialLeft + bars.left,
            top = initialTop + if (top) bars.top else 0,
            right = initialRight + bars.right,
            bottom = initialBottom + if (bottom) maxOf(bars.bottom, keyboard) else 0,
        )
        insets
    }
    requestApplyInsetsWhenAttached()
}

/**
 * Pede os insets assim que a View estiver na tela. Necessário para Fragments adicionados
 * depois da primeira passagem de insets (ex.: ao trocar de aba pela primeira vez).
 */
fun View.requestApplyInsetsWhenAttached() {
    doOnAttach { ViewCompat.requestApplyInsets(it) }
}

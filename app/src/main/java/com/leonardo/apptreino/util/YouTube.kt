package com.leonardo.apptreino.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Utilidades para os vídeos do YouTube: URLs de miniatura/vídeo e abertura do vídeo. */
object YouTube {

    private val VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_-]{11}$")

    /** IDs do YouTube têm sempre 11 caracteres (letras, números, `-` e `_`). */
    fun isValidVideoId(videoId: String): Boolean = VIDEO_ID_PATTERN.matches(videoId)

    /** Tamanhos de miniatura (todos em 16:9, sem faixas pretas). */
    enum class Thumbnail(val fileName: String) {
        /** 320×180: leve, ideal para os cards da lista. */
        MEDIUM("mqdefault"),

        /** 1280×720: para a imagem grande do detalhe. */
        HD("maxresdefault"),
    }

    fun thumbnailUrl(videoId: String, size: Thumbnail): String =
        "https://img.youtube.com/vi/$videoId/${size.fileName}.jpg"

    fun watchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    /**
     * Abre o vídeo com uma Intent IMPLÍCITA (ACTION_VIEW): o Android escolhe quem trata o
     * link — o app do YouTube, se instalado, ou o navegador.
     *
     * @return `false` se nenhum app no aparelho conseguir abrir o link.
     */
    fun openVideo(context: Context, videoId: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, watchUrl(videoId).toUri())
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}

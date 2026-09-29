package com.leonardo.apptreino.ui.common

import android.widget.ImageView
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.leonardo.apptreino.R
import com.leonardo.apptreino.util.YouTube

/**
 * Carrega a miniatura do vídeo com o Coil: download em segundo plano, cache em memória
 * e disco, e uma cor neutra enquanto carrega ou se o aparelho estiver offline.
 */
fun ImageView.loadVideoThumbnail(videoId: String, size: YouTube.Thumbnail) {
    load(YouTube.thumbnailUrl(videoId, size)) {
        crossfade(true)
        placeholder(R.drawable.placeholder_thumbnail)
        error(R.drawable.placeholder_thumbnail)
    }
}

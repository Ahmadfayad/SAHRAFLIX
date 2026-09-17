package com.sahraflix.data.remote

import com.sahraflix.domain.model.PlayUrl
import javax.inject.Inject

class VidSrcResolver @Inject constructor() {
    fun resolveMovie(tmdbId: String): PlayUrl =
        PlayUrl.Embed("https://vidsrc.to/embed/movie/${requireId(tmdbId)}")

    fun resolveEpisode(tmdbId: String, season: Int, episode: Int): PlayUrl =
        PlayUrl.Embed("https://vidsrc.to/embed/tv/${requireId(tmdbId)}/$season/$episode")

    private fun requireId(value: String): Int = value.toIntOrNull()?.takeIf { it > 0 }
        ?: error("Invalid TMDB ID")
}

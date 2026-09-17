package com.sahraflix.data.remote

interface StreamingProviderResolver {
    suspend fun resolveMovie(tmdbId: String): List<ResolvedStream>
    suspend fun resolveEpisode(tmdbId: String, season: Int, episode: Int): List<ResolvedStream>
}

data class ResolvedStream(
    val hlsUrl: String,
    val referer: String?,
    val userAgent: String? = null,
    val needsProxy: Boolean,
    val quality: String,
    val provider: String
)

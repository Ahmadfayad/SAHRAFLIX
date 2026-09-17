package com.sahraflix.domain.model

data class StreamingContent(
    val tmdbId: String,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val overview: String?,
    val releaseYear: Int?,
    val rating: Float?,
    val type: StreamType,
    val source: StreamingSource = StreamingSource.VIDSRC_TMDB
)

enum class StreamingSource {
    VIDSRC_TMDB,
    CINEJOY
}

fun StreamingContent.toCatalogEntry(): CatalogEntry.Streaming = CatalogEntry.Streaming(
    id = "streaming:$tmdbId",
    title = title,
    posterUrl = posterUrl,
    tmdbId = tmdbId.toIntOrNull() ?: 0,
    overview = overview,
    releaseYear = releaseYear,
    rating = rating?.toDouble(),
    source = ContentSource.VIDSRC_TMDB
)

package com.sahraflix.domain.model

sealed interface CatalogEntry {
    val id: String
    val title: String
    val posterUrl: String?

    data class Iptv(
        val stream: StreamItem,
        val source: ContentSource
    ) : CatalogEntry {
        override val id: String = stream.id
        override val title: String = stream.name
        override val posterUrl: String? = stream.logoUrl
    }

    data class Streaming(
        override val id: String,
        override val title: String,
        override val posterUrl: String?,
        val tmdbId: Int,
        val overview: String?,
        val releaseYear: Int?,
        val rating: Double?,
        val seasons: List<TmdbSeason> = emptyList(),
        val source: ContentSource = ContentSource.VIDSRC_TMDB
    ) : CatalogEntry
}

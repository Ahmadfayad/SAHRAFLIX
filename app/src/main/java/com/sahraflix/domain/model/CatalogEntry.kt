package com.sahraflix.domain.model

sealed interface CatalogEntry {
    val id: String
    val title: String
    val posterUrl: String?

    data class Iptv(
        val stream: StreamItem,
        val source: ContentSource
    ) : CatalogEntry {
        override val id: String get() = stream.id
        override val title: String get() = stream.name
        override val posterUrl: String? get() = stream.logoUrl
    }

    data class Tmdb(
        val tmdbId: Int,
        override val title: String,
        override val posterUrl: String?,
        val isSeries: Boolean,
        val overview: String? = null,
        val releaseYear: Int? = null,
        val rating: Double? = null,
        val backdropUrl: String? = null
    ) : CatalogEntry {
        override val id: String get() = routeId(tmdbId, isSeries)

        companion object {
            fun routeId(tmdbId: Int, isSeries: Boolean) = "tmdb-${if (isSeries) "tv" else "movie"}-$tmdbId"
        }
    }
}

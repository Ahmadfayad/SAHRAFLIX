package com.sahraflix.domain.model

sealed interface ContentDetails {
    val entry: CatalogEntry
    val description: String?
    val rating: Double?
    val releaseYear: Int?

    data class Iptv(
        override val entry: CatalogEntry.Iptv,
        override val description: String? = null,
        override val rating: Double? = null,
        override val releaseYear: Int? = null
    ) : ContentDetails

    data class Streaming(
        override val entry: CatalogEntry.Streaming,
        override val description: String?,
        override val rating: Double?,
        override val releaseYear: Int?,
        val backdropUrl: String?,
        val genres: List<String>,
        val cast: List<TmdbCastMember>,
        val crew: List<TmdbCrewMember>,
        val seasons: List<SeasonSummary>,
        val episodes: Map<Int, List<EpisodeInfo>> = emptyMap()
    ) : ContentDetails
}

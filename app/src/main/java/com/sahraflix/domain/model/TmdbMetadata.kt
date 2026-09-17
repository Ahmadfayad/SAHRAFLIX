package com.sahraflix.domain.model

data class TmdbMovie(
    val id: Int,
    val title: String,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val releaseYear: Int?,
    val rating: Double?,
    val runtimeMinutes: Int?,
    val genres: List<String> = emptyList(),
    val cast: List<TmdbCastMember> = emptyList(),
    val crew: List<TmdbCrewMember> = emptyList()
)

data class TmdbTvShow(
    val id: Int,
    val name: String,
    val overview: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val firstAirYear: Int?,
    val rating: Double?,
    val genres: List<String> = emptyList(),
    val seasons: List<TmdbSeason> = emptyList(),
    val cast: List<TmdbCastMember> = emptyList(),
    val crew: List<TmdbCrewMember> = emptyList()
)

data class TmdbSeason(
    val id: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val posterUrl: String?,
    val episodeCount: Int,
    val episodes: List<TmdbEpisode> = emptyList()
)

data class TmdbEpisode(
    val id: Int,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val stillUrl: String?,
    val airDate: String?,
    val rating: Double?
)

data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val profileUrl: String?
)

data class TmdbCrewMember(
    val id: Int,
    val name: String,
    val department: String?,
    val job: String?,
    val profileUrl: String?
)

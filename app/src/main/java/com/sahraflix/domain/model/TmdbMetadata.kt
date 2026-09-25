package com.sahraflix.domain.model

data class TmdbSeason(
    val id: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val posterUrl: String?,
    val episodeCount: Int
)

data class TmdbCastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val profileUrl: String?
)

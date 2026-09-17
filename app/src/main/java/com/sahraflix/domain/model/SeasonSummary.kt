package com.sahraflix.domain.model

data class SeasonSummary(
    val id: Int,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val posterUrl: String?,
    val episodeCount: Int
)

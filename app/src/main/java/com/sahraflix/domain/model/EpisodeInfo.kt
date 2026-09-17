package com.sahraflix.domain.model

data class EpisodeInfo(
    val id: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String,
    val overview: String?,
    val stillUrl: String?,
    val airDate: String?,
    val rating: Double?
)

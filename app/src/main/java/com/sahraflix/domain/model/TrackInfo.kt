package com.sahraflix.domain.model

data class TrackInfo(
    val id: String,
    val name: String,
    val language: String?,
    val selected: Boolean = false
)

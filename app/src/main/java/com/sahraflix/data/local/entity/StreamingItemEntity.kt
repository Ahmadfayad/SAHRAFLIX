package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** TMDB metadata cache ("Discover" rails). contentType is MOVIE or SERIES. */
@Entity(
    tableName = "streaming_items",
    indices = [
        Index(value = ["contentType", "title"]),
        Index(value = ["contentType", "tmdbId"], unique = true),
        Index(value = ["contentType", "rank"])
    ]
)
data class StreamingItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val posterUrl: String?,
    val contentType: String,
    val overview: String?,
    val releaseYear: Int?,
    val rating: Double?,
    val tmdbId: Int,
    val lastUpdated: Long,
    val backdropUrl: String? = null,
    /** Position in TMDB's popularity ordering at sync time. */
    val rank: Int = 0
)

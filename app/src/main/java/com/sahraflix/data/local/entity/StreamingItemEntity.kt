package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streaming_items",
    indices = [Index(value = ["contentType", "title"]), Index(value = ["tmdbId"], unique = true)]
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
    val lastUpdated: Long
)

package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress", indices = [Index(value = ["updatedAt"])])
data class WatchProgressEntity(
    @PrimaryKey val contentId: String,
    val title: String,
    val posterUrl: String?,
    val streamUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

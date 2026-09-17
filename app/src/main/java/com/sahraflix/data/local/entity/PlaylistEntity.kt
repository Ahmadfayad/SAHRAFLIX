package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val username: String?,
    val password: String?,
    val type: PlaylistType,
    val lastUpdated: Long,
    val epgUrl: String? = null
)

enum class PlaylistType {
    XTREAM,
    M3U,
    STALKER
}

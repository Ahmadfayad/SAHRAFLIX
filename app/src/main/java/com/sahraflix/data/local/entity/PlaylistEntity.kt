package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** M3U: playlist URL. Xtream: server base URL. Stalker: portal URL. */
    val url: String,
    val username: String?,
    val password: String?,
    val type: PlaylistType,
    val lastUpdated: Long,
    val epgUrl: String? = null,
    /** Stalker MAC address (00:1A:79:xx:xx:xx). */
    val macAddress: String? = null,
    /** Custom User-Agent some providers require. */
    val userAgent: String? = null,
    val syncState: SyncState = SyncState.NEVER,
    val syncMessage: String? = null,
    val itemCount: Int = 0
)

enum class PlaylistType { XTREAM, M3U, STALKER }

enum class SyncState { NEVER, RUNNING, OK, FAILED }

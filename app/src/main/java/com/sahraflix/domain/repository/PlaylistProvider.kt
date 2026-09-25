package com.sahraflix.domain.repository

import com.sahraflix.data.local.entity.PlaylistEntity
import com.sahraflix.data.repository.SyncWriter

interface PlaylistProvider {
    /** Fetches all categories and streams, writing them through [writer]. Throws on failure. */
    suspend fun sync(playlist: PlaylistEntity, writer: SyncWriter)

    /** Optional EPG URL the provider knows about (e.g. Xtream xmltv.php). */
    fun defaultEpgUrl(playlist: PlaylistEntity): String? = null
}

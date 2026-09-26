package com.sahraflix.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.sahraflix.data.local.entity.PlaylistEntity
import com.sahraflix.data.local.entity.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE, id")
    suspend fun getAll(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlaylistEntity?

    @Query("UPDATE playlists SET epgUrl = :epgUrl WHERE id = :playlistId AND (epgUrl IS NULL OR epgUrl = '')")
    suspend fun setEpgUrlIfMissing(playlistId: String, epgUrl: String)

    @Query("UPDATE playlists SET syncState = :state, syncMessage = :message WHERE id = :id")
    suspend fun setSyncState(id: String, state: SyncState, message: String?)

    @Query("UPDATE playlists SET syncState = 'OK', syncMessage = :message, itemCount = :count, lastUpdated = :time WHERE id = :id")
    suspend fun markSynced(id: String, count: Int, message: String?, time: Long)

    /** Upsert keeps the row (and its streams, via FK) when the same playlist is re-added. */
    @Upsert
    suspend fun upsert(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)
}

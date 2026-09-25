package com.sahraflix.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sahraflix.data.local.entity.EpgProgrammeEntity
import kotlinx.coroutines.flow.Flow

/** A programme joined to the stream that shows it. */
data class NowPlayingRow(
    val streamId: String,
    val streamName: String,
    val logoUrl: String?,
    val title: String,
    val startTime: Long,
    val endTime: Long
)

@Dao
interface EpgDao {
    @Upsert
    suspend fun upsert(programmes: List<EpgProgrammeEntity>)

    @Query(
        "SELECT * FROM epg_programmes WHERE playlistId = :playlistId AND channelId = :channelId " +
            "AND endTime >= :fromTime AND startTime <= :toTime ORDER BY startTime"
    )
    fun window(playlistId: String, channelId: String, fromTime: Long, toTime: Long): Flow<List<EpgProgrammeEntity>>

    @Query(
        "SELECT s.id AS streamId, s.name AS streamName, s.logoUrl AS logoUrl, p.title AS title, " +
            "p.startTime AS startTime, p.endTime AS endTime FROM epg_programmes p " +
            "JOIN stream_items s ON s.playlistId = p.playlistId AND s.epgChannelId = p.channelId " +
            "WHERE p.startTime <= :now AND p.endTime > :now AND s.streamType = 'LIVE' " +
            "GROUP BY s.id ORDER BY s.sortOrder LIMIT :limit"
    )
    fun observeNowPlaying(now: Long, limit: Int): Flow<List<NowPlayingRow>>

    @Query(
        "SELECT s.id AS streamId, s.name AS streamName, s.logoUrl AS logoUrl, p.title AS title, " +
            "p.startTime AS startTime, p.endTime AS endTime FROM epg_programmes p " +
            "JOIN stream_items s ON s.playlistId = p.playlistId AND s.epgChannelId = p.channelId " +
            "WHERE p.title LIKE '%' || :query || '%' AND p.endTime > :now " +
            "GROUP BY s.id, p.startTime ORDER BY p.startTime LIMIT 40"
    )
    suspend fun search(query: String, now: Long): List<NowPlayingRow>

    @Query("DELETE FROM epg_programmes WHERE endTime < :cutoff")
    suspend fun deleteBefore(cutoff: Long)

    @Query("DELETE FROM epg_programmes WHERE playlistId = :playlistId")
    suspend fun deleteForPlaylist(playlistId: String)
}

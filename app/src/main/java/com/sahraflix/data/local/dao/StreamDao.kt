package com.sahraflix.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sahraflix.data.local.entity.StreamItemEntity
import com.sahraflix.domain.model.StreamType
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamDao {
    /** Upsert (UPDATE-or-INSERT) — unlike REPLACE it never deletes rows, so no cascade side-effects. */
    @Upsert
    suspend fun upsertStreams(streams: List<StreamItemEntity>)

    @Query("SELECT * FROM stream_items WHERE streamType = :type ORDER BY sortOrder, name COLLATE NOCASE")
    fun pagingByType(type: StreamType): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE categoryId = :categoryId ORDER BY sortOrder, name COLLATE NOCASE")
    fun pagingByCategory(categoryId: String): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE playlistId = :playlistId AND streamType = 'LIVE' ORDER BY sortOrder, name COLLATE NOCASE")
    fun pagingLiveChannels(playlistId: String): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE streamType = 'LIVE' ORDER BY sortOrder, name COLLATE NOCASE")
    fun pagingAllLiveChannels(): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE name LIKE '%' || :query || '%' ORDER BY streamType, name COLLATE NOCASE")
    fun pagingSearch(query: String): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE name LIKE '%' || :query || '%' ORDER BY streamType, name COLLATE NOCASE LIMIT :limit")
    suspend fun searchPreview(query: String, limit: Int): List<StreamItemEntity>

    /** Candidates for matching a TMDB title against the user's VOD library. */
    @Query("SELECT * FROM stream_items WHERE streamType = :type AND name LIKE '%' || :titleFragment || '%' LIMIT 60")
    suspend fun findVodCandidates(type: StreamType, titleFragment: String): List<StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StreamItemEntity?

    @Query("SELECT * FROM stream_items WHERE id IN (:ids)")
    fun observeByIds(ids: List<String>): Flow<List<StreamItemEntity>>

    @Query("SELECT DISTINCT epgChannelId FROM stream_items WHERE playlistId = :playlistId AND epgChannelId IS NOT NULL AND epgChannelId != ''")
    suspend fun epgChannelIds(playlistId: String): List<String>

    @Query("DELETE FROM stream_items WHERE playlistId = :playlistId AND syncStamp != :stamp")
    suspend fun deleteStale(playlistId: String, stamp: Long): Int

    @Query("SELECT COUNT(*) FROM stream_items WHERE playlistId = :playlistId")
    suspend fun countForPlaylist(playlistId: String): Int

    @Query("SELECT COUNT(*) FROM stream_items")
    fun observeTotalCount(): Flow<Int>
}

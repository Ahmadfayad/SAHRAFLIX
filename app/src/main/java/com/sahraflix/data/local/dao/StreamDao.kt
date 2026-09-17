package com.sahraflix.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sahraflix.data.local.entity.StreamItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreams(streams: List<StreamItemEntity>)

    @Query("SELECT * FROM stream_items ORDER BY name COLLATE NOCASE, id")
    fun getStreams(): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE playlistId = :playlistId ORDER BY name COLLATE NOCASE, id")
    fun getStreamsByPlaylist(playlistId: String): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE playlistId = :playlistId AND streamType = 'LIVE' ORDER BY name COLLATE NOCASE, id")
    fun getLiveChannels(playlistId: String): PagingSource<Int, StreamItemEntity>

    @Query("SELECT id FROM stream_items WHERE playlistId = :playlistId AND epgChannelId = :channelId LIMIT 1")
    suspend fun findIdByEpgChannel(playlistId: String, channelId: String): String?

    @Query("SELECT * FROM stream_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): StreamItemEntity?

    @Query("SELECT providerId FROM stream_items WHERE playlistId = :playlistId AND providerId IS NOT NULL ORDER BY providerId")
    fun getProviderIdsByPlaylist(playlistId: String): PagingSource<Int, String>

    @Query("SELECT id FROM stream_items WHERE playlistId = :playlistId AND providerId = :providerId LIMIT 1")
    suspend fun findIdByProviderId(playlistId: String, providerId: String): String?

    @Query("SELECT * FROM stream_items WHERE categoryId = :categoryId AND streamType = :type ORDER BY name COLLATE NOCASE, id")
    fun getStreamsByCategory(categoryId: String, type: String): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE streamType = :type ORDER BY name COLLATE NOCASE, id")
    fun getStreamsByType(type: String): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE, id")
    fun searchStreams(query: String): PagingSource<Int, StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE, id LIMIT 50")
    suspend fun searchPreview(query: String): List<StreamItemEntity>

    @Query("SELECT * FROM stream_items WHERE id IN (:ids) ORDER BY name COLLATE NOCASE, id")
    fun observeByIds(ids: List<String>): Flow<List<StreamItemEntity>>

    @Query("DELETE FROM stream_items WHERE playlistId = :playlistId")
    suspend fun deleteByPlaylist(playlistId: String)
}

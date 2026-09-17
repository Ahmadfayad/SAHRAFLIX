package com.sahraflix.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sahraflix.data.local.entity.StreamingItemEntity

@Dao
interface StreamingItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<StreamingItemEntity>)

    @Query("SELECT * FROM streaming_items WHERE contentType = :contentType ORDER BY title COLLATE NOCASE, id")
    fun pagingByType(contentType: String): PagingSource<Int, StreamingItemEntity>

    @Query("SELECT * FROM streaming_items WHERE title LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE, id")
    fun search(query: String): PagingSource<Int, StreamingItemEntity>

    @Query("SELECT * FROM streaming_items WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun getByTmdbId(tmdbId: Int): StreamingItemEntity?
}

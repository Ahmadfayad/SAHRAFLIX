package com.sahraflix.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sahraflix.data.local.entity.StreamingItemEntity

@Dao
interface StreamingItemDao {
    @Upsert
    suspend fun upsertAll(items: List<StreamingItemEntity>)

    @Query("SELECT * FROM streaming_items WHERE contentType = :contentType ORDER BY rank, title COLLATE NOCASE")
    fun pagingByType(contentType: String): PagingSource<Int, StreamingItemEntity>

    @Query("SELECT COUNT(*) FROM streaming_items")
    suspend fun count(): Int
}

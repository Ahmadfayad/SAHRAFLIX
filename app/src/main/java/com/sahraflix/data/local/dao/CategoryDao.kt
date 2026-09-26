package com.sahraflix.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sahraflix.data.local.entity.CategoryEntity
import com.sahraflix.domain.model.StreamType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE streamType = :type ORDER BY playlistId, sortOrder, name COLLATE NOCASE")
    fun observeByType(type: StreamType): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE playlistId = :playlistId AND syncStamp != :stamp")
    suspend fun deleteStale(playlistId: String, stamp: Long)
}

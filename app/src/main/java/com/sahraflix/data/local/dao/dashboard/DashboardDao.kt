package com.sahraflix.data.local.dao.dashboard

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sahraflix.data.local.entity.FavoriteEntity
import com.sahraflix.data.local.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(item: FavoriteEntity)

    @Delete
    suspend fun removeFavorite(item: FavoriteEntity)

    @Query("SELECT * FROM watch_progress WHERE positionMs > 0 AND positionMs < durationMs ORDER BY updatedAt DESC LIMIT 20")
    fun observeContinueWatching(): Flow<List<WatchProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(item: WatchProgressEntity)
}

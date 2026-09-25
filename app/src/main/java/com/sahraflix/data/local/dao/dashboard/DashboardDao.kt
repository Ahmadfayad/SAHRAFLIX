package com.sahraflix.data.local.dao.dashboard

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.sahraflix.data.local.entity.FavoriteEntity
import com.sahraflix.data.local.entity.WatchProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE streamId = :streamId)")
    fun observeIsFavorite(streamId: String): Flow<Boolean>

    @Upsert
    suspend fun addFavorite(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE streamId = :streamId")
    suspend fun removeFavorite(streamId: String)

    /** In-progress items: started, and not within the last 5% (treated as finished). */
    @Query(
        "SELECT * FROM watch_progress WHERE positionMs > 30000 AND durationMs > 0 " +
            "AND positionMs < durationMs * 0.95 ORDER BY updatedAt DESC LIMIT 20"
    )
    fun observeContinueWatching(): Flow<List<WatchProgressEntity>>

    @Query("SELECT * FROM watch_progress WHERE contentId = :contentId LIMIT 1")
    suspend fun getProgress(contentId: String): WatchProgressEntity?

    @Upsert
    suspend fun saveProgress(item: WatchProgressEntity)

    @Query("DELETE FROM watch_progress WHERE contentId = :contentId")
    suspend fun clearProgress(contentId: String)
}

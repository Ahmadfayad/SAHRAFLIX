package com.sahraflix.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sahraflix.data.local.entity.EpgEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EpgEventEntity>)

    @Query("SELECT * FROM epg_events WHERE streamId = :streamId AND endTime >= :currentTime ORDER BY startTime ASC")
    fun getEpgForStream(streamId: String, currentTime: Long): Flow<List<EpgEventEntity>>

    @Query("SELECT * FROM epg_events WHERE streamId = :streamId AND endTime >= :fromTime AND startTime <= :toTime ORDER BY startTime ASC")
    fun getEpgWindow(streamId: String, fromTime: Long, toTime: Long): Flow<List<EpgEventEntity>>

    @Query("DELETE FROM epg_events WHERE streamId = :streamId")
    suspend fun deleteForStream(streamId: String)

    @Query("DELETE FROM epg_events WHERE endTime < :cutoff")
    suspend fun deletePastEvents(cutoff: Long)

    @Query("SELECT * FROM epg_events WHERE startTime <= :now AND endTime >= :now ORDER BY startTime ASC LIMIT 50")
    fun observeCurrentEvents(now: Long): Flow<List<EpgEventEntity>>

    @Query("SELECT * FROM epg_events WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY startTime ASC LIMIT 50")
    suspend fun searchEvents(query: String): List<EpgEventEntity>
}

package com.sahraflix.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sahraflix.data.local.dao.CategoryDao
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.dao.UserProfileDao
import com.sahraflix.data.local.dao.StreamingItemDao
import com.sahraflix.data.local.dao.dashboard.DashboardDao
import com.sahraflix.data.local.entity.CategoryEntity
import com.sahraflix.data.local.entity.EpgEventEntity
import com.sahraflix.data.local.entity.PlaylistEntity
import com.sahraflix.data.local.entity.StreamItemEntity
import com.sahraflix.data.local.entity.UserProfileEntity
import com.sahraflix.data.local.entity.StreamingItemEntity
import com.sahraflix.data.local.entity.FavoriteEntity
import com.sahraflix.data.local.entity.WatchProgressEntity

@Database(
    entities = [
        PlaylistEntity::class,
        CategoryEntity::class,
        StreamItemEntity::class,
        EpgEventEntity::class,
        UserProfileEntity::class,
        StreamingItemEntity::class,
        FavoriteEntity::class,
        WatchProgressEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(IptvConverters::class)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun categoryDao(): CategoryDao
    abstract fun streamDao(): StreamDao
    abstract fun epgDao(): EpgDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun streamingItemDao(): StreamingItemDao
    abstract fun dashboardDao(): DashboardDao
}

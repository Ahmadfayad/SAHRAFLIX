package com.sahraflix.di

import android.content.Context
import androidx.room.Room
import com.sahraflix.data.local.ALL_MIGRATIONS
import com.sahraflix.data.local.IptvDatabase
import com.sahraflix.data.local.dao.CategoryDao
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.dao.StreamingItemDao
import com.sahraflix.data.local.dao.UserProfileDao
import com.sahraflix.data.local.dao.dashboard.DashboardDao
import com.sahraflix.data.repository.ProfileRepositoryImpl
import com.sahraflix.data.repository.UnifiedContentRepository
import com.sahraflix.domain.repository.ContentRepository
import com.sahraflix.domain.repository.ProfileRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideIptvDatabase(@ApplicationContext context: Context): IptvDatabase =
        Room.databaseBuilder(context, IptvDatabase::class.java, "iptv.db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides fun providePlaylistDao(db: IptvDatabase): PlaylistDao = db.playlistDao()
    @Provides fun provideCategoryDao(db: IptvDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideStreamDao(db: IptvDatabase): StreamDao = db.streamDao()
    @Provides fun provideEpgDao(db: IptvDatabase): EpgDao = db.epgDao()
    @Provides fun provideUserProfileDao(db: IptvDatabase): UserProfileDao = db.userProfileDao()
    @Provides fun provideStreamingItemDao(db: IptvDatabase): StreamingItemDao = db.streamingItemDao()
    @Provides fun provideDashboardDao(db: IptvDatabase): DashboardDao = db.dashboardDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindContentRepository(impl: UnifiedContentRepository): ContentRepository
    @Binds @Singleton abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
}

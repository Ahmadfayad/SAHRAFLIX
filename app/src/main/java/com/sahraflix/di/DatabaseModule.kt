package com.sahraflix.di

import android.content.Context
import androidx.room.Room
import com.sahraflix.data.local.IptvDatabase
import com.sahraflix.data.local.MIGRATION_1_2
import com.sahraflix.data.local.MIGRATION_2_3
import com.sahraflix.data.local.MIGRATION_3_4
import com.sahraflix.data.local.MIGRATION_4_5
import com.sahraflix.data.local.dao.CategoryDao
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.dao.UserProfileDao
import com.sahraflix.data.local.dao.StreamingItemDao
import com.sahraflix.data.repository.ProfileRepositoryImpl
import com.sahraflix.data.repository.StreamRepositoryImpl
import com.sahraflix.domain.repository.ProfileRepository
import com.sahraflix.domain.repository.StreamRepository
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
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .addMigrations(MIGRATION_4_5)
            .build()

    @Provides
    fun providePlaylistDao(database: IptvDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideCategoryDao(database: IptvDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideStreamDao(database: IptvDatabase): StreamDao = database.streamDao()

    @Provides
    fun provideEpgDao(database: IptvDatabase): EpgDao = database.epgDao()

    @Provides
    fun provideUserProfileDao(database: IptvDatabase): UserProfileDao = database.userProfileDao()

    @Provides
    fun provideStreamingItemDao(database: IptvDatabase): StreamingItemDao = database.streamingItemDao()

    @Provides
    fun provideStreamRepository(implementation: StreamRepositoryImpl): StreamRepository = implementation

    @Provides
    fun provideProfileRepository(implementation: ProfileRepositoryImpl): ProfileRepository = implementation
}

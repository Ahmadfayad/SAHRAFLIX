package com.sahraflix.di

import com.sahraflix.domain.repository.IptvVideoPlayer
import com.sahraflix.player.Media3PlayerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {
    @Provides
    @Singleton
    fun provideIptvVideoPlayer(implementation: Media3PlayerImpl): IptvVideoPlayer = implementation

}

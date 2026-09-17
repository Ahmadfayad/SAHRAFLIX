package com.sahraflix.di

import com.sahraflix.data.remote.VidSrcApi
import com.sahraflix.data.remote.LocalHlsProxy
import com.sahraflix.data.repository.TmdbStreamingRepository
import com.sahraflix.domain.repository.StreamingRepository
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StreamingModule {
    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().build()

    @Provides
    @Singleton
    fun provideVidSrcApi(client: OkHttpClient, moshi: Moshi): VidSrcApi = VidSrcApi(client, moshi)

    @Provides
    @Singleton
    fun provideLocalHlsProxy(client: OkHttpClient): LocalHlsProxy = LocalHlsProxy(client)

    @Provides
    @Singleton
    fun provideStreamingRepository(repository: TmdbStreamingRepository): StreamingRepository = repository

}

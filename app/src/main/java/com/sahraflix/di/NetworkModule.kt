package com.sahraflix.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.sahraflix.data.remote.TmdbApi
import com.sahraflix.data.remote.TmdbClient
import com.sahraflix.data.repository.TmdbVidsrcProvider
import com.sahraflix.domain.repository.tmdb.TmdbCatalogProvider
import com.sahraflix.data.repository.TmdbContentRepository
import com.sahraflix.domain.repository.ContentRepository
import com.sahraflix.data.local.dao.StreamingItemDao
import com.sahraflix.data.repository.UnifiedContentRepository
import com.sahraflix.BuildConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val cookieManager = CookieManager(null, CookiePolicy.ACCEPT_ALL)
        val cookieJar = JavaNetCookieJar(cookieManager)
        val bootstrap = OkHttpClient.Builder().cookieJar(cookieJar).build()
        val doh = DnsOverHttps.Builder()
            .client(bootstrap)
            .url("https://dns.google/dns-query".toHttpUrl())
            .build()
        return OkHttpClient.Builder()
        .dns(doh)
        .cookieJar(cookieJar)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()
            )
        }
        .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(client: OkHttpClient): TmdbApi = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
        .create(TmdbApi::class.java)

    @Provides
    fun provideTmdbClient(api: TmdbApi): TmdbClient = TmdbClient(api, BuildConfig.TMDB_API_KEY)

    @Provides
    @Singleton
    fun provideTmdbCatalogProvider(provider: TmdbVidsrcProvider): TmdbCatalogProvider = provider

    @Provides
    @Singleton
    fun provideTmdbContentRepository(
        streamingDao: StreamingItemDao,
        tmdbClient: TmdbClient,
        vidSrcResolver: com.sahraflix.data.remote.VidSrcResolver
    ): TmdbContentRepository = TmdbContentRepository(streamingDao, tmdbClient, vidSrcResolver)

    @Provides
    @Singleton
    fun provideContentRepository(repository: UnifiedContentRepository): ContentRepository = repository
}

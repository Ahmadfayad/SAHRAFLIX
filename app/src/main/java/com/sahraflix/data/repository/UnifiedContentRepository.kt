package com.sahraflix.data.repository

import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.map
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentDetails
import com.sahraflix.domain.model.ContentSource
import com.sahraflix.domain.model.PlayUrl
import com.sahraflix.domain.repository.ContentRepository
import com.sahraflix.domain.repository.StreamRepository
import kotlinx.coroutines.flow.Flow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UnifiedContentRepository @Inject constructor(
    private val streamRepository: StreamRepository,
    private val tmdbRepository: TmdbContentRepository
) : ContentRepository {
    override fun discoverLiveTv(source: ContentSource): Flow<PagingData<CatalogEntry>> =
        streamRepository.streams().map { page ->
            page.map { CatalogEntry.Iptv(it, source) }
        }

    override fun discoverMovies(source: ContentSource?): Flow<PagingData<CatalogEntry>> = when (source) {
        ContentSource.VIDSRC_TMDB -> tmdbRepository.discoverMovies(source)
        else -> streamRepository.streams().map { page -> page.map { CatalogEntry.Iptv(it, source ?: ContentSource.IPTV_M3U) } }
    }

    override fun discoverSeries(source: ContentSource?): Flow<PagingData<CatalogEntry>> = when (source) {
        ContentSource.VIDSRC_TMDB -> tmdbRepository.discoverSeries(source)
        else -> streamRepository.streams().map { page -> page.map { CatalogEntry.Iptv(it, source ?: ContentSource.IPTV_M3U) } }
    }

    override fun search(query: String, source: ContentSource?): Flow<PagingData<CatalogEntry>> =
        if (source == ContentSource.VIDSRC_TMDB) {
            tmdbRepository.search(query, source)
        } else {
            streamRepository.search(query).map { page ->
                page.map { CatalogEntry.Iptv(it, source ?: ContentSource.IPTV_M3U) }
            }
        }

    override suspend fun getDetails(entry: CatalogEntry): ContentDetails = when (entry) {
        is CatalogEntry.Iptv -> ContentDetails.Iptv(entry)
        is CatalogEntry.Streaming -> tmdbRepository.getDetails(entry)
    }

    override suspend fun resolveStream(entry: CatalogEntry): PlayUrl = when (entry) {
        is CatalogEntry.Iptv -> PlayUrl.Direct(entry.stream.streamUrl)
        is CatalogEntry.Streaming -> tmdbRepository.resolveStream(entry)
    }
}

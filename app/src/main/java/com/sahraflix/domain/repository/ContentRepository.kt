package com.sahraflix.domain.repository

import androidx.paging.PagingData
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentDetails
import com.sahraflix.domain.model.IptvEpisode
import com.sahraflix.domain.model.PlayRequest
import com.sahraflix.domain.model.StreamType
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    /** IPTV items of one type across all playlists (optionally one category). */
    fun iptv(type: StreamType, categoryId: String? = null): Flow<PagingData<CatalogEntry>>
    fun tmdbMovies(): Flow<PagingData<CatalogEntry>>
    fun tmdbSeries(): Flow<PagingData<CatalogEntry>>
    fun searchIptv(query: String): Flow<PagingData<CatalogEntry>>
    suspend fun searchTmdb(query: String): List<CatalogEntry.Tmdb>

    suspend fun iptvEntry(streamId: String): CatalogEntry.Iptv?
    suspend fun iptvDetails(streamId: String): ContentDetails.Iptv
    suspend fun tmdbDetails(tmdbId: Int, isSeries: Boolean): ContentDetails.Tmdb

    /** Builds a playable request; resolves Stalker links and missing URLs. */
    suspend fun playRequest(entry: CatalogEntry.Iptv): PlayRequest
    suspend fun playRequest(series: CatalogEntry.Iptv, episode: IptvEpisode): PlayRequest
    suspend fun catchupRequest(streamId: String, startMs: Long, endMs: Long): PlayRequest?
}

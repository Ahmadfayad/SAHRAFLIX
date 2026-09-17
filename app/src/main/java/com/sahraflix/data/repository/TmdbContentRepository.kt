package com.sahraflix.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.sahraflix.data.local.dao.StreamingItemDao
import com.sahraflix.data.remote.TmdbClient
import com.sahraflix.data.remote.VidSrcResolver
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentDetails
import com.sahraflix.domain.model.ContentSource
import com.sahraflix.domain.model.EpisodeInfo
import com.sahraflix.domain.model.PlayUrl
import com.sahraflix.domain.model.SeasonSummary
import com.sahraflix.domain.model.TmdbTvShow
import com.sahraflix.domain.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TmdbContentRepository @Inject constructor(
    private val streamingDao: StreamingItemDao,
    private val tmdbClient: TmdbClient,
    private val vidSrcResolver: VidSrcResolver
) : ContentRepository {
    suspend fun syncDiscoverCatalog(maxPages: Int = 5) {
        syncMovies(maxPages)
        syncSeries(maxPages)
    }

    private suspend fun syncMovies(maxPages: Int) {
        var page = 1
        var totalPages = 1
        while (page <= minOf(maxPages, totalPages)) {
            val result = tmdbClient.discoverMovies(page)
            totalPages = result.total_pages
            streamingDao.upsertAll(result.results.map { item ->
                com.sahraflix.data.local.entity.StreamingItemEntity(
                    id = "tmdb:movie:${item.id}", title = item.title.orEmpty(), posterUrl = image(item.poster_path),
                    contentType = TYPE_MOVIE, overview = item.overview, releaseYear = item.release_date?.take(4)?.toIntOrNull(),
                    rating = item.vote_average, tmdbId = item.id, lastUpdated = System.currentTimeMillis()
                )
            })
            page++
        }
    }

    private suspend fun syncSeries(maxPages: Int) {
        var page = 1
        var totalPages = 1
        while (page <= minOf(maxPages, totalPages)) {
            val result = tmdbClient.discoverTv(page)
            totalPages = result.total_pages
            streamingDao.upsertAll(result.results.map { item ->
                com.sahraflix.data.local.entity.StreamingItemEntity(
                    id = "tmdb:series:${item.id}", title = item.name.orEmpty(), posterUrl = image(item.poster_path),
                    contentType = TYPE_SERIES, overview = item.overview, releaseYear = item.first_air_date?.take(4)?.toIntOrNull(),
                    rating = item.vote_average, tmdbId = item.id, lastUpdated = System.currentTimeMillis()
                )
            })
            page++
        }
    }
    override fun discoverLiveTv(source: ContentSource): Flow<PagingData<CatalogEntry>> =
        kotlinx.coroutines.flow.flowOf(PagingData.empty())

    override fun discoverMovies(source: ContentSource?): Flow<PagingData<CatalogEntry>> =
        pagingForType(TYPE_MOVIE)

    override fun discoverSeries(source: ContentSource?): Flow<PagingData<CatalogEntry>> =
        pagingForType(TYPE_SERIES)

    override fun search(query: String, source: ContentSource?): Flow<PagingData<CatalogEntry>> =
        Pager(PagingConfig(pageSize = 40, enablePlaceholders = false)) {
            streamingDao.search(query)
        }.flow.map { page -> page.map(::toEntry) }

    override suspend fun getDetails(entry: CatalogEntry): ContentDetails {
        require(entry is CatalogEntry.Streaming && entry.source == ContentSource.VIDSRC_TMDB)
        val movie = runCatching { tmdbClient.movie(entry.tmdbId) }.getOrNull()
        if (movie != null) {
            return ContentDetails.Streaming(
                entry = entry,
                description = movie.overview,
                rating = movie.rating,
                releaseYear = movie.releaseYear,
                backdropUrl = movie.backdropUrl,
                genres = movie.genres,
                cast = movie.cast,
                crew = movie.crew,
                seasons = emptyList()
            )
        }
        val show = tmdbClient.tv(entry.tmdbId)
        return ContentDetails.Streaming(
            entry = entry.copy(title = show.name, overview = show.overview, rating = show.rating, releaseYear = show.firstAirYear, posterUrl = show.posterUrl, seasons = show.seasons),
            description = show.overview,
            rating = show.rating,
            releaseYear = show.firstAirYear,
            backdropUrl = show.backdropUrl,
            genres = show.genres,
            cast = show.cast,
            crew = show.crew,
            seasons = show.seasons.map { SeasonSummary(it.id, it.seasonNumber, it.name, it.overview, it.posterUrl, it.episodeCount) }
        )
    }

    override suspend fun resolveStream(entry: CatalogEntry): PlayUrl {
        require(entry is CatalogEntry.Streaming)
        return if (entry.seasons.isEmpty()) {
            vidSrcResolver.resolveMovie(entry.tmdbId.toString())
        } else {
            vidSrcResolver.resolveEpisode(entry.tmdbId.toString(), 1, 1)
        }
    }

    private fun pagingForType(type: String): Flow<PagingData<CatalogEntry>> =
        Pager(PagingConfig(pageSize = 40, enablePlaceholders = false)) {
            streamingDao.pagingByType(type)
        }.flow.map { page -> page.map(::toEntry) }

    private fun toEntry(item: com.sahraflix.data.local.entity.StreamingItemEntity): CatalogEntry =
        CatalogEntry.Streaming(
            id = item.id,
            title = item.title,
            posterUrl = item.posterUrl,
            tmdbId = item.tmdbId,
            overview = item.overview,
            releaseYear = item.releaseYear,
            rating = item.rating
        )

    private fun image(path: String?): String? = path?.let { "https://image.tmdb.org/t/p/w780$it" }

    private companion object {
        const val TYPE_MOVIE = "MOVIE"
        const val TYPE_SERIES = "SERIES"
    }
}

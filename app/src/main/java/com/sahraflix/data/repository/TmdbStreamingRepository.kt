package com.sahraflix.data.repository

import com.sahraflix.data.remote.LocalHlsProxy
import com.sahraflix.data.remote.TmdbClient
import com.sahraflix.data.remote.TmdbStreamingResolver
import com.sahraflix.data.remote.ResolvedStream
import com.sahraflix.domain.model.PlayUrl
import com.sahraflix.domain.model.StreamType
import com.sahraflix.domain.model.StreamingContent
import com.sahraflix.domain.model.StreamingSource
import com.sahraflix.domain.repository.StreamingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TmdbStreamingRepository @Inject constructor(
    private val tmdbClient: TmdbClient,
    private val resolver: TmdbStreamingResolver,
    private val hlsProxy: LocalHlsProxy
) : StreamingRepository {
    override suspend fun discoverMovies(page: Int): List<StreamingContent> =
        tmdbClient.discoverMovies(page).results.map { movie ->
            StreamingContent(
                tmdbId = movie.id.toString(),
                title = movie.title.orEmpty(),
                posterUrl = image(movie.poster_path),
                backdropUrl = image(movie.backdrop_path),
                overview = movie.overview,
                releaseYear = movie.release_date?.take(4)?.toIntOrNull(),
                rating = movie.vote_average?.toFloat(),
                type = StreamType.MOVIE
            )
        }

    override suspend fun discoverTvShows(page: Int): List<StreamingContent> =
        tmdbClient.discoverTv(page).results.map { show ->
            StreamingContent(
                tmdbId = show.id.toString(),
                title = show.name.orEmpty(),
                posterUrl = image(show.poster_path),
                backdropUrl = image(show.backdrop_path),
                overview = show.overview,
                releaseYear = show.first_air_date?.take(4)?.toIntOrNull(),
                rating = show.vote_average?.toFloat(),
                type = StreamType.SERIES
            )
        }

    override suspend fun search(query: String): List<StreamingContent> =
        tmdbClient.search(query).results.mapNotNull { item ->
            val type = when (item.media_type) {
                "movie" -> StreamType.MOVIE
                "tv" -> StreamType.SERIES
                else -> return@mapNotNull null
            }
            StreamingContent(
                tmdbId = item.id.toString(),
                title = item.title ?: item.name.orEmpty(),
                posterUrl = image(item.poster_path),
                backdropUrl = image(item.backdrop_path),
                overview = item.overview,
                releaseYear = (item.release_date ?: item.first_air_date)?.take(4)?.toIntOrNull(),
                rating = item.vote_average?.toFloat(),
                type = type
            )
        }

    override suspend fun resolveStream(
        content: StreamingContent,
        season: Int?,
        episode: Int?
    ): PlayUrl.Direct {
        hlsProxy.startIfNeeded()
        val streams = if (content.type == StreamType.MOVIE) {
            resolver.resolveMovie(content.tmdbId)
        } else {
            resolver.resolveEpisode(
                content.tmdbId,
                requireNotNull(season) { "Series playback requires a season" },
                requireNotNull(episode) { "Series playback requires an episode" }
            )
        }
        val best = streams.maxByOrNull { it.qualityRank() }
            ?: error("No playable stream found")
        val proxied = hlsProxy.proxyUrl(best.hlsUrl, best.referer)
        return PlayUrl.Direct(proxied)
    }

    private fun image(path: String?): String? = path?.let { "https://image.tmdb.org/t/p/w780$it" }

    private fun ResolvedStream.qualityRank(): Int = when (quality.uppercase()) {
        "4K", "2160P" -> 4
        "1080P" -> 3
        "720P" -> 2
        "480P" -> 1
        else -> 0
    }
}

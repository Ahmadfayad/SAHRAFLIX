package com.sahraflix.data.repository

import com.sahraflix.domain.model.PlayUrl
import com.sahraflix.domain.model.TmdbMovie
import com.sahraflix.domain.model.TmdbTvShow
import com.sahraflix.data.remote.TmdbClient
import com.sahraflix.domain.repository.tmdb.TmdbCatalogProvider
import javax.inject.Inject

class TmdbVidsrcProvider @Inject constructor(
    private val tmdbClient: TmdbClient
) : TmdbCatalogProvider {
    override suspend fun getMovie(tmdbId: Int): TmdbMovie = tmdbClient.movie(tmdbId)

    override suspend fun getTvShow(tmdbId: Int): TmdbTvShow = tmdbClient.tv(tmdbId)

    override fun playbackUrl(
        tmdbId: Int,
        isSeries: Boolean,
        season: Int?,
        episode: Int?
    ): PlayUrl {
        require(!isSeries || (season != null && episode != null)) {
            "Series playback requires season and episode"
        }
        val url = if (isSeries) {
            "https://vidsrc.to/embed/tv/$tmdbId/$season/$episode"
        } else {
            "https://vidsrc.to/embed/movie/$tmdbId"
        }
        return PlayUrl.Embed(url)
    }
}

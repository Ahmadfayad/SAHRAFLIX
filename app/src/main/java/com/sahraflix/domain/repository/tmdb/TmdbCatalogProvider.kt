package com.sahraflix.domain.repository.tmdb

import com.sahraflix.domain.model.PlayUrl
import com.sahraflix.domain.model.TmdbMovie
import com.sahraflix.domain.model.TmdbTvShow

interface TmdbCatalogProvider {
    suspend fun getMovie(tmdbId: Int): TmdbMovie
    suspend fun getTvShow(tmdbId: Int): TmdbTvShow
    fun playbackUrl(tmdbId: Int, isSeries: Boolean, season: Int? = null, episode: Int? = null): PlayUrl
}

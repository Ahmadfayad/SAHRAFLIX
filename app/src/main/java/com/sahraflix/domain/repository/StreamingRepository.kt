package com.sahraflix.domain.repository

import com.sahraflix.domain.model.PlayUrl
import com.sahraflix.domain.model.StreamingContent

interface StreamingRepository {
    suspend fun discoverMovies(page: Int = 1): List<StreamingContent>
    suspend fun discoverTvShows(page: Int = 1): List<StreamingContent>
    suspend fun search(query: String): List<StreamingContent>
    suspend fun resolveStream(content: StreamingContent, season: Int? = null, episode: Int? = null): PlayUrl.Direct
}

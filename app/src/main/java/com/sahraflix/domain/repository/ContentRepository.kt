package com.sahraflix.domain.repository

import androidx.paging.PagingData
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentDetails
import com.sahraflix.domain.model.ContentSource
import com.sahraflix.domain.model.PlayUrl
import kotlinx.coroutines.flow.Flow

interface ContentRepository {
    fun discoverLiveTv(source: ContentSource): Flow<PagingData<CatalogEntry>>
    fun discoverMovies(source: ContentSource?): Flow<PagingData<CatalogEntry>>
    fun discoverSeries(source: ContentSource?): Flow<PagingData<CatalogEntry>>
    fun search(query: String, source: ContentSource?): Flow<PagingData<CatalogEntry>>
    suspend fun getDetails(entry: CatalogEntry): ContentDetails
    suspend fun resolveStream(entry: CatalogEntry): PlayUrl
}

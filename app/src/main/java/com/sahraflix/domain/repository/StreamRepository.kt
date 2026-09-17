package com.sahraflix.domain.repository

import androidx.paging.PagingData
import com.sahraflix.domain.model.StreamItem
import kotlinx.coroutines.flow.Flow

interface StreamRepository {
    fun streams(playlistId: String? = null): Flow<PagingData<StreamItem>>
    fun search(query: String): Flow<PagingData<StreamItem>>
}

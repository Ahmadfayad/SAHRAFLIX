package com.sahraflix.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.domain.model.StreamItem
import com.sahraflix.domain.repository.StreamRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StreamRepositoryImpl @Inject constructor(
    private val streamDao: StreamDao
) : StreamRepository {
    override fun streams(playlistId: String?): Flow<PagingData<StreamItem>> = Pager(
        config = PagingConfig(pageSize = 40, prefetchDistance = 12, enablePlaceholders = false),
        pagingSourceFactory = {
            playlistId?.let(streamDao::getStreamsByPlaylist) ?: streamDao.getStreams()
        }
    ).flow.map { data ->
        data.map(::toDomain)
    }

    override fun search(query: String): Flow<PagingData<StreamItem>> = Pager(
        config = PagingConfig(pageSize = 40, enablePlaceholders = false),
        pagingSourceFactory = { streamDao.searchStreams(query) }
    ).flow.map { data -> data.map(::toDomain) }

    private fun toDomain(entity: com.sahraflix.data.local.entity.StreamItemEntity) = StreamItem(
        id = entity.id,
        name = entity.name,
        streamUrl = entity.streamUrl,
        logoUrl = entity.logoUrl,
        streamType = entity.streamType,
        categoryId = entity.categoryId,
        playlistId = entity.playlistId,
        previewUrl = entity.streamUrl,
        catchupType = entity.catchupType,
        catchupSource = entity.catchupSource
    )
}

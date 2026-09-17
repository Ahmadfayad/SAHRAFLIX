package com.sahraflix.presentation.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.domain.model.EpgProgram
import com.sahraflix.domain.model.StreamItem
import com.sahraflix.domain.model.StreamType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.paging.cachedIn
import javax.inject.Inject

@HiltViewModel
class EpgViewModel @Inject constructor(
    private val streamDao: StreamDao,
    private val epgDao: EpgDao
) : ViewModel() {
    fun channels(playlistId: String): Flow<PagingData<StreamItem>> = Pager(
        config = PagingConfig(pageSize = 50, prefetchDistance = 15, enablePlaceholders = false),
        pagingSourceFactory = { streamDao.getLiveChannels(playlistId) }
    ).flow.map { data ->
        data.map { entity ->
            StreamItem(
                id = entity.id,
                name = entity.name,
                streamUrl = entity.streamUrl,
                logoUrl = entity.logoUrl,
                streamType = StreamType.LIVE,
                categoryId = entity.categoryId,
                playlistId = entity.playlistId
            )
        }
    }.cachedIn(viewModelScope)

    fun programs(streamId: String, fromTime: Long, toTime: Long): Flow<List<EpgProgram>> =
        epgDao.getEpgWindow(streamId, fromTime, toTime).map { events ->
            events.map { event ->
                EpgProgram(
                    id = event.id,
                    streamId = event.streamId,
                    title = event.title,
                    description = event.description,
                    startTime = event.startTime,
                    endTime = event.endTime
                )
            }
        }
}

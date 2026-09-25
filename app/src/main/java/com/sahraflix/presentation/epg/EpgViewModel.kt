package com.sahraflix.presentation.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.entity.PlaylistEntity
import com.sahraflix.data.repository.UnifiedContentRepository.Companion.toEntry
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.EpgProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EpgViewModel @Inject constructor(
    private val streamDao: StreamDao,
    private val epgDao: EpgDao,
    playlistDao: PlaylistDao
) : ViewModel() {
    val playlists: StateFlow<List<PlaylistEntity>> = playlistDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedId = MutableStateFlow<String?>(null)
    val selected: StateFlow<String?> = selectedId

    fun select(id: String?) { selectedId.value = id }

    val channels: Flow<PagingData<CatalogEntry.Iptv>> = selectedId.flatMapLatest { id ->
        Pager(PagingConfig(pageSize = 30, prefetchDistance = 10, enablePlaceholders = false)) {
            if (id == null) streamDao.pagingAllLiveChannels() else streamDao.pagingLiveChannels(id)
        }.flow.map { page -> page.map { it.toEntry() } }
    }.cachedIn(viewModelScope)

    fun programmes(channel: CatalogEntry.Iptv, from: Long, to: Long): Flow<List<EpgProgram>> {
        val epgId = channel.stream.epgChannelId ?: return flowOf(emptyList())
        return epgDao.window(channel.stream.playlistId, epgId, from, to).map { list ->
            list.map { EpgProgram(it.channelId, it.title, it.description, it.startTime, it.endTime) }
        }
    }
}

package com.sahraflix.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.repository.ContentRepository
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.domain.model.StreamItem
import com.sahraflix.domain.model.StreamType
import com.sahraflix.domain.model.ContentSource
import com.sahraflix.domain.model.StreamingContent
import com.sahraflix.domain.model.toCatalogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val streamDao: StreamDao,
    private val epgDao: EpgDao,
    private val streamingRepository: com.sahraflix.domain.repository.StreamingRepository
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results = _query
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) kotlinx.coroutines.flow.flowOf(PagingData.empty())
            else repository.search(query.trim(), null)
        }
        .cachedIn(viewModelScope)

    data class SmartSearchGroups(
        val liveNow: List<CatalogEntry> = emptyList(),
        val channels: List<CatalogEntry> = emptyList(),
        val moviesAndSeries: List<CatalogEntry> = emptyList()
    )

    val groupedResults: StateFlow<SmartSearchGroups> = _query
        .debounce(250)
        .distinctUntilChanged()
        .flatMapLatest { query -> flow {
            if (query.isBlank()) {
                emit(SmartSearchGroups())
            } else {
                val channels = streamDao.searchPreview(query).map { entity ->
                    CatalogEntry.Iptv(StreamItem(entity.id, entity.name, entity.streamUrl, entity.logoUrl, entity.streamType, entity.categoryId, entity.playlistId), ContentSource.IPTV_M3U)
                }
                val epg = epgDao.searchEvents(query).map { event ->
                    CatalogEntry.Iptv(StreamItem(event.streamId, event.title, "", null, StreamType.LIVE, "epg", "epg"), ContentSource.IPTV_M3U)
                }
                val vod = runCatching { streamingRepository.search(query) }
                    .getOrDefault(emptyList())
                    .map(StreamingContent::toCatalogEntry)
                emit(SmartSearchGroups(liveNow = epg, channels = channels, moviesAndSeries = vod))
            }
        } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmartSearchGroups())

    fun setQuery(value: String) {
        _query.value = value
    }
}

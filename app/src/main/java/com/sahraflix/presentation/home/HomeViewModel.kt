package com.sahraflix.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentSource
import com.sahraflix.domain.repository.ContentRepository
import com.sahraflix.domain.repository.StreamingRepository
import com.sahraflix.domain.model.StreamingContent
import com.sahraflix.data.local.dao.dashboard.DashboardDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.remote.NetworkMonitor
import com.sahraflix.domain.model.StreamItem
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import androidx.paging.cachedIn
import com.sahraflix.domain.model.StreamType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val streamingRepository: StreamingRepository,
    private val dashboardDao: DashboardDao,
    private val streamDao: StreamDao,
    private val epgDao: EpgDao,
    networkMonitor: NetworkMonitor
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    val liveStreams: Flow<PagingData<CatalogEntry>> = contentRepository
        .discoverLiveTv(ContentSource.IPTV_M3U).cachedIn(viewModelScope)
    val movieStreams: Flow<PagingData<CatalogEntry>> = contentRepository
        .discoverMovies(ContentSource.IPTV_M3U).cachedIn(viewModelScope)
    val seriesStreams: Flow<PagingData<CatalogEntry>> = contentRepository
        .discoverSeries(ContentSource.IPTV_M3U).cachedIn(viewModelScope)

    val continueWatching: Flow<List<CatalogEntry>> = dashboardDao.observeContinueWatching().map { items ->
        items.map { progress -> CatalogEntry.Iptv(StreamItem(progress.contentId, progress.title, progress.streamUrl, progress.posterUrl, StreamType.MOVIE, "continue", "history"), ContentSource.IPTV_M3U) }
    }

    val favoriteLive: Flow<List<CatalogEntry>> = dashboardDao.observeFavorites()
        .map { favorites -> favorites.map { it.streamId } }
        .flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList())
            else streamDao.observeByIds(ids).map { streams -> streams.map { CatalogEntry.Iptv(StreamItem(it.id, it.name, it.streamUrl, it.logoUrl, it.streamType, it.categoryId, it.playlistId), ContentSource.IPTV_M3U) } }
        }

    val epgHighlights: Flow<List<CatalogEntry>> = epgDao.observeCurrentEvents(System.currentTimeMillis())
        .map { events -> events.map { event -> CatalogEntry.Iptv(StreamItem(event.streamId, event.title, "", null, StreamType.LIVE, "epg", "epg"), ContentSource.IPTV_M3U) } }

    private val _streamingMovies = MutableStateFlow<List<StreamingContent>>(emptyList())
    val streamingMovies: StateFlow<List<StreamingContent>> = _streamingMovies.asStateFlow()
    private val _streamingShows = MutableStateFlow<List<StreamingContent>>(emptyList())
    val streamingShows: StateFlow<List<StreamingContent>> = _streamingShows.asStateFlow()

    init {
        loadStreamingContent()
    }

    private fun loadStreamingContent() {
        viewModelScope.launch {
            runCatching {
                _streamingMovies.value = streamingRepository.discoverMovies()
                _streamingShows.value = streamingRepository.discoverTvShows()
            }
        }
    }
}

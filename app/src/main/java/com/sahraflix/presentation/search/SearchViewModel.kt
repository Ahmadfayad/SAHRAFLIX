package com.sahraflix.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.repository.UnifiedContentRepository.Companion.toEntry
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentSource
import com.sahraflix.domain.model.StreamItem
import com.sahraflix.domain.model.StreamType
import com.sahraflix.domain.repository.ContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchResults(
    val channels: List<CatalogEntry> = emptyList(),
    val vod: List<CatalogEntry> = emptyList(),
    val programmes: List<Pair<CatalogEntry, String>> = emptyList(),
    val tmdb: List<CatalogEntry> = emptyList(),
    val searching: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ContentRepository,
    private val streamDao: StreamDao,
    private val epgDao: EpgDao
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<SearchResults> = _query
        .debounce(300)
        .distinctUntilChanged()
        .mapLatest { q ->
            val query = q.trim()
            if (query.length < 2) return@mapLatest SearchResults()
            val local = streamDao.searchPreview(query, 60).map { it.toEntry() }
            val programmes = epgDao.search(query, System.currentTimeMillis()).map { r ->
                CatalogEntry.Iptv(StreamItem(r.streamId, r.streamName, "", r.logoUrl, StreamType.LIVE, "", ""), ContentSource.IPTV_M3U) to
                    "${r.title} · ${time(r.startTime)}"
            }
            val tmdb = runCatching { repository.searchTmdb(query) }.getOrDefault(emptyList())
            SearchResults(
                channels = local.filter { it.stream.streamType == StreamType.LIVE },
                vod = local.filter { it.stream.streamType != StreamType.LIVE },
                programmes = programmes,
                tmdb = tmdb
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    fun setQuery(value: String) { _query.value = value }

    private fun time(ms: Long) = java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ms))
}

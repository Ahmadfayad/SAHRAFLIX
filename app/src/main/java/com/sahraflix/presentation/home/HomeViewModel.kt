package com.sahraflix.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentSource
import com.sahraflix.domain.repository.ContentRepository
import com.sahraflix.domain.repository.StreamingRepository
import com.sahraflix.domain.model.StreamingContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import androidx.paging.cachedIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: ContentRepository,
    private val streamingRepository: StreamingRepository
) : ViewModel() {
    val liveStreams: Flow<PagingData<CatalogEntry>> = contentRepository
        .discoverLiveTv(ContentSource.IPTV_M3U).cachedIn(viewModelScope)
    val movieStreams: Flow<PagingData<CatalogEntry>> = contentRepository
        .discoverMovies(ContentSource.IPTV_M3U).cachedIn(viewModelScope)
    val seriesStreams: Flow<PagingData<CatalogEntry>> = contentRepository
        .discoverSeries(ContentSource.IPTV_M3U).cachedIn(viewModelScope)

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

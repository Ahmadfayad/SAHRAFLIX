package com.sahraflix.presentation.catalog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.ContentDetails
import com.sahraflix.domain.repository.ContentRepository
import com.sahraflix.domain.repository.StreamingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatalogDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentRepository: ContentRepository,
    private val streamingRepository: StreamingRepository
) : ViewModel() {
    private val entry = CatalogEntry.Streaming(
        id = "streaming:${savedStateHandle.get<String>("entryId").orEmpty()}",
        title = savedStateHandle.get<String>("title").orEmpty(),
        posterUrl = null,
        tmdbId = savedStateHandle.get<String>("entryId")?.toIntOrNull() ?: 0,
        overview = null,
        releaseYear = null,
        rating = null
    )
    private val _details = MutableStateFlow<ContentDetails?>(null)
    val details: StateFlow<ContentDetails?> = _details.asStateFlow()

    init {
        viewModelScope.launch {
            _details.value = runCatching { contentRepository.getDetails(entry) }.getOrNull()
        }
    }

    fun play(season: Int? = null, episode: Int? = null, onResolved: (String) -> Unit) {
        viewModelScope.launch {
            val details = _details.value as? ContentDetails.Streaming
            val playable = details?.entry?.toStreaming(
                isSeries = !details.seasons.isEmpty()
            ) ?: return@launch
            runCatching { streamingRepository.resolveStream(playable, season, episode) }
                .onSuccess { onResolved(it.url) }
        }
    }

    private fun CatalogEntry.Streaming.toStreaming(isSeries: Boolean) = com.sahraflix.domain.model.StreamingContent(
        tmdbId = tmdbId.toString(), title = title, posterUrl = posterUrl, backdropUrl = null,
        overview = overview, releaseYear = releaseYear, rating = rating?.toFloat(),
        type = if (isSeries) com.sahraflix.domain.model.StreamType.SERIES else com.sahraflix.domain.model.StreamType.MOVIE
    )
}

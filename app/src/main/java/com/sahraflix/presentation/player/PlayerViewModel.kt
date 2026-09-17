package com.sahraflix.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.PlayUrl
import com.sahraflix.domain.repository.ContentRepository
import com.sahraflix.domain.repository.IptvVideoPlayer
import com.sahraflix.domain.model.StreamingContent
import com.sahraflix.domain.repository.StreamingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val player: IptvVideoPlayer,
    private val contentRepository: ContentRepository,
    private val streamingRepository: StreamingRepository
) : ViewModel() {
    private val _isHomeVisible = MutableStateFlow(true)
    val isHomeVisible: StateFlow<Boolean> = _isHomeVisible.asStateFlow()
    private val _isResolving = MutableStateFlow(false)
    val isResolving: StateFlow<Boolean> = _isResolving.asStateFlow()
    private val _embedUrl = MutableStateFlow<String?>(null)
    val embedUrl: StateFlow<String?> = _embedUrl.asStateFlow()
    private val _playerError = MutableStateFlow<String?>(null)
    val playerError: StateFlow<String?> = _playerError.asStateFlow()

    fun playStream(url: String) {
        _playerError.value = null
        _embedUrl.value = null
        player.playStream(url)
        _isHomeVisible.value = false
    }

    fun playCatalogEntry(entry: CatalogEntry) {
        viewModelScope.launch {
            _isResolving.value = true
            runCatching { contentRepository.resolveStream(entry) }
                .onSuccess { url ->
                    when (url) {
                        is PlayUrl.Direct -> playStream(url.url)
                        is PlayUrl.Embed -> {
                            _embedUrl.value = url.htmlUrl
                            _isHomeVisible.value = false
                        }
                    }
                }
                .onFailure { _playerError.value = it.message ?: "Unable to start playback" }
            _isResolving.value = false
        }
    }

    fun playStreamingContent(content: StreamingContent) {
        viewModelScope.launch {
            _isResolving.value = true
            runCatching { streamingRepository.resolveStream(content) }
                .onSuccess { playStream(it.url) }
                .onFailure { _playerError.value = it.message ?: "Unable to resolve stream" }
            _isResolving.value = false
        }
    }

    fun showHome() {
        _isHomeVisible.value = true
    }
}

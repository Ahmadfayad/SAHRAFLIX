package com.sahraflix.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
    private var bufferingJob: Job? = null
    private var currentRawUrl: String? = null

    fun playStream(url: String, isLive: Boolean = false) {
        _playerError.value = null
        currentRawUrl = url
        bufferingJob?.cancel()
        bufferingJob = viewModelScope.launch {
            delay(10_000)
            if (player.playerState.value == com.sahraflix.domain.repository.PlayerState.BUFFERING) {
                playExternally(url)
            }
        }
        ContextCompat.startForegroundService(context, Intent(context, com.sahraflix.player.PlaybackService::class.java))
        _embedUrl.value = null
        player.playStream(url, isLive)
        _isHomeVisible.value = false
    }

    fun playExternally(url: String = currentRawUrl.orEmpty()) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.parse(url), "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val packages = listOf("org.videolan.vlc", "com.mxtech.videoplayer.ad")
        val resolver = context.packageManager
        packages.firstOrNull { packageName ->
            intent.setPackage(packageName)
            resolver.resolveActivity(intent, 0) != null
        }?.let { packageName ->
            intent.setPackage(packageName)
            context.startActivity(intent)
        } ?: run {
            intent.setPackage(null)
            context.startActivity(Intent.createChooser(intent, "Play externally"))
        }
    }

    fun playCatalogEntry(entry: CatalogEntry) {
        viewModelScope.launch {
            _isResolving.value = true
            runCatching { contentRepository.resolveStream(entry) }
                .onSuccess { url ->
                    when (url) {
                        is PlayUrl.Direct -> playStream(url.url, entry is CatalogEntry.Iptv && entry.stream.streamType == com.sahraflix.domain.model.StreamType.LIVE)
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

    fun playStreamingContent(content: StreamingContent, season: Int? = null, episode: Int? = null) {
        viewModelScope.launch {
            _isResolving.value = true
            runCatching { streamingRepository.resolveStream(content, season, episode) }
                .onSuccess { playStream(it.url) }
                .onFailure { _playerError.value = it.message ?: "Unable to resolve stream" }
            _isResolving.value = false
        }
    }

    fun showHome() {
        _isHomeVisible.value = true
    }
}

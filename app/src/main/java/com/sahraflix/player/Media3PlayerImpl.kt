package com.sahraflix.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.sahraflix.domain.model.TrackInfo
import com.sahraflix.domain.repository.IptvVideoPlayer
import com.sahraflix.domain.repository.PlayerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Media3PlayerImpl @Inject constructor(
    @ApplicationContext context: Context
) : IptvVideoPlayer {
    private val trackSelector = DefaultTrackSelector(context)
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            500,
            15_000,
            500,
            500
        )
        .build()
    override val player: Player = ExoPlayer.Builder(context)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionJob: Job? = null
    private val audioOverrides = mutableMapOf<String, TrackSelectionOverride>()
    private val subtitleOverrides = mutableMapOf<String, TrackSelectionOverride>()

    private val _playerState = MutableStateFlow(PlayerState.IDLE)
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    override val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                _playerState.value = when (playbackState) {
                    androidx.media3.common.Player.STATE_BUFFERING -> PlayerState.BUFFERING
                    androidx.media3.common.Player.STATE_READY -> {
                        if (player.isPlaying) PlayerState.PLAYING else PlayerState.IDLE
                    }
                    androidx.media3.common.Player.STATE_IDLE -> PlayerState.IDLE
                    else -> _playerState.value
                }
                publishPosition()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) _playerState.value = PlayerState.PLAYING
                publishPosition()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _playerState.value = PlayerState.ERROR
                publishPosition()
            }
        })
        positionJob = scope.launch {
            while (isActive) {
                publishPosition()
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    override fun playStream(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    override fun loadExternalSubtitle(videoUrl: String, subtitleUri: Uri) {
        val subtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("en")
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(videoUrl)
            .setSubtitleConfigurations(listOf(subtitle))
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun getAvailableAudioTracks(): List<TrackInfo> =
        getTracksOfType(C.TRACK_TYPE_AUDIO)

    override fun getAvailableSubtitleTracks(): List<TrackInfo> =
        getTracksOfType(C.TRACK_TYPE_TEXT)

    override fun setAudioTrack(id: String) {
        setTrack(id, C.TRACK_TYPE_AUDIO, audioOverrides)
    }

    override fun setSubtitleTrack(id: String) {
        setTrack(id, C.TRACK_TYPE_TEXT, subtitleOverrides)
    }

    override fun release() {
        positionJob?.cancel()
        scope.cancel()
        player.release()
    }

    private fun getTracksOfType(trackType: Int): List<TrackInfo> =
        player.currentTracks.groups
            .filter { it.type == trackType }
            .flatMap { group ->
                (0 until group.length)
                    .filter(group::isTrackSupported)
                    .map { trackIndex ->
                        val format = group.getTrackFormat(trackIndex)
                        TrackInfo(
                            id = trackId(trackType, group, trackIndex),
                            name = format.trackName(),
                            language = format.language
                        )
                    }
            }

    private fun setTrack(
        id: String,
        trackType: Int,
        overrides: MutableMap<String, TrackSelectionOverride>
    ) {
        val override = findOverride(id, trackType) ?: return
        val parameters = trackSelector.parameters.buildUpon()
            .clearOverridesOfType(trackType)
            .setOverrideForType(override)
            .build()
        trackSelector.parameters = parameters
        overrides[id] = override
    }

    private fun findOverride(id: String, trackType: Int): TrackSelectionOverride? =
        player.currentTracks.groups
            .filter { it.type == trackType }
            .firstNotNullOfOrNull { group ->
                (0 until group.length)
                    .firstOrNull { trackId(trackType, group, it) == id }
                    ?.let { trackIndex ->
                        TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex))
                    }
            }

    private fun trackId(trackType: Int, group: Tracks.Group, trackIndex: Int): String =
        "$trackType:${group.mediaTrackGroup.id}:$trackIndex"

    private fun Format.trackName(): String =
        label?.takeIf { it.isNotBlank() }
            ?: language?.takeIf { it.isNotBlank() }
            ?: codecs?.takeIf { it.isNotBlank() }
            ?: "Track"

    private fun publishPosition() {
        _currentPosition.value = player.currentPosition.coerceAtLeast(0L)
        _duration.value = player.duration.takeUnless { it == androidx.media3.common.C.TIME_UNSET }
            ?.coerceAtLeast(0L)
            ?: 0L
    }

    private companion object {
        const val POSITION_UPDATE_INTERVAL_MS = 250L
    }
}

package com.sahraflix.domain.repository

import android.net.Uri
import androidx.media3.common.Player
import com.sahraflix.domain.model.PlayRequest
import com.sahraflix.domain.model.TrackInfo
import kotlinx.coroutines.flow.StateFlow

interface IptvVideoPlayer {
    val player: Player
    val playerState: StateFlow<PlayerState>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>
    val lastError: StateFlow<PlaybackFailure?>
    val current: StateFlow<PlayRequest?>

    fun play(request: PlayRequest)
    fun retry()
    fun stop()
    fun loadExternalSubtitle(subtitleUri: Uri, mimeType: String, language: String?)
    fun getAvailableAudioTracks(): List<TrackInfo>
    fun getAvailableSubtitleTracks(): List<TrackInfo>
    fun getAvailableVideoTracks(): List<TrackInfo>
    fun setAudioTrack(id: String)
    fun setSubtitleTrack(id: String?)
    fun setVideoTrack(id: String?)
    fun setPlaybackSpeed(speed: Float)
    fun release()
}

enum class PlayerState { IDLE, BUFFERING, READY, PLAYING, PAUSED, ENDED, ERROR }

/** User-facing classification of playback errors. */
data class PlaybackFailure(
    val kind: Kind,
    val message: String,
    val technical: String?,
    val retryable: Boolean
) {
    enum class Kind { NETWORK, HTTP_AUTH, HTTP_NOT_FOUND, HTTP_SERVER, UNSUPPORTED_FORMAT, DECODER, DRM, UNKNOWN }
}

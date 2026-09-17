package com.sahraflix.domain.repository

import android.net.Uri
import androidx.media3.common.Player
import com.sahraflix.domain.model.TrackInfo
import com.sahraflix.domain.model.DrmConfig
import kotlinx.coroutines.flow.StateFlow

interface IptvVideoPlayer {
    val player: Player
    val playerState: StateFlow<PlayerState>
    val currentPosition: StateFlow<Long>
    val duration: StateFlow<Long>

    fun playStream(url: String, isLive: Boolean = false, drm: DrmConfig? = null)
    fun loadExternalSubtitle(videoUrl: String, subtitleUri: Uri)
    fun getAvailableAudioTracks(): List<TrackInfo>
    fun getAvailableSubtitleTracks(): List<TrackInfo>
    fun setAudioTrack(id: String)
    fun setSubtitleTrack(id: String)
    fun release()
}

enum class PlayerState {
    IDLE,
    BUFFERING,
    PLAYING,
    ERROR
}

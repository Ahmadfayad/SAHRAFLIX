package com.sahraflix.player

import androidx.media3.common.Timeline
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.ExoTrackSelection
import androidx.media3.exoplayer.upstream.Allocator

class DynamicLoadControl : LoadControl {
    private val live = DefaultLoadControl.Builder()
        .setBufferDurationsMs(15_000, 30_000, 1_000, 5_000)
        .build()
    private val vod = DefaultLoadControl.Builder()
        .setBufferDurationsMs(2_500, 5_000, 500, 2_500)
        .build()
    @Volatile var isLive: Boolean = false

    private fun delegate(): DefaultLoadControl = if (isLive) live else vod

    override fun onPrepared(playerId: PlayerId) = delegate().onPrepared(playerId)
    override fun onTracksSelected(parameters: LoadControl.Parameters, groups: TrackGroupArray, selections: Array<out ExoTrackSelection?>) =
        delegate().onTracksSelected(parameters, groups, selections)
    override fun onStopped(playerId: PlayerId) = delegate().onStopped(playerId)
    override fun onReleased(playerId: PlayerId) = delegate().onReleased(playerId)
    override fun getAllocator(playerId: PlayerId): Allocator = delegate().getAllocator(playerId)
    override fun getBackBufferDurationUs(playerId: PlayerId): Long = delegate().getBackBufferDurationUs(playerId)
    override fun retainBackBufferFromKeyframe(playerId: PlayerId): Boolean = delegate().retainBackBufferFromKeyframe(playerId)
    override fun shouldContinueLoading(parameters: LoadControl.Parameters): Boolean = delegate().shouldContinueLoading(parameters)
    override fun shouldStartPlayback(parameters: LoadControl.Parameters): Boolean = delegate().shouldStartPlayback(parameters)
    override fun shouldContinuePreloading(playerId: PlayerId, timeline: Timeline, mediaPeriodId: MediaSource.MediaPeriodId, playbackPositionUs: Long): Boolean =
        delegate().shouldContinuePreloading(playerId, timeline, mediaPeriodId, playbackPositionUs)
}

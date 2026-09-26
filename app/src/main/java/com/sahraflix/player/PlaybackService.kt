package com.sahraflix.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sahraflix.domain.repository.IptvVideoPlayer
import com.sahraflix.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Exposes the shared player to the system (media notification, remote/voice controls, Google TV
 * "now playing"). Media3 promotes the service to foreground itself while playback is active, so it
 * must NOT be started with startForegroundService (that crashed when nothing was playing yet).
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var iptvPlayer: IptvVideoPlayer
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, iptvPlayer.player).setSessionActivity(open).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Stop when the app is swiped away and nothing is playing.
        if (!iptvPlayer.player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.release() // the player is app-scoped and outlives the service
        mediaSession = null
        super.onDestroy()
    }
}

package com.sahraflix.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sahraflix.domain.repository.IptvVideoPlayer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackActionReceiver : BroadcastReceiver() {
    @Inject lateinit var player: IptvVideoPlayer

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> if (player.player.isPlaying) player.player.pause() else player.player.play()
            ACTION_CLOSE -> player.player.stop()
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.sahraflix.action.PLAY_PAUSE"
        const val ACTION_CLOSE = "com.sahraflix.action.CLOSE"
    }
}

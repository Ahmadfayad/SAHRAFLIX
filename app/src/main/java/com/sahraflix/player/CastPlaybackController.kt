package com.sahraflix.player

import android.content.Context
import androidx.media3.cast.CastPlayer
import androidx.media3.common.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CastPlaybackController @Inject constructor(
    @ApplicationContext context: Context
) {
    private val castPlayer: CastPlayer = CastPlayer.Builder(context).build()

    fun play(url: String) {
        castPlayer.setMediaItem(MediaItem.fromUri(url))
        castPlayer.prepare()
        castPlayer.play()
    }

    fun release() = castPlayer.release()
}

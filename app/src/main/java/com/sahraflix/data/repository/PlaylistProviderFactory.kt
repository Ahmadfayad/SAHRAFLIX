package com.sahraflix.data.repository

import com.sahraflix.data.local.entity.PlaylistType
import com.sahraflix.domain.repository.PlaylistProvider
import javax.inject.Inject

class PlaylistProviderFactory @Inject constructor(
    private val xtreamProvider: XtreamProviderImpl,
    private val m3uProvider: M3uProviderImpl,
    private val stalkerProvider: StalkerProviderImpl
) {
    fun forType(type: PlaylistType): PlaylistProvider = when (type) {
        PlaylistType.XTREAM -> xtreamProvider
        PlaylistType.M3U -> m3uProvider
        PlaylistType.STALKER -> stalkerProvider
    }
}

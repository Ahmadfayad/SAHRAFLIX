package com.sahraflix.domain.repository

import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.PlayUrl
import com.sahraflix.domain.model.StreamType

interface CatalogPlaybackResolver {
    fun resolve(entry: CatalogEntry, season: Int? = null, episode: Int? = null): PlayUrl
}

fun CatalogEntry.Iptv.asDirectPlayUrl(): PlayUrl.Direct = PlayUrl.Direct(stream.streamUrl)

fun CatalogEntry.Iptv.supportsLivePlayback(): Boolean = stream.streamType == StreamType.LIVE

package com.sahraflix.domain.model

enum class ContentSource {
    IPTV_XTREAM,
    IPTV_M3U,
    IPTV_STALKER,
    /** Metadata-only catalogue from TMDB. Playback comes from the user's own library or a licensed service. */
    TMDB;

    val isIptv: Boolean get() = this != TMDB
}

package com.sahraflix.core

object AppConfig {
    const val DEFAULT_USER_AGENT = "SahraFlix/2.0 (Linux; Android) Media3"
    const val CONNECT_TIMEOUT_S = 15L
    const val READ_TIMEOUT_S = 60L   // large M3U / XMLTV downloads stream slowly on some panels

    const val TMDB_IMAGE_POSTER = "https://image.tmdb.org/t/p/w342"
    const val TMDB_IMAGE_BACKDROP = "https://image.tmdb.org/t/p/w1280"
    const val TMDB_IMAGE_LOGO = "https://image.tmdb.org/t/p/w92"

    /** Keep EPG this far into the past (for catch-up). */
    const val EPG_KEEP_PAST_MS = 3L * 24 * 60 * 60 * 1000
}

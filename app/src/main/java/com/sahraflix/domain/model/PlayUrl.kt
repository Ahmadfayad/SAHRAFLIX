package com.sahraflix.domain.model

/** Everything the player needs to start a stream. */
data class PlayRequest(
    val url: String,
    val title: String,
    val contentId: String,
    val isLive: Boolean,
    val posterUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val resumePositionMs: Long = 0L,
    val drm: DrmConfig? = null
)

package com.sahraflix.domain.model

data class StreamItem(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val streamType: StreamType,
    val categoryId: String,
    val playlistId: String,
    /** Provider-side id (Xtream stream/series id, Stalker channel id). */
    val providerId: String? = null,
    val catchupType: String? = null,
    val catchupSource: String? = null,
    val epgChannelId: String? = null
)

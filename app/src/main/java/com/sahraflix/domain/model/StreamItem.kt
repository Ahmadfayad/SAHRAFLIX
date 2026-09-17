package com.sahraflix.domain.model

data class StreamItem(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val streamType: StreamType,
    val categoryId: String,
    val playlistId: String,
    val previewUrl: String? = null,
    val catchupType: String? = null,
    val catchupSource: String? = null
)

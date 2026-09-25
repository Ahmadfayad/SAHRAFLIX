package com.sahraflix.domain.model

enum class DrmScheme { WIDEVINE, PLAYREADY, CLEARKEY }

data class DrmConfig(
    val scheme: DrmScheme,
    val licenseUrl: String,
    val licenseHeaders: Map<String, String> = emptyMap()
)

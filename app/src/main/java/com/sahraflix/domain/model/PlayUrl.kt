package com.sahraflix.domain.model

sealed interface PlayUrl {
    data class Direct(val url: String) : PlayUrl
    data class Embed(val htmlUrl: String) : PlayUrl
}

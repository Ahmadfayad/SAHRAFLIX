package com.sahraflix.domain.repository

interface PlaylistProvider {
    suspend fun syncCategories(playlistId: String)

    suspend fun syncStreams(playlistId: String)

    suspend fun syncEpg(playlistId: String)
}

package com.sahraflix.data.repository

import com.sahraflix.data.local.dao.CategoryDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.entity.PlaylistEntity
import com.sahraflix.data.local.entity.PlaylistType
import com.sahraflix.data.local.entity.SyncState
import com.sahraflix.domain.repository.PlaylistProvider
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/** Runs a full playlist sync: streams (atomic stale cleanup) then EPG (best effort). */
@Singleton
class PlaylistSyncer @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val categoryDao: CategoryDao,
    private val streamDao: StreamDao,
    private val m3u: M3uProviderImpl,
    private val xtream: XtreamProviderImpl,
    private val stalker: StalkerProviderImpl,
    private val epg: XmltvEpgSynchronizer
) {
    fun provider(type: PlaylistType): PlaylistProvider = when (type) {
        PlaylistType.M3U -> m3u
        PlaylistType.XTREAM -> xtream
        PlaylistType.STALKER -> stalker
    }

    /** @return true on success. Failure details are stored on the playlist row for the UI. */
    suspend fun sync(playlistId: String): Boolean {
        val playlist = playlistDao.getById(playlistId) ?: return false
        playlistDao.setSyncState(playlist.id, SyncState.RUNNING, "Downloading channels…")
        val writer = SyncWriter(playlist.id, System.currentTimeMillis(), categoryDao, streamDao)
        return try {
            val provider = provider(playlist.type)
            provider.sync(playlist, writer)
            writer.finish()
            val count = streamDao.countForPlaylist(playlist.id)
            playlistDao.markSynced(playlist.id, count, "Updating TV guide…", System.currentTimeMillis())
            val epgMessage = syncEpg(playlistDao.getById(playlist.id) ?: playlist, provider)
            playlistDao.markSynced(playlist.id, count, epgMessage, System.currentTimeMillis())
            true
        } catch (e: CancellationException) {
            playlistDao.setSyncState(playlist.id, SyncState.FAILED, "Sync was interrupted")
            throw e
        } catch (e: Throwable) {
            playlistDao.setSyncState(playlist.id, SyncState.FAILED, describe(e))
            false
        }
    }

    private suspend fun syncEpg(playlist: PlaylistEntity, provider: PlaylistProvider): String {
        val source = playlist.epgUrl?.takeIf { it.isNotBlank() } ?: provider.defaultEpgUrl(playlist)
            ?: return "No TV guide source"
        return try {
            val n = epg.sync(playlist, source)
            if (n > 0) "TV guide: $n programmes" else "TV guide had no matching channels"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            "Channels OK · guide failed: ${describe(e)}"
        }
    }

    companion object {
        fun describe(e: Throwable): String = when (e) {
            is UnknownHostException -> "Server address not found. Check the URL."
            is SocketTimeoutException -> "The server took too long to respond."
            is java.net.ConnectException -> "Could not connect to the server."
            is javax.net.ssl.SSLException -> "Secure connection failed (${e.message})."
            is IOException, is IllegalStateException -> e.message ?: e.javaClass.simpleName
            else -> e.message ?: e.javaClass.simpleName
        }
    }
}

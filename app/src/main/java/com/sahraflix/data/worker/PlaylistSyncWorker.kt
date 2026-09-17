package com.sahraflix.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sahraflix.data.local.dao.CategoryDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.repository.PlaylistProviderFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class PlaylistSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playlistDao: PlaylistDao,
    private val categoryDao: CategoryDao,
    private val streamDao: StreamDao,
    private val providerFactory: PlaylistProviderFactory
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val playlistId = inputData.getString(KEY_PLAYLIST_ID) ?: return Result.failure()
        return try {
            val playlist = playlistDao.getById(playlistId) ?: return Result.failure()
            val provider = providerFactory.forType(playlist.type)
            provider.syncCategories(playlistId)
            provider.syncStreams(playlistId)
            provider.syncEpg(playlistId)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.retry()
        }
    }

    private companion object {
        const val KEY_PLAYLIST_ID = "playlist_id"
    }
}

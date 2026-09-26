package com.sahraflix.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.repository.PlaylistSyncer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/** Syncs one playlist (KEY_PLAYLIST_ID) or, when absent, every playlist. */
@HiltWorker
class PlaylistSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val playlistDao: PlaylistDao,
    private val syncer: PlaylistSyncer
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ids = inputData.getString(KEY_PLAYLIST_ID)?.let(::listOf) ?: playlistDao.getAll().map { it.id }
        var allOk = true
        for (id in ids) allOk = syncer.sync(id) && allOk
        // Retry transient failures a few times; the error is already stored for the UI.
        return if (allOk || runAttemptCount >= 2) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_PLAYLIST_ID = "playlist_id"
        private const val PERIODIC = "playlist_sync_periodic"
        private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        fun syncNow(context: Context, playlistId: String) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "playlist_sync_$playlistId",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PlaylistSyncWorker>()
                    .setInputData(workDataOf(KEY_PLAYLIST_ID to playlistId))
                    .setConstraints(network)
                    .build()
            )
        }

        fun schedulePeriodic(context: Context, hours: Long, enabled: Boolean) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) { wm.cancelUniqueWork(PERIODIC); return }
            wm.enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<PlaylistSyncWorker>(hours, TimeUnit.HOURS)
                    .setInitialDelay(hours, TimeUnit.HOURS)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
                    .build()
            )
        }
    }
}

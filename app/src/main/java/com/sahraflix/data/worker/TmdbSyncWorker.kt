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
import com.sahraflix.data.remote.TmdbNotConfiguredException
import com.sahraflix.data.repository.UnifiedContentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import java.util.concurrent.TimeUnit

@HiltWorker
class TmdbSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: UnifiedContentRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = try {
        repository.syncTrending(pages = 3)
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: TmdbNotConfiguredException) {
        Result.success() // nothing to do without a key; don't retry forever
    } catch (e: Throwable) {
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }

    companion object {
        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context)
            val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            wm.enqueueUniquePeriodicWork(
                "tmdb_catalog_sync", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<TmdbSyncWorker>(24, TimeUnit.HOURS).setConstraints(net).build()
            )
            wm.enqueueUniqueWork(
                "tmdb_catalog_initial_sync", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<TmdbSyncWorker>().setConstraints(net).build()
            )
        }
    }
}

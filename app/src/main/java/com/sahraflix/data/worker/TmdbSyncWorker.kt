package com.sahraflix.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sahraflix.data.repository.TmdbContentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class TmdbSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: TmdbContentRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        repository.syncDiscoverCatalog(maxPages = 5)
        Result.success()
    } catch (error: CancellationException) {
        throw error
    } catch (error: IllegalStateException) {
        Result.failure()
    } catch (error: Throwable) {
        Result.retry()
    }
}

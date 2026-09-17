package com.sahraflix

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sahraflix.data.worker.TmdbSyncScheduler
import com.sahraflix.data.remote.LocalHlsProxy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var localHlsProxy: LocalHlsProxy

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        TmdbSyncScheduler.schedule(this)
        localHlsProxy.startIfNeeded()
        if (BuildConfig.DEBUG) {
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                Log.e("Sahraflix", "Unhandled error on ${thread.name}", throwable)
            }
        }
    }
}

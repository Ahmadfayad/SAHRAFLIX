package com.sahraflix

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.sahraflix.data.local.secureUserPreferences
import com.sahraflix.data.worker.PlaylistSyncWorker
import com.sahraflix.data.worker.TmdbSyncWorker
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class BaseApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var okHttpClient: OkHttpClient

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        TmdbSyncWorker.schedule(this)
        val prefs = secureUserPreferences()
        PlaylistSyncWorker.schedulePeriodic(
            this,
            hours = prefs.getString("update_interval_hours")?.toLongOrNull() ?: 12L,
            enabled = prefs.getBoolean("background_refresh_enabled", true)
        )
    }

    /** Channel logos: thousands of small images — generous disk cache, shared HTTP stack. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(okHttpClient)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizeBytes(256L * 1024 * 1024).build() }
        .crossfade(true)
        .respectCacheHeaders(false) // many logo hosts send no-cache
        .build()
}

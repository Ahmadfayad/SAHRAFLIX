package com.sahraflix.data.repository

import com.sahraflix.data.local.dao.CategoryDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.entity.CategoryEntity
import com.sahraflix.data.local.entity.StreamItemEntity

/**
 * Batches category/stream writes for one sync run and stamps each row with the run's generation,
 * so rows that no longer exist upstream can be removed afterwards ([finish]).
 */
class SyncWriter(
    val playlistId: String,
    val stamp: Long,
    private val categoryDao: CategoryDao,
    private val streamDao: StreamDao
) {
    private val categories = LinkedHashMap<String, CategoryEntity>()
    private val streams = ArrayList<StreamItemEntity>(BATCH)
    private var categoryOrder = 0
    var count = 0
        private set

    fun category(entity: CategoryEntity) {
        if (!categories.containsKey(entity.id)) {
            categories[entity.id] = entity.copy(sortOrder = categoryOrder++, syncStamp = stamp)
        }
    }

    suspend fun stream(entity: StreamItemEntity) {
        streams += entity.copy(syncStamp = stamp)
        count++
        if (streams.size >= BATCH) flushStreams()
    }

    private suspend fun flushStreams() {
        if (categories.isNotEmpty()) categoryDao.upsertAll(categories.values.toList()).also { categoriesWritten += categories.keys; categories.clear() }
        if (streams.isNotEmpty()) { streamDao.upsertStreams(streams.toList()); streams.clear() }
    }

    private val categoriesWritten = HashSet<String>()

    /** Flushes remaining rows and deletes anything not seen in this run. */
    suspend fun finish() {
        flushStreams()
        if (count == 0) throw IllegalStateException("The provider returned no channels or videos")
        streamDao.deleteStale(playlistId, stamp)
        categoryDao.deleteStale(playlistId, stamp)
    }

    private companion object { const val BATCH = 500 }
}

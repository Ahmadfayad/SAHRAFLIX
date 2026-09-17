package com.sahraflix.data.repository

import android.content.Context
import android.net.Uri
import com.sahraflix.data.local.dao.CategoryDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.entity.CategoryEntity
import com.sahraflix.data.local.entity.PlaylistType
import com.sahraflix.data.local.entity.StreamItemEntity
import com.sahraflix.domain.model.StreamType
import com.sahraflix.domain.repository.PlaylistProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject

class M3uProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val playlistDao: PlaylistDao,
    private val categoryDao: CategoryDao,
    private val streamDao: StreamDao,
    private val epgSynchronizer: XmltvEpgSynchronizer
) : PlaylistProvider {
    override suspend fun syncCategories(playlistId: String) {
        // M3U categories are discovered while stream records are streamed.
    }

    override suspend fun syncStreams(playlistId: String) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist not found: $playlistId")
        require(playlist.type == PlaylistType.M3U) { "Playlist is not M3U: $playlistId" }

        val collector = BatchCollector(categoryDao, streamDao)
        openPlaylistInput(playlist.url).use { input ->
            parseRecords(input, playlistId).collect { record ->
                collector.collect(record)
            }
        }
        collector.flush()
    }

    override suspend fun syncEpg(playlistId: String) {
        epgSynchronizer.sync(playlistId)
    }

    private fun openPlaylistInput(source: String): InputStream {
        val uri = Uri.parse(source)
        return if (uri.scheme == "content") {
            context.contentResolver.openInputStream(uri)
                ?: error("Unable to open playlist URI: $source")
        } else {
            val request = Request.Builder().url(source).get().build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                response.close()
                error("M3U request failed: ${response.code}")
            }
            response.body?.byteStream() ?: run {
                response.close()
                error("M3U response was empty")
            }
        }
    }

    private fun parseRecords(input: InputStream, playlistId: String): Flow<M3uRecord> = flow {
        input.bufferedReader().useLines { lines ->
            var metadata: M3uMetadata? = null
            for (rawLine in lines) {
                val line = rawLine.trim()
                when {
                    line.startsWith("#EXTM3U") -> line.attribute("url-tvg")?.let { epgUrl ->
                        playlistDao.updateEpgUrl(playlistId, epgUrl)
                    }
                    line.startsWith("#EXTINF:") -> metadata = parseMetadata(line)
                    line.isNotEmpty() && !line.startsWith("#") && metadata != null -> {
                        val current = metadata ?: continue
                        val categoryKey = current.categoryName.ifBlank { UNCATEGORIZED }
                        val categoryId = "$playlistId:m3u:$categoryKey"
                        emit(
                            M3uRecord(
                                category = CategoryEntity(
                                    id = categoryId,
                                    name = categoryKey,
                                    playlistId = playlistId,
                                    streamType = current.streamType
                                ),
                                stream = StreamItemEntity(
                                    id = "$playlistId:m3u:$line:${current.name}",
                                    name = current.name,
                                    streamUrl = line,
                                    logoUrl = current.logoUrl,
                                    streamType = current.streamType,
                                    categoryId = categoryId,
                                    playlistId = playlistId,
                                    epgChannelId = current.epgChannelId,
                                    catchupType = current.catchupType,
                                    catchupSource = current.catchupSource
                                )
                            )
                        )
                        metadata = null
                    }
                }
            }
        }
    }

    private fun parseMetadata(line: String): M3uMetadata {
        val title = line.substringAfterLast(',').trim().ifBlank { "Unnamed stream" }
        return M3uMetadata(
            name = title,
            logoUrl = line.attribute("tvg-logo"),
            categoryName = buildCategoryName(
                line.attribute("group-title") ?: UNCATEGORIZED,
                line.attribute("tvg-shift")
            ),
            epgChannelId = line.attribute("tvg-id"),
            streamType = detectStreamType(title, line),
            catchupType = line.attribute("catchup"),
            catchupSource = line.attribute("catchup-source")
        )
    }

    private fun buildCategoryName(group: String, shift: String?): String =
        shift?.takeIf { it.isNotBlank() }?.let { "$group (UTC$it)" } ?: group

    private fun detectStreamType(name: String, metadata: String): StreamType {
        val value = "$name $metadata".lowercase()
        return when {
            Regex("s\\d{1,2}e\\d{1,2}").containsMatchIn(value) ||
                "/series/" in value || "series/" in value -> StreamType.SERIES
            ".mp4" in value || ".mkv" in value || "/movie/" in value || "movie/" in value -> StreamType.MOVIE
            else -> StreamType.LIVE
        }
    }

    private fun String.attribute(name: String): String? {
        val prefix = "$name=\""
        val start = indexOf(prefix)
        if (start < 0) return null
        val valueStart = start + prefix.length
        val valueEnd = indexOf('"', valueStart)
        return if (valueEnd < 0) null else substring(valueStart, valueEnd)
    }

    private class BatchCollector(
        private val categoryDao: CategoryDao,
        private val streamDao: StreamDao
    ) {
        private val categories = ArrayList<CategoryEntity>(BATCH_SIZE)
        private val streams = ArrayList<StreamItemEntity>(BATCH_SIZE)
        private val categoryIds = HashSet<String>(BATCH_SIZE)

        suspend fun collect(record: M3uRecord) {
            if (categoryIds.add(record.category.id)) categories += record.category
            streams += record.stream
            if (streams.size == BATCH_SIZE) flush()
        }

        suspend fun flush() {
            if (streams.isEmpty()) return
            categoryDao.insertAll(categories)
            streamDao.insertStreams(streams)
            categories.clear()
            streams.clear()
        }
    }

    private data class M3uMetadata(
        val name: String,
        val logoUrl: String?,
        val categoryName: String,
        val epgChannelId: String?,
        val streamType: StreamType,
        val catchupType: String?,
        val catchupSource: String?
    )

    private data class M3uRecord(
        val category: CategoryEntity,
        val stream: StreamItemEntity
    )

    private companion object {
        const val BATCH_SIZE = 500
        const val UNCATEGORIZED = "Uncategorized"
    }
}

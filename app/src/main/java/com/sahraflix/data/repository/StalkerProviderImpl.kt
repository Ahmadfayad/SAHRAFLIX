package com.sahraflix.data.repository

import android.util.JsonReader
import android.util.JsonToken
import com.sahraflix.data.local.dao.CategoryDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.entity.CategoryEntity
import com.sahraflix.data.local.entity.PlaylistType
import com.sahraflix.data.local.entity.StreamItemEntity
import com.sahraflix.domain.model.StreamType
import com.sahraflix.domain.repository.PlaylistProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class StalkerProviderImpl @Inject constructor(
    private val httpClient: OkHttpClient,
    private val playlistDao: PlaylistDao,
    private val categoryDao: CategoryDao,
    private val streamDao: StreamDao,
    private val xmltvEpgSynchronizer: XmltvEpgSynchronizer
) : PlaylistProvider {
    override suspend fun syncCategories(playlistId: String) {
        syncType(playlistId, StreamType.LIVE, "get_all_channels")
    }

    override suspend fun syncStreams(playlistId: String) = withContext(Dispatchers.IO) {
        syncType(playlistId, StreamType.LIVE, "get_all_channels")
        syncType(playlistId, StreamType.MOVIE, "get_ordered_list", "vod")
        syncType(playlistId, StreamType.SERIES, "get_ordered_list", "series")
    }

    override suspend fun syncEpg(playlistId: String) {
        xmltvEpgSynchronizer.sync(playlistId)
    }

    private suspend fun syncType(
        playlistId: String,
        streamType: StreamType,
        action: String,
        type: String? = null
    ) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist not found: $playlistId")
        require(playlist.type == PlaylistType.STALKER) { "Playlist is not Stalker: $playlistId" }
        val request = buildRequest(playlist.url, playlist.username.orEmpty(), action, type)

        httpClient.newCall(request).execute().use { response ->
            require(response.isSuccessful) { "Stalker $action failed: ${response.code}" }
            val body = response.body ?: error("Stalker response was empty")
            body.byteStream().use { input ->
                InputStreamReader(input, StandardCharsets.UTF_8).use { inputReader ->
                    JsonReader(inputReader).use { reader ->
                        val streams = ArrayList<StreamItemEntity>(BATCH_SIZE)
                        val categories = ArrayList<CategoryEntity>(BATCH_SIZE)
                        val categoryIds = HashSet<String>(BATCH_SIZE)
                        readResponse(reader, playlistId, streamType) { item ->
                            val categoryId = "$playlistId:stalker:${streamType.name}:${item.categoryId}"
                            if (categoryIds.add(categoryId)) {
                                categories += CategoryEntity(
                                    id = categoryId,
                                    name = item.categoryName,
                                    playlistId = playlistId,
                                    streamType = streamType
                                )
                            }
                            streams += StreamItemEntity(
                                id = "$playlistId:stalker:${streamType.name}:${item.id}",
                                name = item.name,
                                streamUrl = item.url,
                                logoUrl = item.logoUrl,
                                streamType = streamType,
                                categoryId = categoryId,
                                playlistId = playlistId,
                                providerId = item.id
                            )
                            if (streams.size == BATCH_SIZE) {
                                categoryDao.insertAll(categories)
                                streamDao.insertStreams(streams)
                                categories.clear()
                                streams.clear()
                            }
                        }
                        if (streams.isNotEmpty()) {
                            categoryDao.insertAll(categories)
                            streamDao.insertStreams(streams)
                        }
                    }
                }
            }
        }
    }

    private fun buildRequest(base: String, mac: String, action: String, type: String?): Request {
        val url = base.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegment("server.php")
            .addQueryParameter("type", "itv")
            .addQueryParameter("action", action)
            .apply { type?.let { addQueryParameter("p", it) } }
            .build()
        return Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C)")
            .header("Cookie", "mac=$mac")
            .header("X-User-Agent", "Model: MAG250; Link: WiFi")
            .get()
            .build()
    }

    private suspend fun readResponse(
        reader: JsonReader,
        playlistId: String,
        streamType: StreamType,
        emit: suspend (StalkerRecord) -> Unit
    ) {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return
        }
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "js", "data" -> readArray(reader, playlistId, streamType, emit)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    private suspend fun readArray(
        reader: JsonReader,
        playlistId: String,
        streamType: StreamType,
        emit: suspend (StalkerRecord) -> Unit
    ) {
        if (reader.peek() != JsonToken.BEGIN_ARRAY) {
            reader.skipValue()
            return
        }
        reader.beginArray()
        while (reader.hasNext()) {
            if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                reader.skipValue()
                continue
            }
            var id: String? = null
            var name = "Unnamed stream"
            var logo: String? = null
            var categoryId = "uncategorized"
            var categoryName = "Uncategorized"
            var cmd: String? = null
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id", "ch_id", "series_id" -> id = reader.nextNullableString()
                    "name", "title" -> name = reader.nextNullableString() ?: name
                    "logo", "logo_2" -> logo = reader.nextNullableString()
                    "tv_genre_id", "category_id" -> categoryId = reader.nextNullableString() ?: categoryId
                    "tv_genre_name", "category_name" -> categoryName = reader.nextNullableString() ?: categoryName
                    "cmd", "stream_url" -> cmd = reader.nextNullableString()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            id?.takeIf { it.isNotBlank() }?.let {
                emit(StalkerRecord(it, name, logo, categoryId, categoryName, cmd ?: it))
            }
        }
        reader.endArray()
    }

    private fun JsonReader.nextNullableString(): String? =
        if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else nextString()

    private data class StalkerRecord(
        val id: String,
        val name: String,
        val logoUrl: String?,
        val categoryId: String,
        val categoryName: String,
        val url: String
    )

    private companion object {
        const val BATCH_SIZE = 500
    }
}

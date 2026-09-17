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

class XtreamProviderImpl @Inject constructor(
    private val httpClient: OkHttpClient,
    private val playlistDao: PlaylistDao,
    private val categoryDao: CategoryDao,
    private val streamDao: StreamDao,
    private val xmltvEpgSynchronizer: XmltvEpgSynchronizer,
    private val xtreamEpgSynchronizer: XtreamEpgSynchronizer
) : PlaylistProvider {
    override suspend fun syncCategories(playlistId: String) {
        // Xtream VOD responses carry category metadata alongside each stream.
    }

    override suspend fun syncStreams(playlistId: String) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist not found: $playlistId")
        require(playlist.type == PlaylistType.XTREAM) { "Playlist is not Xtream: $playlistId" }

        listOf(
            StreamType.LIVE to "get_live_streams",
            StreamType.MOVIE to "get_vod_streams",
            StreamType.SERIES to "get_series"
        ).forEach { (type, action) ->
            val endpoint = playlist.url.trimEnd('/').toHttpUrl().newBuilder()
                .addPathSegment("player_api.php")
                .addQueryParameter("username", playlist.username.orEmpty())
                .addQueryParameter("password", playlist.password.orEmpty())
                .addQueryParameter("action", action)
                .build()
            httpClient.newCall(Request.Builder().url(endpoint).get().build()).execute().use { response ->
                require(response.isSuccessful) { "Xtream $action failed: ${response.code}" }
                val body = response.body ?: error("Xtream response was empty")
                body.byteStream().use { input ->
                    InputStreamReader(input, StandardCharsets.UTF_8).use { inputReader ->
                        JsonReader(inputReader).use { reader ->
                            if (reader.peek() != JsonToken.BEGIN_ARRAY) return@use
                            reader.beginArray()
                        val streamBatch = ArrayList<StreamItemEntity>(BATCH_SIZE)
                        val categoryBatch = ArrayList<CategoryEntity>(BATCH_SIZE)
                        val categoryIds = HashSet<String>(BATCH_SIZE)

                        while (reader.hasNext()) {
                            val parsed = readStream(
                                reader = reader,
                                playlistId = playlistId,
                                type = type,
                                baseUrl = playlist.url.trimEnd('/'),
                                username = playlist.username.orEmpty(),
                                password = playlist.password.orEmpty()
                            )
                            if (parsed == null) continue

                            if (categoryIds.add(parsed.category.id)) {
                                categoryBatch += parsed.category
                            }
                            streamBatch += parsed.stream

                            if (streamBatch.size == BATCH_SIZE) {
                                categoryDao.insertAll(categoryBatch)
                                streamDao.insertStreams(streamBatch)
                                categoryBatch.clear()
                                streamBatch.clear()
                            }
                        }
                            reader.endArray()

                            if (streamBatch.isNotEmpty()) {
                                categoryDao.insertAll(categoryBatch)
                                streamDao.insertStreams(streamBatch)
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun syncEpg(playlistId: String) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist not found: $playlistId")
        if (playlist.epgUrl.isNullOrBlank()) {
            xtreamEpgSynchronizer.sync(playlistId)
        } else {
            xmltvEpgSynchronizer.sync(playlistId)
        }
    }

    private fun readStream(
        reader: JsonReader,
        playlistId: String,
        type: StreamType,
        baseUrl: String,
        username: String,
        password: String
    ): ParsedXtreamStream? {
        var streamId: String? = null
        var name = "Unnamed stream"
        var logoUrl: String? = null
        var categoryId = UNCATEGORIZED_ID
        var categoryName = "Uncategorized"
        var directSource: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "stream_id", "vod_id", "series_id" -> streamId = reader.nextNullableString()
                "name" -> name = reader.nextNullableString() ?: name
                "stream_icon" -> logoUrl = reader.nextNullableString()
                "category_id" -> categoryId = reader.nextNullableString() ?: UNCATEGORIZED_ID
                "category_name" -> categoryName = reader.nextNullableString() ?: categoryName
                "direct_source" -> directSource = reader.nextNullableString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val id = streamId?.takeIf { it.isNotBlank() } ?: return null
        val category = CategoryEntity(
            id = "$playlistId:${type.name.lowercase()}:$categoryId",
            name = categoryName,
            playlistId = playlistId,
            streamType = type
        )
        return ParsedXtreamStream(
            category = category,
            stream = StreamItemEntity(
                id = "$playlistId:${type.name.lowercase()}:$id",
                name = name,
                streamUrl = directSource?.takeIf { it.isNotBlank() }
                    ?: buildStreamUrl(baseUrl, username, password, id, type),
                logoUrl = logoUrl,
                streamType = StreamType.MOVIE,
                categoryId = category.id,
                playlistId = playlistId,
                providerId = id
            )
        )
    }

    private fun buildStreamUrl(baseUrl: String, username: String, password: String, streamId: String, type: StreamType): String {
        return when (type) {
            StreamType.LIVE -> "$baseUrl/live/$username/$password/$streamId.ts"
            StreamType.MOVIE -> "$baseUrl/movie/$username/$password/$streamId.mp4"
            StreamType.SERIES -> "$baseUrl/series/$username/$password/$streamId.mp4"
        }
    }

    private fun JsonReader.nextNullableString(): String? =
        if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextString()
        }

    private data class ParsedXtreamStream(
        val category: CategoryEntity,
        val stream: StreamItemEntity
    )

    private companion object {
        const val BATCH_SIZE = 500
        const val UNCATEGORIZED_ID = "uncategorized"
    }
}

package com.sahraflix.data.repository

import android.util.JsonReader
import android.util.JsonToken
import androidx.paging.PagingSource
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.entity.EpgEventEntity
import com.sahraflix.data.local.entity.PlaylistType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class XtreamEpgSynchronizer @Inject constructor(
    private val httpClient: OkHttpClient,
    private val playlistDao: PlaylistDao,
    private val streamDao: StreamDao,
    private val epgDao: EpgDao
) {
    suspend fun sync(playlistId: String) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist not found: $playlistId")
        require(playlist.type == PlaylistType.XTREAM) { "Playlist is not Xtream: $playlistId" }

        val providerIds = streamDao.getProviderIdsByPlaylist(playlistId)
        var loadParams: PagingSource.LoadParams<Int> = PagingSource.LoadParams.Refresh(
            key = null,
            loadSize = PAGE_SIZE,
            placeholdersEnabled = false
        )
        val events = ArrayList<EpgEventEntity>(BATCH_SIZE)

        while (true) {
            when (val result = providerIds.load(loadParams)) {
                is PagingSource.LoadResult.Error -> throw result.throwable
                is PagingSource.LoadResult.Invalid -> return@withContext
                is PagingSource.LoadResult.Page -> {
                    result.data.forEach { providerId ->
                        val streamId = streamDao.findIdByProviderId(playlistId, providerId)
                            ?: return@forEach
                        fetchShortEpg(playlist, providerId, streamId, events)
                        if (events.size >= BATCH_SIZE) {
                            epgDao.insertEvents(events)
                            events.clear()
                        }
                    }
                    val nextKey = result.nextKey ?: break
                    loadParams = PagingSource.LoadParams.Append(
                        key = nextKey,
                        loadSize = PAGE_SIZE,
                        placeholdersEnabled = false
                    )
                }
            }
        }
        if (events.isNotEmpty()) epgDao.insertEvents(events)
    }

    private fun fetchShortEpg(
        playlist: com.sahraflix.data.local.entity.PlaylistEntity,
        providerId: String,
        streamId: String,
        events: MutableList<EpgEventEntity>
    ) {
        val endpoint = playlist.url.trimEnd('/').toHttpUrl().newBuilder()
            .addPathSegment("player_api.php")
            .addQueryParameter("username", playlist.username.orEmpty())
            .addQueryParameter("password", playlist.password.orEmpty())
            .addQueryParameter("action", "get_short_epg")
            .addQueryParameter("stream_id", providerId)
            .build()
        httpClient.newCall(Request.Builder().url(endpoint).get().build()).execute().use { response ->
            if (!response.isSuccessful || response.body == null) return
            response.body!!.byteStream().use { input ->
                InputStreamReader(input, StandardCharsets.UTF_8).use { reader ->
                    JsonReader(reader).use { json ->
                        readEpgResponse(json, streamId, events)
                    }
                }
            }
        }
    }

    private fun readEpgResponse(
        reader: JsonReader,
        streamId: String,
        events: MutableList<EpgEventEntity>
    ) {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return
        }
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "epg_listings" && reader.peek() == JsonToken.BEGIN_ARRAY) {
                reader.beginArray()
                while (reader.hasNext()) readEvent(reader, streamId, events)
                reader.endArray()
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
    }

    private fun readEvent(
        reader: JsonReader,
        streamId: String,
        events: MutableList<EpgEventEntity>
    ) {
        var title = ""
        var description: String? = null
        var startTime = 0L
        var endTime = 0L
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "title" -> title = reader.nextNullableString().orEmpty()
                "description" -> description = reader.nextNullableString()
                "start_timestamp", "start" -> startTime = reader.nextTimestamp()
                "stop_timestamp", "stop", "end_timestamp" -> endTime = reader.nextTimestamp()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if (title.isNotBlank() && endTime > startTime) {
            events += EpgEventEntity(
                streamId = streamId,
                title = title,
                description = description,
                startTime = startTime,
                endTime = endTime
            )
        }
    }

    private fun JsonReader.nextNullableString(): String? =
        if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextString()
        }

    private fun JsonReader.nextTimestamp(): Long =
        nextNullableString()?.toLongOrNull() ?: 0L

    private companion object {
        const val PAGE_SIZE = 100
        const val BATCH_SIZE = 500
    }
}

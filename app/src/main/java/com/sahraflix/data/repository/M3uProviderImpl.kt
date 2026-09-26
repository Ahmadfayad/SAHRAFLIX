package com.sahraflix.data.repository

import android.content.Context
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.entity.CategoryEntity
import com.sahraflix.data.local.entity.PlaylistEntity
import com.sahraflix.data.local.entity.StreamItemEntity
import com.sahraflix.data.parser.M3uParser
import com.sahraflix.domain.model.StreamType
import com.sahraflix.domain.repository.PlaylistProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.security.MessageDigest
import javax.inject.Inject

class M3uProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val playlistDao: PlaylistDao
) : PlaylistProvider {

    override suspend fun sync(playlist: PlaylistEntity, writer: SyncWriter) = withContext(Dispatchers.IO) {
        val parser = M3uParser()
        var epgHint: String? = null
        var order = 0
        HttpStreams.open(context, httpClient, playlist.url, playlist.userAgent).use { input ->
            input.bufferedReader().use { reader ->
                // parse() is inline, so the suspending writes below run directly inside this coroutine:
                // the playlist is streamed straight to the database without being held in memory.
                parser.parse(reader, onHeader = { epgHint = it.epgUrls.firstOrNull() }) { entry ->
                    val type = when (entry.kind) {
                        M3uParser.Kind.LIVE -> StreamType.LIVE
                        M3uParser.Kind.MOVIE -> StreamType.MOVIE
                        M3uParser.Kind.SERIES -> StreamType.SERIES
                    }
                    val groupName = entry.group?.takeIf { it.isNotBlank() } ?: UNCATEGORIZED
                    val categoryId = "${playlist.id}:${type.name}:${hash(groupName)}"
                    writer.category(CategoryEntity(categoryId, groupName, playlist.id, type))
                    writer.stream(
                        StreamItemEntity(
                            id = "${playlist.id}:${hash(entry.url + "|" + entry.title)}",
                            name = entry.title,
                            streamUrl = entry.url,
                            logoUrl = entry.logo,
                            streamType = type,
                            categoryId = categoryId,
                            playlistId = playlist.id,
                            epgChannelId = entry.tvgId ?: entry.attributes["tvg-name"],
                            catchupType = entry.catchup,
                            catchupSource = entry.catchupSource,
                            userAgent = entry.userAgent,
                            referrer = entry.referrer,
                            sortOrder = entry.attributes["tvg-chno"]?.toIntOrNull() ?: order
                        )
                    )
                    order++
                }
            }
        }
        epgHint?.let { playlistDao.setEpgUrlIfMissing(playlist.id, it) }
        Unit
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.take(10).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val UNCATEGORIZED = "Uncategorized"
    }
}

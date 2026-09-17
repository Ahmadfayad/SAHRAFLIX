package com.sahraflix.data.repository

import android.content.Context
import android.net.Uri
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.entity.EpgEventEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import javax.inject.Inject

class XmltvEpgSynchronizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val playlistDao: PlaylistDao,
    private val streamDao: StreamDao,
    private val epgDao: EpgDao
) {
    suspend fun sync(playlistId: String) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist not found: $playlistId")
        val source = playlist.epgUrl?.takeIf { it.isNotBlank() } ?: return@withContext
        openInput(source).use { input ->
            val batch = ArrayList<EpgEventEntity>(BATCH_SIZE)
            XmltvEpgParser().parse(input).collect { programme ->
                val streamId = streamDao.findIdByEpgChannel(playlistId, programme.channelId)
                    ?: return@collect
                batch += EpgEventEntity(
                    streamId = streamId,
                    title = programme.title,
                    description = programme.description,
                    startTime = programme.startTime,
                    endTime = programme.endTime
                )
                if (batch.size == BATCH_SIZE) {
                    epgDao.insertEvents(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) epgDao.insertEvents(batch)
        }
        epgDao.deletePastEvents(System.currentTimeMillis() - 86_400_000L)
    }

    private fun openInput(source: String): InputStream {
        val uri = Uri.parse(source)
        if (uri.scheme == "content") {
            return context.contentResolver.openInputStream(uri)
                ?: error("Unable to open EPG URI: $source")
        }
        val response = httpClient.newCall(Request.Builder().url(source).get().build()).execute()
        if (!response.isSuccessful) {
            response.close()
            error("EPG request failed: ${response.code}")
        }
        return response.body?.byteStream() ?: run {
            response.close()
            error("EPG response was empty")
        }
    }

    private companion object {
        const val BATCH_SIZE = 500
    }
}

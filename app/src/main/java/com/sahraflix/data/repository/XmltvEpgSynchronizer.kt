package com.sahraflix.data.repository

import android.content.Context
import android.util.Xml
import com.sahraflix.core.AppConfig
import com.sahraflix.data.local.dao.EpgDao
import com.sahraflix.data.local.dao.StreamDao
import com.sahraflix.data.local.entity.EpgProgrammeEntity
import com.sahraflix.data.local.entity.PlaylistEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject

/**
 * Streams an XMLTV guide (plain or gzipped) into the database.
 * Only programmes for channels that exist in the playlist and that are not already over are stored,
 * which keeps full-country guides (hundreds of MB uncompressed) manageable.
 */
class XmltvEpgSynchronizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val streamDao: StreamDao,
    private val epgDao: EpgDao
) {
    suspend fun sync(playlist: PlaylistEntity, source: String): Int = withContext(Dispatchers.IO) {
        val wanted = streamDao.epgChannelIds(playlist.id).toHashSet()
        if (wanted.isEmpty()) return@withContext 0
        val cutoff = System.currentTimeMillis() - AppConfig.EPG_KEEP_PAST_MS
        val batch = ArrayList<EpgProgrammeEntity>(BATCH)
        var stored = 0
        HttpStreams.open(context, httpClient, source, playlist.userAgent).use { input ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)
            var channel: String? = null
            var start = 0L
            var stop = 0L
            var title: String? = null
            var desc: String? = null
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "programme" -> {
                            val ch = parser.getAttributeValue(null, "channel")
                            channel = ch?.takeIf { it in wanted }
                            if (channel != null) {
                                start = XmltvTime.parse(parser.getAttributeValue(null, "start"))
                                stop = XmltvTime.parse(parser.getAttributeValue(null, "stop"))
                            }
                            title = null; desc = null
                        }
                        "title" -> if (channel != null && title == null) title = parser.nextText().trim()
                        "desc" -> if (channel != null && desc == null) desc = parser.nextText().trim()
                    }
                } else if (event == XmlPullParser.END_TAG && parser.name == "programme") {
                    val ch = channel
                    val t = title
                    if (ch != null && !t.isNullOrBlank() && stop > start && stop > cutoff) {
                        batch += EpgProgrammeEntity(playlist.id, ch, start, stop, t, desc?.takeIf { it.isNotBlank() })
                        if (batch.size >= BATCH) { epgDao.upsert(batch.toList()); stored += batch.size; batch.clear() }
                    }
                    channel = null
                }
                event = parser.next()
            }
        }
        if (batch.isNotEmpty()) { epgDao.upsert(batch); stored += batch.size }
        epgDao.deleteBefore(cutoff)
        stored
    }

    private companion object { const val BATCH = 1000 }
}

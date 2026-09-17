package com.sahraflix.data.repository

import android.util.Xml
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class XmltvEpgParser {
    fun parse(input: InputStream): Flow<XmltvProgramme> = flow {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)
        var currentChannel: String? = null
        var title: String? = null
        var description: String? = null
        var startTime = 0L
        var endTime = 0L
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "programme" -> {
                        currentChannel = parser.getAttributeValue(null, "channel")
                        startTime = parseXmltvTime(parser.getAttributeValue(null, "start"))
                        endTime = parseXmltvTime(parser.getAttributeValue(null, "stop"))
                        title = null
                        description = null
                    }
                    "title" -> if (currentChannel != null) title = parser.nextText().trim()
                    "desc" -> if (currentChannel != null) description = parser.nextText().trim()
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme") {
                    val channel = currentChannel
                    val name = title?.takeIf { it.isNotBlank() }
                    if (!channel.isNullOrBlank() && !name.isNullOrBlank() && endTime > startTime) {
                        emit(
                            XmltvProgramme(
                                channelId = channel,
                                title = name,
                                description = description?.takeIf { it.isNotBlank() },
                                startTime = startTime,
                                endTime = endTime
                            )
                        )
                    }
                    currentChannel = null
                }
            }
            eventType = parser.next()
        }
    }

    private fun parseXmltvTime(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val normalized = value.trim().replace(Regex("\\s+"), " ")
        val formats = if (normalized.contains(" ")) {
            listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmssZ")
        } else {
            listOf("yyyyMMddHHmmssZ", "yyyyMMddHHmmss")
        }
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(normalized)?.time
            }.getOrNull()
        } ?: 0L
    }
}

data class XmltvProgramme(
    val channelId: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long
)

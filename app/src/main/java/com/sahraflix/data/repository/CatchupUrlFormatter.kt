package com.sahraflix.data.repository

import com.sahraflix.data.local.entity.StreamItemEntity
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CatchupUrlFormatter {
    fun format(stream: StreamItemEntity, startTime: Long, endTime: Long): String? {
        val template = stream.catchupSource ?: return null
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val epochStart = (startTime / 1000L).toString()
        val epochEnd = (endTime / 1000L).toString()
        return template
            .replace("{start}", epochStart)
            .replace("{end}", epochEnd)
            .replace("{duration}", ((endTime - startTime) / 1000L).toString())
            .replace("{timestamp}", epochStart)
            .replace("{utc}", iso.format(Date(startTime)))
            .replace("{start_date}", SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startTime)))
            .replace("{start_time}", SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(startTime)))
            .let { value -> if (value.contains("{url}")) value.replace("{url}", URLEncoder.encode(stream.streamUrl, StandardCharsets.UTF_8.name())) else value }
    }
}

package com.sahraflix.data.repository

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** XMLTV timestamps: "20240101120000 +0100", "20240101120000+0100", "20240101120000" (UTC), "202401011200". */
object XmltvTime {
    private val WITH_ZONE = Regex("""^(\d{12,14})\s*([+-]\d{4})?$""")

    fun parse(value: String?): Long {
        val v = value?.trim()?.replace(Regex("\\s+"), " ") ?: return 0L
        val m = WITH_ZONE.find(v) ?: return 0L
        val digits = m.groupValues[1].padEnd(14, '0')
        val zone = m.groupValues[2].ifBlank { "+0000" }
        return runCatching {
            SimpleDateFormat("yyyyMMddHHmmssZ", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                .parse(digits + zone)?.time ?: 0L
        }.getOrDefault(0L)
    }
}

package com.sahraflix.data.repository

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Builds catch-up (timeshift) URLs. Supports the Kodi/TiviMate token set used by M3U playlists
 * ({utc}, {start}, {lutc}, {duration}, {offset}, {Y}{m}{d}{H}{M}{S}, ${start}, {utc:FORMAT} …)
 * plus the Xtream timeshift form produced by [XtreamProviderImpl].
 * Pure JVM code (no Android APIs) so it is unit-tested directly.
 */
object CatchupUrlFormatter {
    const val TYPE_XTREAM = "xtream"

    /**
     * @param template catch-up source template; for catchup="append"/"shift" types it is appended
     *                 to (or derived from) [streamUrl].
     */
    fun format(
        template: String?,
        catchupType: String?,
        streamUrl: String,
        startMs: Long,
        endMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): String? {
        val type = catchupType?.lowercase()
        val source = when {
            type == "shift" -> streamUrl + (if ('?' in streamUrl) "&" else "?") + "utc={utc}&lutc={lutc}"
            type == "append" && template != null -> streamUrl + template
            template.isNullOrBlank() -> return null
            else -> template
        }
        val startS = startMs / 1000
        val endS = endMs / 1000
        val durationS = ((endMs - startMs) / 1000).coerceAtLeast(60)
        val offsetS = ((nowMs - startMs) / 1000).coerceAtLeast(0)
        var out = source

        // {utc:FORMAT} / {start:FORMAT} style custom formats (strftime-like letters).
        out = CUSTOM.replace(out) { m ->
            val which = m.groupValues[1]
            val fmt = m.groupValues[2]
            val ts = if (which == "end" || which == "utcend") endMs else startMs
            strftime(fmt, ts, TimeZone.getTimeZone("UTC"))
        }
        // Xtream: {xtream_start|Europe/London} → YYYY-MM-DD:HH-MM in the server's timezone.
        out = XTREAM_START.replace(out) { m ->
            val tz = TimeZone.getTimeZone(m.groupValues[1].ifBlank { "UTC" })
            SimpleDateFormat("yyyy-MM-dd:HH-mm", Locale.US).apply { timeZone = tz }.format(Date(startMs))
        }
        val utc = TimeZone.getTimeZone("UTC")
        val replacements = linkedMapOf(
            "{utc}" to startS.toString(),
            "{start}" to startS.toString(),
            "\${start}" to startS.toString(),
            "{timestamp}" to startS.toString(),
            "{utcend}" to endS.toString(),
            "{end}" to endS.toString(),
            "\${end}" to endS.toString(),
            "{lutc}" to (nowMs / 1000).toString(),
            "{now}" to (nowMs / 1000).toString(),
            "{duration}" to durationS.toString(),
            "{duration_min}" to ((durationS + 59) / 60).toString(),
            "{offset}" to offsetS.toString(),
            "{Y}" to fmt("yyyy", startMs, utc),
            "{m}" to fmt("MM", startMs, utc),
            "{d}" to fmt("dd", startMs, utc),
            "{H}" to fmt("HH", startMs, utc),
            "{M}" to fmt("mm", startMs, utc),
            "{S}" to fmt("ss", startMs, utc),
            "{url}" to URLEncoder.encode(streamUrl, "UTF-8")
        )
        replacements.forEach { (k, v) -> out = out.replace(k, v) }
        return out
    }

    private fun fmt(pattern: String, ms: Long, tz: TimeZone) =
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = tz }.format(Date(ms))

    /** Minimal strftime: Y m d H M S tokens (with or without %). */
    private fun strftime(format: String, ms: Long, tz: TimeZone): String {
        val sb = StringBuilder()
        var i = 0
        while (i < format.length) {
            val c = format[i]
            val token = if (c == '%' && i + 1 < format.length) format[++i] else c
            sb.append(
                when (token) {
                    'Y' -> fmt("yyyy", ms, tz); 'm' -> fmt("MM", ms, tz); 'd' -> fmt("dd", ms, tz)
                    'H' -> fmt("HH", ms, tz); 'M' -> fmt("mm", ms, tz); 'S' -> fmt("ss", ms, tz)
                    else -> token.toString()
                }
            )
            i++
        }
        return sb.toString()
    }

    private val CUSTOM = Regex("""\{(utc|start|end|utcend):([^}]+)}""")
    private val XTREAM_START = Regex("""\{xtream_start\|([^}]*)}""")
}

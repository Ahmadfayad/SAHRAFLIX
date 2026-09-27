package com.sahraflix.data.parser

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * Streaming M3U/M3U8 (extended IPTV) parser. Pure Kotlin so it is unit-testable on the JVM.
 *
 * Handles: attributes containing commas, titles containing commas, #EXTGRP, #EXTVLCOPT
 * user-agent/referrer, KODIPROP license hints, catchup attributes, tvg-shift and
 * playlist-level url-tvg / x-tvg-url EPG hints.
 */
class M3uParser {

    data class Header(val epgUrls: List<String>)

    data class Entry(
        val title: String,
        val url: String,
        val attributes: Map<String, String>,
        val group: String?,
        val userAgent: String?,
        val referrer: String?,
        val kind: Kind,
        /** All #KODIPROP key=value pairs collected between the #EXTINF line and this URL. */
        val kodiProps: Map<String, String> = emptyMap()
    ) {
        val tvgId: String? get() = attributes["tvg-id"]?.takeIf { it.isNotBlank() }
        val logo: String? get() = attributes["tvg-logo"]?.takeIf { it.isNotBlank() }
        val catchup: String? get() = attributes["catchup"]?.takeIf { it.isNotBlank() }
        val catchupSource: String? get() = attributes["catchup-source"]?.takeIf { it.isNotBlank() }
        /** e.g. "Widevine", "PlayReady" — from #KODIPROP:inputstream.adaptive.license_type */
        val drmType: String? get() = kodiProps["inputstream.adaptive.license_type"]?.takeIf { it.isNotBlank() }
        /** License server URL — from #KODIPROP:inputstream.adaptive.license_key */
        val drmKey: String? get() = kodiProps["inputstream.adaptive.license_key"]?.takeIf { it.isNotBlank() }
    }

    enum class Kind { LIVE, MOVIE, SERIES }

    sealed class ParseEvent {
        data class HeaderEvent(val header: Header) : ParseEvent()
        data class EntryEvent(val entry: Entry) : ParseEvent()
    }

    /**
     * Parses [reader] line by line, invoking [onHeader] once (if an #EXTM3U line is present)
     * and [onEntry] for each stream. Never holds the whole playlist in memory.
     */
    inline fun parse(reader: BufferedReader, onHeader: (Header) -> Unit = {}, onEntry: (Entry) -> Unit) {
        var info: String? = null
        var group: String? = null
        var userAgent: String? = null
        var referrer: String? = null
        var kodiProps: MutableMap<String, String>? = null
        var firstLine = true
        while (true) {
            val raw = reader.readLine() ?: break
            val line = if (firstLine) raw.removePrefix("\uFEFF").trim() else raw.trim()
            firstLine = false
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTM3U", ignoreCase = true) -> {
                    val attrs = parseAttributes(line)
                    val urls = listOfNotNull(attrs["url-tvg"], attrs["x-tvg-url"], attrs["tvg-url"])
                        .flatMap { it.split(',') }
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                    onHeader(Header(urls))
                }
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    info = line; group = null; userAgent = null; referrer = null; kodiProps = null
                }
                line.startsWith("#EXTGRP:", ignoreCase = true) -> group = line.substringAfter(':').trim()
                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val opt = line.substringAfter(':')
                    when {
                        opt.startsWith("http-user-agent=", true) -> userAgent = opt.substringAfter('=').trim()
                        opt.startsWith("http-referrer=", true) || opt.startsWith("http-referer=", true) ->
                            referrer = opt.substringAfter('=').trim()
                    }
                }
                line.startsWith("#KODIPROP:", ignoreCase = true) -> {
                    val prop = line.substringAfter(':')
                    val key = prop.substringBefore('=').trim().lowercase()
                    val value = prop.substringAfter('=', "").trim()
                    if (key.isNotEmpty()) {
                        (kodiProps ?: LinkedHashMap<String, String>().also { kodiProps = it })[key] = value
                    }
                }
                line.startsWith("#") -> Unit
                else -> {
                    val meta = info ?: continue // bare URL without #EXTINF: ignore (not an IPTV entry)
                    val attributes = parseAttributes(meta)
                    val title = extractTitle(meta).ifBlank { attributes["tvg-name"] ?: "Unnamed" }
                    val resolvedGroup = attributes["group-title"]?.takeIf { it.isNotBlank() } ?: group
                    onEntry(
                        Entry(
                            title = title,
                            url = line,
                            attributes = attributes,
                            group = resolvedGroup,
                            userAgent = userAgent ?: attributes["user-agent"],
                            referrer = referrer,
                            kind = detectKind(line, title, resolvedGroup),
                            kodiProps = kodiProps ?: emptyMap()
                        )
                    )
                    info = null
                }
            }
        }
    }

    /**
     * Flow-based variant of [parse]. Emits [ParseEvent.HeaderEvent] once (if an #EXTM3U line
     * is present) then [ParseEvent.EntryEvent] for every stream entry.
     *
     * Uses a 64 KB read buffer and runs IO on [Dispatchers.IO]. The [stream] is closed
     * automatically when collection completes or is cancelled.
     */
    fun parseFlow(
        stream: InputStream,
        charset: Charset = Charsets.UTF_8
    ): Flow<ParseEvent> = flow {
        BufferedReader(InputStreamReader(stream, charset), 65536).use { reader ->
            var info: String? = null
            var group: String? = null
            var userAgent: String? = null
            var referrer: String? = null
            var kodiProps: MutableMap<String, String>? = null
            var firstLine = true
            while (true) {
                val raw = reader.readLine() ?: break
                val line = if (firstLine) raw.removePrefix("\uFEFF").trim() else raw.trim()
                firstLine = false
                when {
                    line.isEmpty() -> Unit
                    line.startsWith("#EXTM3U", ignoreCase = true) -> {
                        val attrs = parseAttributes(line)
                        val urls = listOfNotNull(attrs["url-tvg"], attrs["x-tvg-url"], attrs["tvg-url"])
                            .flatMap { it.split(',') }
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .distinct()
                        emit(ParseEvent.HeaderEvent(Header(urls)))
                    }
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        info = line; group = null; userAgent = null; referrer = null; kodiProps = null
                    }
                    line.startsWith("#EXTGRP:", ignoreCase = true) -> group = line.substringAfter(':').trim()
                    line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                        val opt = line.substringAfter(':')
                        when {
                            opt.startsWith("http-user-agent=", true) -> userAgent = opt.substringAfter('=').trim()
                            opt.startsWith("http-referrer=", true) || opt.startsWith("http-referer=", true) ->
                                referrer = opt.substringAfter('=').trim()
                        }
                    }
                    line.startsWith("#KODIPROP:", ignoreCase = true) -> {
                        val prop = line.substringAfter(':')
                        val key = prop.substringBefore('=').trim().lowercase()
                        val value = prop.substringAfter('=', "").trim()
                        if (key.isNotEmpty()) {
                            (kodiProps ?: LinkedHashMap<String, String>().also { kodiProps = it })[key] = value
                        }
                    }
                    line.startsWith("#") -> Unit
                    else -> {
                        val meta = info ?: continue
                        val attributes = parseAttributes(meta)
                        val title = extractTitle(meta).ifBlank { attributes["tvg-name"] ?: "Unnamed" }
                        val resolvedGroup = attributes["group-title"]?.takeIf { it.isNotBlank() } ?: group
                        emit(
                            ParseEvent.EntryEvent(
                                Entry(
                                    title = title,
                                    url = line,
                                    attributes = attributes,
                                    group = resolvedGroup,
                                    userAgent = userAgent ?: attributes["user-agent"],
                                    referrer = referrer,
                                    kind = detectKind(line, title, resolvedGroup),
                                    kodiProps = kodiProps ?: emptyMap()
                                )
                            )
                        )
                        info = null
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /** Parses key="value" / key=value pairs; tolerant of commas and spaces inside quotes. */
    fun parseAttributes(line: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        // Only look at the part before the title separator.
        val end = titleSeparatorIndex(line).let { if (it < 0) line.length else it }
        var i = line.indexOf(' ').let { if (it < 0 || it > end) end else it }
        while (i < end) {
            while (i < end && line[i] == ' ') i++
            val keyStart = i
            while (i < end && line[i] != '=' && line[i] != ' ') i++
            if (i >= end || line[i] != '=') { i++; continue }
            val key = line.substring(keyStart, i).lowercase()
            i++ // skip '='
            val value: String
            if (i < end && (line[i] == '"' || line[i] == '\'')) {
                val quote = line[i]
                val close = line.indexOf(quote, i + 1).let { if (it < 0) end else it }
                value = line.substring(i + 1, close)
                i = close + 1
            } else {
                val vs = i
                while (i < end && line[i] != ' ') i++
                value = line.substring(vs, i)
            }
            if (key.isNotEmpty()) result[key] = value.trim()
        }
        return result
    }

    /** The title follows the first comma that is not inside a quoted attribute value. */
    fun extractTitle(line: String): String {
        val idx = titleSeparatorIndex(line)
        return if (idx < 0) "" else line.substring(idx + 1).trim()
    }

    private fun titleSeparatorIndex(line: String): Int {
        var quote: Char? = null
        for (i in line.indices) {
            val c = line[i]
            if (quote != null) {
                if (c == quote) quote = null
            } else if (c == '"') {
                quote = c
            } else if (c == ',') {
                return i
            }
        }
        return -1
    }

    fun detectKind(url: String, title: String, group: String?): Kind {
        val path = url.substringBefore('?').lowercase()
        val g = group.orEmpty().lowercase()
        return when {
            "/series/" in path || SERIES_PATTERN.containsMatchIn(title) -> Kind.SERIES
            "/movie/" in path || VOD_EXTENSIONS.any { path.endsWith(it) } -> Kind.MOVIE
            g.startsWith("vod") || g.contains("movies") || g.contains("films") -> Kind.MOVIE
            g.contains("series") -> Kind.SERIES
            else -> Kind.LIVE
        }
    }

    companion object {
        private val SERIES_PATTERN = Regex("""\bS\d{1,2}\s?E\d{1,3}\b""", RegexOption.IGNORE_CASE)
        private val VOD_EXTENSIONS = listOf(".mp4", ".mkv", ".avi", ".mov", ".m4v", ".webm")
    }
}

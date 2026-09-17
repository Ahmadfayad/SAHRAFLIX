package com.sahraflix.data.remote

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalHlsProxy @Inject constructor(
    private val okHttpClient: OkHttpClient
) : NanoHTTPD("127.0.0.1", PORT) {
    fun startIfNeeded() {
        if (!isAlive) start(SOCKET_READ_TIMEOUT, false)
    }

    fun proxyUrl(url: String, referer: String?): String =
        "http://127.0.0.1:$PORT/hls?url=${URLEncoder.encode(url, StandardCharsets.UTF_8.name())}&ref=${URLEncoder.encode(referer.orEmpty(), StandardCharsets.UTF_8.name())}"

    override fun serve(session: IHTTPSession): Response {
        if (!session.uri.equals("/hls", ignoreCase = true)) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
        }
        val encodedUrl = session.parameters["url"]?.firstOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing url")
        val streamUrl = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.name())
        val referer = session.parameters["ref"]?.firstOrNull()?.let {
            URLDecoder.decode(it, StandardCharsets.UTF_8.name())
        }.orEmpty()
        if (!streamUrl.startsWith("https://") && !streamUrl.startsWith("http://")) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Invalid url")
        }

        val request = Request.Builder()
            .url(streamUrl)
            .header("Referer", referer)
            .header("User-Agent", BROWSER_USER_AGENT)
            .build()
        val upstream = runBlocking(Dispatchers.IO) { okHttpClient.newCall(request).execute() }
        if (!upstream.isSuccessful || upstream.body == null) {
            upstream.close()
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Upstream failed")
        }
        val body = upstream.body!!
        val contentType = body.contentType()?.toString() ?: MIME_DEFAULT_BINARY
        return if (contentType.contains("mpegurl", ignoreCase = true) || streamUrl.contains(".m3u8", true)) {
            val playlist = body.string()
            upstream.close()
            val rewritten = rewritePlaylist(playlist, referer)
            newFixedLengthResponse(Response.Status.OK, "application/vnd.apple.mpegurl", rewritten)
        } else {
            newChunkedResponse(Response.Status.OK, contentType, body.byteStream()).apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        }
    }

    private fun rewritePlaylist(playlist: String, referer: String): String = playlist.lineSequence()
        .joinToString("\n") { line ->
            if (line.isBlank() || line.startsWith("#")) line
            else proxyUrl(resolveUrl(line, referer), referer)
        }

    private fun resolveUrl(value: String, referer: String): String = when {
        value.startsWith("http://") || value.startsWith("https://") -> value
        referer.isBlank() -> value
        else -> java.net.URI(referer).resolve(value).toString()
    }

    private companion object {
        const val PORT = 18080
        const val SOCKET_READ_TIMEOUT = 5_000
        const val MIME_DEFAULT_BINARY = "application/octet-stream"
        const val BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
}

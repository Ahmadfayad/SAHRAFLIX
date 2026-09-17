package com.sahraflix.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class TmdbStreamingResolver @Inject constructor(
    private val okHttpClient: OkHttpClient
) : StreamingProviderResolver {
    override suspend fun resolveMovie(tmdbId: String): List<ResolvedStream> =
        resolve("https://vidsrc.fyi/embed/movie/$tmdbId")

    override suspend fun resolveEpisode(tmdbId: String, season: Int, episode: Int): List<ResolvedStream> =
        resolve("https://vidsrc.fyi/embed/tv/$tmdbId/$season/$episode")

    private suspend fun resolve(embedUrl: String): List<ResolvedStream> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(embedUrl)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Referer", REFERER)
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Streaming provider request failed: ${response.code}")
            extractHlsUrls(response.body?.string().orEmpty(), REFERER)
        }
    }

    private fun extractHlsUrls(html: String, referer: String): List<ResolvedStream> {
        val urls = linkedSetOf<String>()
        HLS_PATTERN.findAll(html).forEach { urls += it.value.decodeHtmlUrl() }
        IFRAME_PATTERN.findAll(html).forEach { match ->
            val value = match.groupValues[1].decodeHtmlUrl()
            if (value.contains("m3u8", ignoreCase = true)) urls += value
        }
        return urls.map { url ->
            ResolvedStream(
                hlsUrl = url,
                referer = referer,
                userAgent = BROWSER_USER_AGENT,
                needsProxy = true,
                quality = qualityOf(url),
                provider = "VidSrc"
            )
        }
    }

    private fun String.decodeHtmlUrl(): String =
        replace("&amp;", "&").replace("\\/", "/")

    private fun qualityOf(url: String): String = when {
        Regex("4k|2160", RegexOption.IGNORE_CASE).containsMatchIn(url) -> "4K"
        Regex("1080", RegexOption.IGNORE_CASE).containsMatchIn(url) -> "1080p"
        Regex("720", RegexOption.IGNORE_CASE).containsMatchIn(url) -> "720p"
        Regex("480", RegexOption.IGNORE_CASE).containsMatchIn(url) -> "480p"
        else -> "Auto"
    }

    private companion object {
        const val REFERER = "https://vidsrc.fyi/"
        const val BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        val HLS_PATTERN = Regex("https?://[^\\\"'\\s<>]+\\.m3u8[^\\\"'\\s<>]*")
        val IFRAME_PATTERN = Regex("src=[\\\"']([^\\\"']+)[\\\"']")
    }
}

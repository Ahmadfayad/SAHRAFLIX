package com.sahraflix.data.remote

import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class VidSrcApi @Inject constructor(
    private val okHttpClient: OkHttpClient,
    moshi: Moshi
) {
    private val adapter = moshi.adapter(VidSrcResponse::class.java)

    suspend fun getMovieSources(tmdbId: String): List<StreamServer> =
        request("https://vidsrc.fyi/vapi/v2/movie/$tmdbId")

    suspend fun getEpisodeSources(tmdbId: String, season: Int, episode: Int): List<StreamServer> =
        request("https://vidsrc.fyi/vapi/v2/tv/$tmdbId/$season/$episode")

    private suspend fun request(url: String): List<StreamServer> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("VidSrc request failed: ${response.code}")
            val body = response.body?.string() ?: error("VidSrc response was empty")
            adapter.fromJson(body)?.results.orEmpty()
        }
    }
}

data class VidSrcResponse(val results: List<StreamServer> = emptyList())
data class StreamServer(
    val title: String? = null,
    val url: String? = null,
    val quality: String? = null,
    val type: String? = null
) {
    val streamUrl: String get() = url.orEmpty()
}

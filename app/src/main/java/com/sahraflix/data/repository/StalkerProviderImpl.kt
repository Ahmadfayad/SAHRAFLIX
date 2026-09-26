package com.sahraflix.data.repository

import com.sahraflix.data.local.entity.CategoryEntity
import com.sahraflix.data.local.entity.PlaylistEntity
import com.sahraflix.data.local.entity.StreamItemEntity
import com.sahraflix.domain.model.StreamType
import com.sahraflix.domain.repository.PlaylistProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stalker / Ministra middleware (MAG set-top-box protocol).
 * handshake → Bearer token → get_profile → itv genres + channels. Stream links are created
 * on demand at play time (create_link), because they are short-lived and token-bound.
 * VOD on Stalker portals is paged per category and is not synchronised (live TV only).
 */
@Singleton
class StalkerProviderImpl @Inject constructor(
    private val httpClient: OkHttpClient
) : PlaylistProvider {

    private data class Session(val endpoint: HttpUrl, val mac: String, val token: String, val createdAt: Long)
    private val sessions = HashMap<String, Session>()
    private val mutex = Mutex()

    override suspend fun sync(playlist: PlaylistEntity, writer: SyncWriter) = withContext(Dispatchers.IO) {
        val session = session(playlist, forceNew = true)
        val genres = LinkedHashMap<String, String>()
        runCatching {
            val js = call(session, "itv", "get_genres").optJSONArray("js")
            for (i in 0 until (js?.length() ?: 0)) {
                val g = js!!.optJSONObject(i) ?: continue
                genres[g.optString("id")] = g.optString("title").ifBlank { "Genre ${g.optString("id")}" }
            }
        }
        genres.forEach { (id, title) -> writer.category(CategoryEntity(catId(playlist.id, id), title, playlist.id, StreamType.LIVE)) }
        writer.category(CategoryEntity(catId(playlist.id, "0"), "All channels", playlist.id, StreamType.LIVE))

        val data = call(session, "itv", "get_all_channels").optJSONObject("js")?.optJSONArray("data")
            ?: throw IOException("Portal returned no channel list")
        for (i in 0 until data.length()) {
            val ch = data.optJSONObject(i) ?: continue
            val id = ch.optString("id").takeIf { it.isNotBlank() } ?: continue
            val cmd = ch.optString("cmd").takeIf { it.isNotBlank() } ?: continue
            val genre = ch.optString("tv_genre_id").takeIf { it in genres } ?: "0"
            writer.stream(
                StreamItemEntity(
                    id = "${playlist.id}:LIVE:$id",
                    name = ch.optString("name").ifBlank { "Channel $id" },
                    // Stored with a marker; resolved to a real URL through create_link at play time.
                    streamUrl = "$STALKER_SCHEME$cmd",
                    logoUrl = ch.optString("logo").takeIf { it.startsWith("http") },
                    streamType = StreamType.LIVE,
                    categoryId = catId(playlist.id, genre),
                    playlistId = playlist.id,
                    epgChannelId = ch.optString("xmltv_id").takeIf { it.isNotBlank() },
                    providerId = id,
                    sortOrder = ch.optString("number").toIntOrNull() ?: i
                )
            )
        }
    }

    /** Turns a stored "stalker:<cmd>" into a playable URL. Retries once with a fresh token. */
    suspend fun createLink(playlist: PlaylistEntity, storedUrl: String): String = withContext(Dispatchers.IO) {
        val cmd = storedUrl.removePrefix(STALKER_SCHEME)
        suspend fun attempt(force: Boolean): String {
            val s = session(playlist, force)
            val js = call(s, "itv", "create_link", "cmd" to cmd, "forced_storage" to "0", "disable_ad" to "0").opt("js")
            val link = when (js) {
                is JSONObject -> js.optString("cmd")
                else -> ""
            }
            return cleanCmd(link).takeIf { it.startsWith("http") } ?: throw IOException("Portal did not return a stream link")
        }
        runCatching { attempt(false) }.getOrElse { attempt(true) }
    }

    private suspend fun session(playlist: PlaylistEntity, forceNew: Boolean): Session = mutex.withLock {
        val cached = sessions[playlist.id]
        if (!forceNew && cached != null && System.currentTimeMillis() - cached.createdAt < TOKEN_TTL_MS) return cached
        val mac = playlist.macAddress?.uppercase()?.trim()?.takeIf { MAC.matches(it) }
            ?: throw IOException("A valid MAC address (00:1A:79:XX:XX:XX) is required for Stalker portals")
        val endpoint = resolveEndpoint(playlist.url, mac)
        val hs = rawCall(endpoint, mac, null, "stb", "handshake", "token" to "")
        val token = hs.optJSONObject("js")?.optString("token")?.takeIf { it.isNotBlank() }
            ?: throw IOException("Portal handshake failed (no token). Check the portal URL and MAC.")
        val s = Session(endpoint, mac, token, System.currentTimeMillis())
        // get_profile activates the token on most Ministra builds.
        runCatching { rawCall(endpoint, mac, token, "stb", "get_profile", "hd" to "1", "stb_type" to "MAG250") }
        sessions[playlist.id] = s
        s
    }

    /** Portal URLs come as ".../c/", ".../stalker_portal/c/" or bare host; find load.php / portal.php. */
    private fun resolveEndpoint(portal: String, mac: String): HttpUrl {
        var base = portal.trim()
        if (!base.startsWith("http", true)) base = "http://$base"
        base = base.trimEnd('/').removeSuffix("/c").removeSuffix("/index.html").trimEnd('/')
        val candidates = listOf("$base/server/load.php", "$base/portal.php", "$base/stalker_portal/server/load.php")
        for (candidate in candidates) {
            val url = candidate.toHttpUrlOrNull() ?: continue
            val ok = runCatching { rawCall(url, mac, null, "stb", "handshake", "token" to "").has("js") }.getOrDefault(false)
            if (ok) return url
        }
        throw IOException("Could not reach a Stalker portal API at $portal")
    }

    private fun call(s: Session, type: String, action: String, vararg params: Pair<String, String>): JSONObject =
        rawCall(s.endpoint, s.mac, s.token, type, action, *params)

    private fun rawCall(endpoint: HttpUrl, mac: String, token: String?, type: String, action: String, vararg params: Pair<String, String>): JSONObject {
        val url = endpoint.newBuilder()
            .addQueryParameter("type", type)
            .addQueryParameter("action", action)
            .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
            .addQueryParameter("JsHttpRequest", "1-xml")
            .build()
        val request = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3")
            .header("X-User-Agent", "Model: MAG250; Link: WiFi")
            .header("Cookie", "mac=${mac.replace(":", "%3A")}; stb_lang=en; timezone=UTC")
            .header("Referer", endpoint.newBuilder().encodedPath("/c/").query(null).build().toString())
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        httpClient.newCall(request).execute().use { r ->
            if (!r.isSuccessful) throw IOException("Portal error ${r.code} on $action")
            val body = r.body?.string().orEmpty()
            return runCatching { JSONObject(body) }.getOrElse { throw IOException("Unexpected portal response on $action") }
        }
    }

    companion object {
        const val STALKER_SCHEME = "stalker:"
        private const val TOKEN_TTL_MS = 30 * 60 * 1000L
        private val MAC = Regex("([0-9A-F]{2}:){5}[0-9A-F]{2}")
        private fun catId(playlistId: String, genre: String) = "$playlistId:LIVE:genre:$genre"
        /** "ffmpeg http://…", "auto http://…" → "http://…" */
        fun cleanCmd(cmd: String): String = cmd.trim().split(' ').lastOrNull { it.startsWith("http") } ?: cmd.trim()
    }
}

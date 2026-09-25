package com.sahraflix.data.repository

import android.content.Context
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream

/** Opens http(s)/content/file sources, transparently un-gzipping (.xml.gz EPGs are the norm). */
object HttpStreams {
    fun open(context: Context, client: OkHttpClient, source: String, userAgent: String? = null): InputStream {
        val uri = Uri.parse(source)
        val raw: InputStream = when (uri.scheme?.lowercase()) {
            "content", "file" -> context.contentResolver.openInputStream(uri)
                ?: throw IOException("Unable to open $source")
            "http", "https" -> {
                val request = Request.Builder().url(source).apply {
                    userAgent?.takeIf { it.isNotBlank() }?.let { header("User-Agent", it) }
                }.build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val code = response.code
                    response.close()
                    throw IOException(
                        when (code) {
                            401, 403 -> "Access denied by the server ($code). Check your credentials or subscription."
                            404 -> "Playlist not found at this address (404)."
                            else -> "Server returned HTTP $code"
                        }
                    )
                }
                val body = response.body ?: run { response.close(); throw IOException("Empty response") }
                // Closing the stream closes the response.
                object : FilterInputStream(body.byteStream()) {
                    override fun close() { super.close(); response.close() }
                }
            }
            else -> throw IOException("Unsupported address: $source")
        }
        val buffered = BufferedInputStream(raw, 64 * 1024)
        buffered.mark(2)
        val b1 = buffered.read(); val b2 = buffered.read()
        buffered.reset()
        return if (b1 == 0x1f && b2 == 0x8b) GZIPInputStream(buffered, 64 * 1024) else buffered
    }
}

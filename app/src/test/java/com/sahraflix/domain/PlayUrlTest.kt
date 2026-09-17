package com.sahraflix.domain

import com.sahraflix.domain.model.PlayUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayUrlTest {
    @Test
    fun directUrlIsPreservedForExoPlayer() {
        val url = PlayUrl.Direct("http://127.0.0.1:18080/hls?url=stream")
        assertEquals("http://127.0.0.1:18080/hls?url=stream", url.url)
    }
}

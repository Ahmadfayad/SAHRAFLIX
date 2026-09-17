package com.sahraflix.domain

import com.sahraflix.domain.model.CatalogEntry
import com.sahraflix.domain.model.StreamType
import com.sahraflix.domain.model.StreamingContent
import com.sahraflix.domain.model.toCatalogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamingContentTest {
    @Test
    fun convertsToCatalogEntryWithoutCachingPlaybackUrl() {
        val content = StreamingContent(
            tmdbId = "550",
            title = "Fight Club",
            posterUrl = "poster",
            backdropUrl = null,
            overview = "Overview",
            releaseYear = 1999,
            rating = 8.4f,
            type = StreamType.MOVIE
        )

        val entry = content.toCatalogEntry()

        assertEquals("streaming:550", entry.id)
        assertEquals(550, entry.tmdbId)
        assertEquals("Fight Club", entry.title)
        assertEquals(CatalogEntry.Streaming::class, entry::class)
    }
}
